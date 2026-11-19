package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.charts.tree.AtlasBodyArt;
import exiledsector.charts.tree.AtlasLinkPath;
import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.charts.tree.AtlasTreeBodies.Link;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.AtlasShipGroup;
import exiledsector.ui.node.ConnectorKind;
import exiledsector.ui.node.ConnectorPainter;
import exiledsector.ui.node.ConnectorPainter.ConnectorEndpoint;
import exiledsector.ui.node.ConnectorPainter.ConnectorFade;
import exiledsector.ui.node.ConnectorPainter.ConnectorStyle;
import exiledsector.ui.node.ConnectorPainter.EdgeFill;
import exiledsector.ui.node.NodeBeams;
import exiledsector.ui.node.NodeRingArt;
import exiledsector.ui.node.SkillTreeNodeGeometry;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;
import java.util.Arrays;

final class AtlasTreeView {

    interface States {

        boolean allocated(String nodeId);

        boolean allocatable(String nodeId);
    }

    private static final Color DEFAULT_COLOUR = new Color(200, 210, 230);
    private static final Color HOVER_COLOUR = Color.WHITE;
    private static final float MIN_BODY_PIXELS = 2.5f;
    private static final float MIN_HIT_PIXELS = 8f;
    private static final float HIT_SCALE = 1.15f;
    private static final float SHIP_GROUP_HIT_SCALE = 1.1f;
    private static final float STAR_GAP_SCALE = 1.05f;
    private static final float HIGHWAY_RING_SCALE = 1.35f;
    private static final float BODY_RING_SCALE = 1.12f;
    private static final float MIN_PATH_STEP_PIXELS = 4f;
    private static final int MAX_PATH_POINTS = 32;
    private static final float HOVER_RING_SCALE = 1.25f;
    private static final float HOVER_RING_ALPHA = 0.8f;
    private static final float SPIN_DEGREES_PER_SECOND = 12f;
    private static final float OBJECT_SPIN_SHARE = 0.2f;
    private static final float DULL_SHIP_ALPHA = 0.55f;
    private static final float AVAILABLE_SHIP_ALPHA = 0.8f;
    private static final double ANIMATION_PERIOD_SECONDS = 3600.0;
    private static final float[] CIRCLE = NodeRingArt.outlineRadii((cos, sin) -> 1f);
    private static final String CORONA_PATH = "graphics/fx/hit_glow.png";
    private static final float CORONA_SCALE = 2.8f;
    private static final float BREACH_CORONA_SCALE = 3.8f;
    private static final float DULL_CORONA_ALPHA = 0.16f;
    private static final float FULL_CORONA_ALPHA = 0.55f;

    private final AtlasTreeBodies bodies;
    private final AtlasBodyPainter painter = new AtlasBodyPainter();
    private final NodeRingArt ringArt = new NodeRingArt();
    private final NodeBeams beams = new NodeBeams(ringArt);
    private final ConnectorPainter connectors;
    private final Color[] colours;
    private final Color[] siteLights;
    private final float[] spinSeeds;
    private final AtlasShipGroup[] shipGroups;
    private final float[] pathXs = new float[MAX_PATH_POINTS];
    private final float[] pathYs = new float[MAX_PATH_POINTS];
    private final SpriteCache spriteCache = new SpriteCache(AtlasTreeView.class);
    private final TexturedQuadBatch coronaBatch = new TexturedQuadBatch();
    private final int[] wheelMembers;
    private final int[] wheelAllocated;
    private final float[] stormSeconds;
    private final AtlasBodyPainter.Lighting[] lightings;
    private boolean[] allocated = new boolean[0];
    private double elapsedSeconds;

    AtlasTreeView(AtlasTreeBodies bodies, SkillTreePanelStyle panelStyle) {
        this.bodies = bodies;
        this.connectors = new ConnectorPainter(panelStyle, beams);
        int count = bodies.bodies().size();
        this.colours = new Color[count];
        this.siteLights = new Color[count];
        this.spinSeeds = new float[count];
        this.shipGroups = new AtlasShipGroup[count];
        this.wheelMembers = new int[count];
        this.wheelAllocated = new int[count];
        this.stormSeconds = new float[count];
        this.lightings = new AtlasBodyPainter.Lighting[count];
        Arrays.fill(lightings, AtlasBodyPainter.Lighting.DULL);
        for (Body body : bodies.bodies()) {
            colours[body.index()] = parseColour(body.colour(), DEFAULT_COLOUR);
            siteLights[body.index()] = parseColour(body.art().lightColour(), null);
            spinSeeds[body.index()] = Math.floorMod(body.nodeId().hashCode(), 360);
            shipGroups[body.index()] = shipGroupOf(body);
        }
        measureWheels();
    }

