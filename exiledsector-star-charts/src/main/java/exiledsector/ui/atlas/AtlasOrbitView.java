package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt;
import exiledsector.charts.tree.AtlasOrbitSystem;
import exiledsector.charts.tree.AtlasOrbitSystem.Body;
import exiledsector.charts.tree.AtlasOrbitSystem.Link;
import exiledsector.charts.tree.AtlasOrbitSystem.Track;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.LineBatch;

import java.awt.Color;
import java.util.List;

final class AtlasOrbitView {

    interface States {

        boolean allocated(String nodeId);

        boolean allocatable(String nodeId);
    }

    private static final Color DEFAULT_COLOUR = new Color(200, 210, 230);
    private static final Color TRACK_COLOUR = new Color(110, 130, 165);
    private static final Color LINK_COLOUR = new Color(120, 135, 160);
    private static final Color HOVER_COLOUR = Color.WHITE;
    private static final float STAR_TRACK_ALPHA = 0.22f;
    private static final float SATELLITE_TRACK_ALPHA = 0.13f;
    private static final float LIT_TRACK_ALPHA = 0.4f;
    private static final float DIM_LINK_ALPHA = 0.4f;
    private static final float LIT_LINK_ALPHA = 0.85f;
    private static final float MIN_TRACK_PIXELS = 3f;
    private static final float TRACK_SEGMENT_PIXELS = 6f;
    private static final int MIN_TRACK_SEGMENTS = 24;
    private static final int MAX_TRACK_SEGMENTS = 360;
    private static final float ARC_SEGMENT_PIXELS = 8f;
    private static final float MIN_BODY_PIXELS = 2.5f;
    private static final float MIN_HIT_PIXELS = 8f;
    private static final float HIT_SCALE = 1.15f;
    private static final float FLEET_HIT_SCALE = 1.6f;
    private static final float LINK_GAP_SCALE = 1.05f;
    private static final float MIN_VISIBLE_LINK_SHARE = 0.35f;
    private static final float PULSE_SECONDS = 1.6f;
    private static final float PULSE_RING_SCALE = 1.45f;
    private static final float HOVER_RING_SCALE = 1.65f;
    private static final int RING_SEGMENTS = 40;
    private static final float HOLD_EASE_PER_SECOND = 3f;
    private static final float SPIN_DEGREES_PER_SECOND = 12f;

    private static double orbitSeconds;

    private final AtlasOrbitSystem system;
    private final AtlasOrbitSystem.Positions positions;
    private final AtlasBodyPainter painter = new AtlasBodyPainter();
    private final Color[] colours;
    private final float[] spinSeeds;
    private final boolean[] linkedTracks;
    private final LineBatch trackLines = new LineBatch(1f);
    private final LineBatch dimLinks = new LineBatch(1.5f);
    private final LineBatch litLinks = new LineBatch(2.5f);
    private final LineBatch rings = new LineBatch(1.5f);
    private float timeScale = 1f;
    private float pulseSeconds;

    AtlasOrbitView(AtlasOrbitSystem system) {
        this.system = system;
        this.positions = system.newPositions();
        List<Body> bodies = system.bodies();
        this.colours = new Color[bodies.size()];
        this.spinSeeds = new float[bodies.size()];
        for (Body body : bodies) {
            colours[body.index()] = parseColour(body.colour());
            spinSeeds[body.index()] = Math.floorMod(body.nodeId().hashCode(), 360);
        }
        this.linkedTracks = new boolean[system.tracks().size()];
        for (Link link : system.links()) {
            if (link.trackIndex() >= 0) {
                linkedTracks[link.trackIndex()] = true;
            }
        }
        system.positionsAt(orbitSeconds, positions);
    }

    private static Color parseColour(String hex) {
        if (hex == null || hex.length() != 7 || hex.charAt(0) != '#') {
            return DEFAULT_COLOUR;
        }
        try {
            return new Color(Integer.parseInt(hex.substring(1), 16));
        } catch (NumberFormatException e) {
            return DEFAULT_COLOUR;
        }
    }

    void advance(float amount, boolean holding) {
        float target = holding ? 0f : 1f;
        float step = HOLD_EASE_PER_SECOND * amount;
        timeScale = timeScale < target ? Math.min(target, timeScale + step) : Math.max(target, timeScale - step);
        orbitSeconds += amount * timeScale;
        pulseSeconds = (pulseSeconds + amount) % PULSE_SECONDS;
        system.positionsAt(orbitSeconds, positions);
    }

    float x(String nodeId) {
        Body body = system.body(nodeId);
        return body == null ? 0f : positions.x(body.index());
    }

    float y(String nodeId) {
        Body body = system.body(nodeId);
        return body == null ? 0f : positions.y(body.index());
    }

    String nodeAt(TreeViewport viewport, float screenX, float screenY) {
        String closest = null;
        float closestDistance = Float.MAX_VALUE;
        for (Body body : system.bodies()) {
            if (body.art().kind() == AtlasBodyArt.Kind.STAR) {
                continue;
            }
            float hitScale = body.art().kind() == AtlasBodyArt.Kind.FLEET ? FLEET_HIT_SCALE : HIT_SCALE;
            float hitRadius = Math.max(body.art().size() * viewport.zoom() * hitScale, MIN_HIT_PIXELS);
            float distance = (float) Math.hypot(screenX - viewport.screenX(positions.x(body.index())), screenY - viewport.screenY(positions.y(body.index())));
            if (distance <= hitRadius && distance - hitRadius < closestDistance) {
                closest = body.nodeId();
                closestDistance = distance - hitRadius;
            }
        }
        return closest;
    }

    void render(TreeViewport viewport, States states, String hoveredNodeId, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        float seconds = (float) (orbitSeconds % 3600.0);
        boolean[] allocated = new boolean[system.bodies().size()];
        for (Body body : system.bodies()) {
            allocated[body.index()] = states.allocated(body.nodeId());
        }
        trackLines.clear();
        dimLinks.clear();
        litLinks.clear();
        rings.clear();
        for (Track track : system.tracks()) {
            if (linkedTracks[track.index()]) {
                addTrack(viewport, track, allocated, alpha);
            }
        }
        for (Link link : system.links()) {
            addLink(viewport, link, allocated, alpha);
        }
        try (GlScope scope = GlScope.flat()) {
            trackLines.flush();
            dimLinks.flush();
            litLinks.flush();
        }
        for (Body body : system.bodies()) {
            renderBody(viewport, body, states, allocated[body.index()], body.nodeId().equals(hoveredNodeId), seconds, alpha);
        }
        try (GlScope scope = GlScope.flat()) {
            rings.flush();
        }
    }

    private void addTrack(TreeViewport viewport, Track track, boolean[] allocated, float alpha) {
        float radius = track.radius() * viewport.zoom();
        float centreX = viewport.screenX(positions.x(track.centreIndex()));
        float centreY = viewport.screenY(positions.y(track.centreIndex()));
        if (radius < MIN_TRACK_PIXELS || !viewport.overlaps(centreX - radius, centreY - radius, centreX + radius, centreY + radius)) {
            return;
        }
        boolean aroundStar = !system.bodies().get(track.centreIndex()).orbits();
        Color colour = TRACK_COLOUR;
        float trackAlpha = aroundStar ? STAR_TRACK_ALPHA : SATELLITE_TRACK_ALPHA;
        for (int member : track.memberIndexes()) {
            if (allocated[member]) {
                colour = colours[member];
                trackAlpha = LIT_TRACK_ALPHA;
                break;
            }
        }
        int segments = Math.max(MIN_TRACK_SEGMENTS, Math.min(MAX_TRACK_SEGMENTS, (int) (radius * 2f * Math.PI / TRACK_SEGMENT_PIXELS)));
        addArc(trackLines, centreX, centreY, radius, 0.0, Math.PI * 2.0, segments, colour, alpha * trackAlpha);
    }