    private void measureWheels() {
        for (Body body : bodies.bodies()) {
            if (body.wheel() != AtlasTreeBodies.NO_WHEEL) {
                wheelMembers[body.wheel()]++;
            }
        }
        for (Body body : bodies.bodies()) {
            if (wheelMembers[body.index()] > 0) {
                wheelMembers[body.index()]++;
            }
        }
    }

    private int wheelCentreOf(Body body) {
        if (body.wheel() != AtlasTreeBodies.NO_WHEEL) {
            return body.wheel();
        }
        return wheelMembers[body.index()] > 0 ? body.index() : AtlasTreeBodies.NO_WHEEL;
    }

    private static AtlasShipGroup shipGroupOf(Body body) {
        AtlasBodyArt art = body.art();
        AtlasShipGroup.Mode mode = switch (art.kind()) {
            case FLEET -> AtlasShipGroup.Mode.UNDER_WAY;
            case DERELICTS -> AtlasShipGroup.Mode.DORMANT;
            case SHROUDED -> AtlasShipGroup.Mode.SWIMMING;
            case STATION -> AtlasShipGroup.Mode.STATION;
            case SWARMED_PLANET -> AtlasShipGroup.Mode.CIRCLING;
            default -> null;
        };
        return mode == null ? null : new AtlasShipGroup(mode, art.hullIds(), art.size(), body.nodeId().hashCode());
    }