    private static void addArc(LineBatch batch, float centreX, float centreY, float radius, double startRadians, double sweepRadians, int segments,
                               Color colour, float alpha) {
        float previousX = centreX + radius * (float) Math.cos(startRadians);
        float previousY = centreY + radius * (float) Math.sin(startRadians);
        for (int segment = 1; segment <= segments; segment++) {
            double angle = startRadians + sweepRadians * segment / segments;
            float nextX = centreX + radius * (float) Math.cos(angle);
            float nextY = centreY + radius * (float) Math.sin(angle);
            batch.add(previousX, previousY, nextX, nextY, colour, alpha);
            previousX = nextX;
            previousY = nextY;
        }
    }

    private void addLink(TreeViewport viewport, Link link, boolean[] allocated, float alpha) {
        switch (link.kind()) {
            case ARC -> addArcLink(viewport, link, allocated, alpha);
            case TETHER -> addTether(viewport, link, allocated, alpha);
            default -> addStraight(viewport, link, allocated, alpha);
        }
    }

    private void addArcLink(TreeViewport viewport, Link link, boolean[] allocated, float alpha) {
        if (!(allocated[link.first()] && allocated[link.second()])) {
            return;
        }
        Track track = system.tracks().get(link.trackIndex());
        float radius = track.radius() * viewport.zoom();
        float centreX = viewport.screenX(positions.x(track.centreIndex()));
        float centreY = viewport.screenY(positions.y(track.centreIndex()));
        double firstAngle = Math.atan2(viewport.screenY(positions.y(link.first())) - centreY, viewport.screenX(positions.x(link.first())) - centreX);
        double secondAngle = Math.atan2(viewport.screenY(positions.y(link.second())) - centreY, viewport.screenX(positions.x(link.second())) - centreX);
        double sweep = Math.IEEEremainder(secondAngle - firstAngle, Math.PI * 2.0);
        float arcPixels = (float) Math.abs(sweep) * radius;
        float midX = centreX + radius * (float) Math.cos(firstAngle + sweep / 2.0);
        float midY = centreY + radius * (float) Math.sin(firstAngle + sweep / 2.0);
        if (!viewport.isVisible(midX, midY, arcPixels)) {
            return;
        }
        int segments = Math.max(2, (int) (arcPixels / ARC_SEGMENT_PIXELS));
        addArc(litLinks, centreX, centreY, radius, firstAngle, sweep, segments, colours[link.first()], alpha * LIT_LINK_ALPHA);
    }

    private void addStraight(TreeViewport viewport, Link link, boolean[] allocated, float alpha) {
        float firstX = viewport.screenX(positions.x(link.first()));
        float firstY = viewport.screenY(positions.y(link.first()));
        float secondX = viewport.screenX(positions.x(link.second()));
        float secondY = viewport.screenY(positions.y(link.second()));
        boolean lit = allocated[link.first()] && allocated[link.second()];
        addTrimmed(viewport, firstX, firstY, gapOf(link.first(), viewport), secondX, secondY, gapOf(link.second(), viewport),
                lit ? litLinks : dimLinks, lit ? colours[link.second()] : LINK_COLOUR, alpha * (lit ? LIT_LINK_ALPHA : DIM_LINK_ALPHA));
    }

    private void addTether(TreeViewport viewport, Link link, boolean[] allocated, float alpha) {
        Track track = system.tracks().get(link.trackIndex());
        float anchorX = viewport.screenX(positions.x(link.first()));
        float anchorY = viewport.screenY(positions.y(link.first()));
        float centreX = viewport.screenX(positions.x(track.centreIndex()));
        float centreY = viewport.screenY(positions.y(track.centreIndex()));
        float towardX = anchorX - centreX;
        float towardY = anchorY - centreY;
        float distance = (float) Math.hypot(towardX, towardY);
        if (distance <= 0f) {
            return;
        }
        float radius = track.radius() * viewport.zoom();
        float landingX = centreX + towardX / distance * radius;
        float landingY = centreY + towardY / distance * radius;
        boolean lit = allocated[link.first()] && anyAllocated(track, allocated);
        addTrimmed(viewport, anchorX, anchorY, gapOf(link.first(), viewport), landingX, landingY, 0f, lit ? litLinks : dimLinks,
                lit ? colours[link.first()] : LINK_COLOUR, alpha * (lit ? LIT_LINK_ALPHA : DIM_LINK_ALPHA));
    }