    private static Color parseColour(String hex, Color fallback) {
        if (hex == null || hex.length() != 7 || hex.charAt(0) != '#') {
            return fallback;
        }
        try {
            return new Color(Integer.parseInt(hex.substring(1), 16));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    void advance(float amount) {
        elapsedSeconds = (elapsedSeconds + amount) % ANIMATION_PERIOD_SECONDS;
        ringArt.advance(amount);
        for (Body body : bodies.bodies()) {
            int index = body.index();
            float pace = StormWeather.pace(lightings[index]);
            stormSeconds[index] = (float) ((stormSeconds[index] + amount * pace) % ANIMATION_PERIOD_SECONDS);
            AtlasShipGroup group = shipGroups[index];
            if (group != null) {
                group.advance(body.art().kind() == AtlasBodyArt.Kind.SWARMED_PLANET ? amount * pace : amount, body.art().heading());
            }
        }
    }

    float x(String nodeId) {
        Body body = bodies.body(nodeId);
        return body == null ? 0f : body.x();
    }

    float y(String nodeId) {
        Body body = bodies.body(nodeId);
        return body == null ? 0f : body.y();
    }

    String nodeAt(TreeViewport viewport, float screenX, float screenY) {
        String closest = null;
        float closestDistance = Float.MAX_VALUE;
        for (Body body : bodies.bodies()) {
            if (body.art().kind() == AtlasBodyArt.Kind.STAR) {
                continue;
            }
            float hitScale = body.art().isShipGroup() ? SHIP_GROUP_HIT_SCALE : HIT_SCALE;
            float hitRadius = Math.max(body.art().size() * viewport.zoom() * hitScale, MIN_HIT_PIXELS);
            float distance = (float) Math.hypot(screenX - viewport.screenX(body.x()), screenY - viewport.screenY(body.y()));
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
        float seconds = (float) elapsedSeconds;
        if (allocated.length != bodies.bodies().size()) {
            allocated = new boolean[bodies.bodies().size()];
        }
        Arrays.fill(wheelAllocated, 0);
        for (Body body : bodies.bodies()) {
            allocated[body.index()] = states.allocated(body.nodeId());
            int centre = wheelCentreOf(body);
            if (allocated[body.index()] && centre != AtlasTreeBodies.NO_WHEEL) {
                wheelAllocated[centre]++;
            }
        }
        renderCoronas(viewport, alpha);
        for (Body body : bodies.bodies()) {
            renderBody(viewport, body, states, body.nodeId().equals(hoveredNodeId), seconds, alpha);
        }
        for (Link link : bodies.links()) {
            addLink(viewport, link, alpha);
        }
        beams.flush();
    }

    private void renderCoronas(TreeViewport viewport, float alpha) {
        SpriteAPI glow = spriteCache.texture(CORONA_PATH);
        if (glow == null) {
            return;
        }
        try (TexturedQuadBatch quads = coronaBatch.beginAdditive(glow)) {
            for (Body body : bodies.bodies()) {
                int index = body.index();
                if (wheelMembers[index] == 0) {
                    continue;
                }
                float screenX = viewport.screenX(body.x());
                float screenY = viewport.screenY(body.y());
                float scale = body.art().kind() == AtlasBodyArt.Kind.BREACH ? BREACH_CORONA_SCALE : CORONA_SCALE;
                float size = screenRadiusOf(body, viewport) * scale;
                if (!viewport.isVisible(screenX, screenY, size / 2f)) {
                    continue;
                }
                float share = wheelAllocated[index] / (float) wheelMembers[index];
                float coronaAlpha = DULL_CORONA_ALPHA + (FULL_CORONA_ALPHA - DULL_CORONA_ALPHA) * share;
                quads.quadAtCenter(screenX, screenY, size, size, 0f, colours[index], alpha * coronaAlpha);
            }
        }
    }

    private void addLink(TreeViewport viewport, Link link, float alpha) {
        AtlasLinkPath path = link.path();
        float left = viewport.screenX(path.minX());
        float right = viewport.screenX(path.maxX());
        float top = viewport.screenY(path.maxY());
        float bottom = viewport.screenY(path.minY());
        if (!viewport.overlaps(Math.min(left, right), Math.min(top, bottom), Math.max(left, right), Math.max(top, bottom))) {
            return;
        }
        Body first = bodies.bodies().get(link.first());
        Body second = bodies.bodies().get(link.second());
        float startGap = gapOf(first, viewport);
        float endGap = gapOf(second, viewport);
        ConnectorStyle style = style(viewport, link, alpha);
        if (path.size() == 2) {
            connectors.paintStraight(new ConnectorEndpoint(viewport.screenX(path.x(0)), viewport.screenY(path.y(0)), startGap),
                    new ConnectorEndpoint(viewport.screenX(path.x(1)), viewport.screenY(path.y(1)), endGap), style);
            return;
        }
        int kept = 0;
        int last = Math.min(path.size(), MAX_PATH_POINTS) - 1;
        for (int point = 0; point <= last; point++) {
            float screenX = viewport.screenX(path.x(point));
            float screenY = viewport.screenY(path.y(point));
            boolean close = kept > 0 && Math.hypot(screenX - pathXs[kept - 1], screenY - pathYs[kept - 1]) < MIN_PATH_STEP_PIXELS;
            if (close && point != last) {
                continue;
            }
            if (close && kept > 1) {
                kept--;
            }
            pathXs[kept] = screenX;
            pathYs[kept] = screenY;
            kept++;
        }
        connectors.paintPath(pathXs, pathYs, kept, lengthAlongPath(kept, true, startGap), lengthAlongPath(kept, false, endGap), style);
    }

    private float lengthAlongPath(int count, boolean fromStart, float reach) {
        int step = fromStart ? 1 : -1;
        int end = fromStart ? 0 : count - 1;
        float walked = 0f;
        for (int point = end; fromStart ? point + 1 < count : point > 0; point += step) {
            int next = point + step;
            float stepX = pathXs[next] - pathXs[point];
            float stepY = pathYs[next] - pathYs[point];
            float stepLength = (float) Math.hypot(stepX, stepY);
            if (stepLength <= 0f) {
                continue;
            }
            if (Math.hypot(pathXs[next] - pathXs[end], pathYs[next] - pathYs[end]) >= reach) {
                float offsetX = pathXs[point] - pathXs[end];
                float offsetY = pathYs[point] - pathYs[end];
                float half = (offsetX * stepX + offsetY * stepY) / stepLength;
                float rest = offsetX * offsetX + offsetY * offsetY - reach * reach;
                float along = -half + (float) Math.sqrt(Math.max(0f, half * half - rest));
                return walked + Math.max(0f, Math.min(stepLength, along));
            }
            walked += stepLength;
        }
        return walked;
    }

    private ConnectorStyle style(TreeViewport viewport, Link link, float alpha) {
        ConnectorKind kind = allocated[link.first()] && allocated[link.second()] ? ConnectorKind.GLOW : ConnectorKind.DULL;
        return new ConnectorStyle(new ConnectorFade(kind, false, false, false, false), EdgeFill.NONE, viewport.zoom(), alpha);
    }

    private static float screenRadiusOf(Body body, TreeViewport viewport) {
        return Math.max(body.art().size() * viewport.zoom(), MIN_BODY_PIXELS);
    }

    private static float ringRadiusOf(Body body, TreeViewport viewport) {
        float scale = body.highway() ? HIGHWAY_RING_SCALE : BODY_RING_SCALE;
        return NodeBeams.huggingRingRadius(screenRadiusOf(body, viewport) * scale, NodeBeams.widthScale(viewport.zoom()));
    }

    private static float gapOf(Body body, TreeViewport viewport) {
        if (body.art().kind() == AtlasBodyArt.Kind.STAR) {
            return screenRadiusOf(body, viewport) * STAR_GAP_SCALE;
        }
        float ringOuterEdge = NodeBeams.ringOuterEdge(ringRadiusOf(body, viewport), NodeBeams.widthScale(viewport.zoom()));
        return NodeBeams.laneReach(ringOuterEdge, SkillTreeNodeGeometry.laneWidthScale(viewport.zoom()));
    }

    private static float spinSpeed(Body body) {
        float share = body.art().isPlanet() ? 1f : OBJECT_SPIN_SHARE;
        return SPIN_DEGREES_PER_SECOND * share * body.art().spinRate() * (body.index() % 2 == 0 ? 1f : -1f);
    }

    private void renderBody(TreeViewport viewport, Body body, States states, boolean hovered, float seconds, float alpha) {
        int index = body.index();
        float screenX = viewport.screenX(body.x());
        float screenY = viewport.screenY(body.y());
        float screenRadius = screenRadiusOf(body, viewport);
        if (!viewport.isVisible(screenX, screenY, screenRadius * HOVER_RING_SCALE * 2f)) {
            return;
        }
        boolean lit = allocated[index];
        boolean available = !lit && states.allocatable(body.nodeId());
        AtlasBodyPainter.Lighting lighting = lit ? AtlasBodyPainter.Lighting.LIT
                : available ? AtlasBodyPainter.Lighting.AVAILABLE : AtlasBodyPainter.Lighting.DULL;
        lightings[index] = lighting;
        float heading = body.art().heading();
        float spin = spinSeeds[index] + seconds * spinSpeed(body);
        AtlasBodyPainter.Placement placement = new AtlasBodyPainter.Placement(screenX, screenY, screenRadius, heading, spin, spinSeeds[index],
                stormSeconds[index]);
        painter.paint(body.art(), colours[index], siteLights[index], placement, lighting, seconds, alpha);
        AtlasShipGroup group = shipGroups[index];
        if (group != null) {
            float shipAlpha = lit ? 1f : available ? AVAILABLE_SHIP_ALPHA : DULL_SHIP_ALPHA;
            group.render(screenX, screenY, viewport.zoom(), heading, alpha * shipAlpha);
        }
        if (body.art().kind() == AtlasBodyArt.Kind.STAR) {
            return;
        }
        float widthScale = NodeBeams.widthScale(viewport.zoom());
        float ringRadius = ringRadiusOf(body, viewport);
        NodeBeams.RingLight light = lit ? NodeBeams.RingLight.LIT : available ? NodeBeams.RingLight.BREATHING : NodeBeams.RingLight.DULL;
        beams.nodeRing(screenX, screenY, ringRadius, CIRCLE, light, colours[index], body.nodeId(), alpha, widthScale);
        if (hovered) {
            beams.ring(screenX, screenY, ringRadius * HOVER_RING_SCALE, CIRCLE, beams.lit(HOVER_COLOUR), alpha * HOVER_RING_ALPHA, widthScale);
        }
    }
}