    private static boolean anyAllocated(Track track, boolean[] allocated) {
        for (int member : track.memberIndexes()) {
            if (allocated[member]) {
                return true;
            }
        }
        return false;
    }

    private float gapOf(int bodyIndex, TreeViewport viewport) {
        AtlasBodyArt art = system.bodies().get(bodyIndex).art();
        return Math.max(art.size() * viewport.zoom(), MIN_BODY_PIXELS) * LINK_GAP_SCALE;
    }

    private static void addTrimmed(TreeViewport viewport, float fromX, float fromY, float fromGap, float toX, float toY, float toGap, LineBatch batch,
                                   Color colour, float alpha) {
        float deltaX = toX - fromX;
        float deltaY = toY - fromY;
        float length = (float) Math.hypot(deltaX, deltaY);
        if (length <= 0f) {
            return;
        }
        float hiddenShare = (fromGap + toGap) / length;
        if (hiddenShare > 1f - MIN_VISIBLE_LINK_SHARE) {
            float shrink = (1f - MIN_VISIBLE_LINK_SHARE) / hiddenShare;
            fromGap *= shrink;
            toGap *= shrink;
        }
        if (!viewport.overlaps(Math.min(fromX, toX), Math.min(fromY, toY), Math.max(fromX, toX), Math.max(fromY, toY))) {
            return;
        }
        float unitX = deltaX / length;
        float unitY = deltaY / length;
        batch.add(fromX + unitX * fromGap, fromY + unitY * fromGap, toX - unitX * toGap, toY - unitY * toGap, colour, alpha);
    }

    private void renderBody(TreeViewport viewport, Body body, States states, boolean allocated, boolean hovered, float seconds, float alpha) {
        int index = body.index();
        float screenX = viewport.screenX(positions.x(index));
        float screenY = viewport.screenY(positions.y(index));
        float screenRadius = Math.max(body.art().size() * viewport.zoom(), MIN_BODY_PIXELS);
        if (!viewport.isVisible(screenX, screenY, screenRadius * HOVER_RING_SCALE * 2f)) {
            return;
        }
        boolean available = !allocated && states.allocatable(body.nodeId());
        AtlasBodyPainter.Lighting lighting = allocated ? AtlasBodyPainter.Lighting.LIT
                : available ? AtlasBodyPainter.Lighting.AVAILABLE : AtlasBodyPainter.Lighting.DULL;
        float heading = headingOf(body);
        float spin = spinSeeds[index] + seconds * SPIN_DEGREES_PER_SECOND * (index % 2 == 0 ? 1f : -1f);
        painter.paint(body.art(), colours[index], new AtlasBodyPainter.Placement(screenX, screenY, screenRadius, heading, spin), lighting, seconds, alpha);
        if (available) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(pulseSeconds / PULSE_SECONDS * Math.PI * 2.0);
            addArc(rings, screenX, screenY, screenRadius * PULSE_RING_SCALE, 0.0, Math.PI * 2.0, RING_SEGMENTS, colours[index], alpha * (0.3f + 0.45f * pulse));
        }
        if (hovered) {
            addArc(rings, screenX, screenY, screenRadius * HOVER_RING_SCALE, 0.0, Math.PI * 2.0, RING_SEGMENTS, HOVER_COLOUR, alpha * 0.8f);
        }
    }

    private float headingOf(Body body) {
        if (!body.orbits()) {
            return 90f;
        }
        int index = body.index();
        float outwardX = positions.x(index) - positions.x(body.centreIndex());
        float outwardScreenY = positions.y(body.centreIndex()) - positions.y(index);
        float outward = (float) Math.toDegrees(Math.atan2(outwardScreenY, outwardX));
        return body.degreesPerSecond() >= 0f ? outward - 90f : outward + 90f;
    }
}
