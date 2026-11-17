package exiledsector.ui.atlas;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.charts.tree.AtlasBodyArt;
import exiledsector.charts.tree.AtlasOrbitLayout;
import exiledsector.charts.tree.AtlasOrbitSystem;
import exiledsector.charts.tree.AtlasOrbitSystem.Body;
import exiledsector.charts.tree.AtlasOrbitSystem.Link;
import exiledsector.charts.tree.AtlasOrbitSystem.Track;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.RingBeltRenderer;
import exiledsector.ui.decoration.AtlasShipGroup;
import exiledsector.ui.node.ConnectorKind;
import exiledsector.ui.node.ConnectorPainter;
import exiledsector.ui.node.ConnectorPainter.ConnectorEndpoint;
import exiledsector.ui.node.ConnectorPainter.ConnectorFade;
import exiledsector.ui.node.ConnectorPainter.ConnectorStyle;
import exiledsector.ui.node.ConnectorPainter.EdgeFill;
import exiledsector.ui.node.NodeBeams;
import exiledsector.ui.node.NodeRingArt;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class AtlasOrbitView {

    interface States {

        boolean allocated(String nodeId);

        boolean allocatable(String nodeId);
    }

    private static final Color DEFAULT_COLOUR = new Color(200, 210, 230);
    private static final Color HOVER_COLOUR = Color.WHITE;
    private static final String ASTEROID_BELT_PATH = "graphics/exiledSector/scenery/rings/ring_band_asteroids.png";
    private static final String ICE_BELT_PATH = "graphics/exiledSector/scenery/rings/ring_band_ice.png";
    private static final float BELT_HALF_WIDTH = 75f;
    private static final float BELT_ALPHA = 0.9f;
    private static final float MIN_BODY_PIXELS = 2.5f;
    private static final float MIN_HIT_PIXELS = 8f;
    private static final float HIT_SCALE = 1.15f;
    private static final float SHIP_GROUP_HIT_SCALE = 1.1f;
    private static final float LINK_GAP_SCALE = 1.05f;
    private static final float HIGHWAY_RING_SCALE = 1.35f;
    private static final float AVAILABLE_RING_SCALE = 1.45f;
    private static final float HOVER_RING_SCALE = 1.65f;
    private static final float HOVER_RING_ALPHA = 0.8f;
    private static final float ROUTE_BEND_SHARE = 0.15f;
    private static final float HOVER_TIME_SCALE = 0.25f;
    private static final float HOLD_EASE_PER_SECOND = 3f;
    private static final float SPIN_DEGREES_PER_SECOND = 12f;
    private static final float DULL_SHIP_ALPHA = 0.55f;
    private static final float AVAILABLE_SHIP_ALPHA = 0.8f;
    private static final float[] CIRCLE = NodeRingArt.outlineRadii((cos, sin) -> 1f);

    private static double orbitSeconds;

    private final AtlasOrbitSystem system;
    private final AtlasOrbitSystem.Positions positions;
    private final AtlasBodyPainter painter = new AtlasBodyPainter();
    private final SpriteCache beltSprites = new SpriteCache(AtlasOrbitView.class);
    private final NodeRingArt ringArt = new NodeRingArt();
    private final NodeBeams beams = new NodeBeams(ringArt);
    private final ConnectorPainter connectors;
    private final Color[] colours;
    private final Color[] siteLights;
    private final float[] spinSeeds;
    private final AtlasOrbitLayout layout;
    private final AtlasShipGroup[] shipGroups;
    private final List<Wheel> wheels = new ArrayList<>();
    private final float[] pathXs = new float[Math.max(AtlasWheelRoute.APPROACH_POINTS, AtlasWheelRoute.SPIRAL_POINTS)];
    private final float[] pathYs = new float[Math.max(AtlasWheelRoute.APPROACH_POINTS, AtlasWheelRoute.SPIRAL_POINTS)];
    private boolean[] allocated = new boolean[0];
    private float timeScale = 1f;

    private record Wheel(AtlasOrbitLayout.Wheel bodies, AtlasWheelRoute route) {
    }

    AtlasOrbitView(AtlasOrbitSystem system, SkillTreePanelStyle panelStyle) {
        this.system = system;
        this.positions = system.newPositions();
        this.connectors = new ConnectorPainter(panelStyle, beams);
        List<Body> bodies = system.bodies();
        this.colours = new Color[bodies.size()];
        this.siteLights = new Color[bodies.size()];
        this.spinSeeds = new float[bodies.size()];
        this.shipGroups = new AtlasShipGroup[bodies.size()];
        this.layout = AtlasOrbitLayout.of(system);
        for (Body body : bodies) {
            colours[body.index()] = parseColour(body.colour(), DEFAULT_COLOUR);
            siteLights[body.index()] = parseColour(body.art().lightColour(), null);
            spinSeeds[body.index()] = Math.floorMod(body.nodeId().hashCode(), 360);
            shipGroups[body.index()] = shipGroupOf(body);
        }
        for (AtlasOrbitLayout.Wheel wheel : layout.wheels()) {
            wheels.add(new Wheel(wheel, new AtlasWheelRoute()));
        }
        system.positionsAt(orbitSeconds, positions);
        updateRoutes();
    }

    private static AtlasShipGroup shipGroupOf(Body body) {
        AtlasBodyArt art = body.art();
        AtlasShipGroup.Mode mode = switch (art.kind()) {
            case FLEET -> AtlasShipGroup.Mode.UNDER_WAY;
            case DERELICTS -> AtlasShipGroup.Mode.DORMANT;
            case STATION -> AtlasShipGroup.Mode.STATION;
            default -> null;
        };
        return mode == null ? null : new AtlasShipGroup(mode, art.hullIds(), art.size(), body.nodeId().hashCode());
    }

    static Color parseColour(String hex, Color fallback) {
        if (hex == null || hex.length() != 7 || hex.charAt(0) != '#') {
            return fallback;
        }
        try {
            return new Color(Integer.parseInt(hex.substring(1), 16));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    void advance(float amount, boolean holding) {
        float target = holding ? HOVER_TIME_SCALE : 1f;
        float step = HOLD_EASE_PER_SECOND * amount;
        timeScale = timeScale < target ? Math.min(target, timeScale + step) : Math.max(target, timeScale - step);
        orbitSeconds += amount * timeScale;
        system.positionsAt(orbitSeconds, positions);
        ringArt.advance(amount);
        for (Wheel wheel : wheels) {
            wheel.route().advance(amount);
        }
        updateRoutes();
        for (Body body : system.bodies()) {
            AtlasShipGroup group = shipGroups[body.index()];
            if (group != null) {
                group.advance(amount * timeScale, headingOf(body));
            }
        }
    }

    private void updateRoutes() {
        for (Wheel wheel : wheels) {
            AtlasOrbitLayout.Wheel bodies = wheel.bodies();
            int highway = bodies.highway() >= 0 ? bodies.highway() : bodies.outer();
            wheel.route().update(new AtlasWheelRoute.Wheel(positions.x(highway), positions.y(highway), positions.x(bodies.centre()),
                    positions.y(bodies.centre()), positions.x(bodies.outer()), positions.y(bodies.outer()), positions.x(bodies.inner()),
                    positions.y(bodies.inner())));
        }
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
            float hitScale = body.art().isShipGroup() ? SHIP_GROUP_HIT_SCALE : HIT_SCALE;
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
        if (allocated.length != system.bodies().size()) {
            allocated = new boolean[system.bodies().size()];
        }
        for (Body body : system.bodies()) {
            allocated[body.index()] = states.allocated(body.nodeId());
        }
        renderBelts(viewport, alpha);
        for (Body body : system.bodies()) {
            renderBody(viewport, body, states, body.nodeId().equals(hoveredNodeId), seconds, alpha);
        }
        for (Link ray : layout.rays()) {
            addRay(viewport, ray, alpha);
        }
        for (Wheel wheel : wheels) {
            addWheel(viewport, wheel, alpha);
        }
        beams.flush();
    }

    private void renderBelts(TreeViewport viewport, float alpha) {
        float zoom = viewport.zoom();
        for (AtlasOrbitLayout.Belt belt : layout.belts()) {
            Track track = belt.track();
            float centreX = viewport.screenX(positions.x(track.centreIndex()));
            float centreY = viewport.screenY(positions.y(track.centreIndex()));
            float outer = (track.radius() + BELT_HALF_WIDTH) * zoom;
            if (!viewport.overlaps(centreX - outer, centreY - outer, centreX + outer, centreY + outer)) {
                continue;
            }
            SpriteAPI texture = beltSprites.texture(belt.outermost() ? ICE_BELT_PATH : ASTEROID_BELT_PATH);
            if (texture == null) {
                continue;
            }
            float turn = positions.turnDegrees(track.memberIndexes().get(0));
            RadialBand band = new RadialBand(new Vector2f(centreX, centreY), (track.radius() - BELT_HALF_WIDTH) * zoom, outer);
            RingBeltRenderer.render(texture, band, Color.WHITE, alpha * BELT_ALPHA, -turn, viewport);
        }
    }

    private void addRay(TreeViewport viewport, Link ray, float alpha) {
        float firstX = viewport.screenX(positions.x(ray.first()));
        float firstY = viewport.screenY(positions.y(ray.first()));
        float secondX = viewport.screenX(positions.x(ray.second()));
        float secondY = viewport.screenY(positions.y(ray.second()));
        if (!viewport.overlaps(Math.min(firstX, secondX), Math.min(firstY, secondY), Math.max(firstX, secondX), Math.max(firstY, secondY))) {
            return;
        }
        connectors.paintStraight(new ConnectorEndpoint(firstX, firstY, gapOf(ray.first(), viewport)),
                new ConnectorEndpoint(secondX, secondY, gapOf(ray.second(), viewport)), style(viewport, ray.first(), ray.second(), alpha));
    }

    private void addWheel(TreeViewport viewport, Wheel wheel, float alpha) {
        AtlasOrbitLayout.Wheel bodies = wheel.bodies();
        float centreX = viewport.screenX(positions.x(bodies.centre()));
        float centreY = viewport.screenY(positions.y(bodies.centre()));
        boolean hasHighway = bodies.highway() >= 0;
        int reachIndex = hasHighway ? bodies.highway() : bodies.outer();
        float reach = (float) Math.hypot(viewport.screenX(positions.x(reachIndex)) - centreX, viewport.screenY(positions.y(reachIndex)) - centreY)
                + (float) Math.hypot(viewport.screenX(positions.x(bodies.outer())) - centreX, viewport.screenY(positions.y(bodies.outer())) - centreY);
        if (!viewport.isVisible(centreX, centreY, reach * 2f)) {
            return;
        }
        AtlasWheelRoute route = wheel.route();
        route.trace();
        if (hasHighway) {
            paintRoute(viewport, route.approachXs, route.approachYs, AtlasWheelRoute.APPROACH_POINTS, bodies.highway(), bodies.outer(), alpha);
        }
        paintRoute(viewport, route.spiralXs, route.spiralYs, AtlasWheelRoute.SPIRAL_POINTS, bodies.outer(), bodies.inner(), alpha);
        float innerX = positions.x(bodies.inner());
        float innerY = positions.y(bodies.inner());
        float worldCentreX = positions.x(bodies.centre());
        float worldCentreY = positions.y(bodies.centre());
        float bend = ROUTE_BEND_SHARE * route.bendSide();
        float throughX = (innerX + worldCentreX) / 2f - (worldCentreY - innerY) * bend;
        float throughY = (innerY + worldCentreY) / 2f + (worldCentreX - innerX) * bend;
        connectors.paintCurved(new ConnectorEndpoint(viewport.screenX(innerX), viewport.screenY(innerY), gapOf(bodies.inner(), viewport)),
                viewport.screenX(throughX), viewport.screenY(throughY), new ConnectorEndpoint(centreX, centreY, gapOf(bodies.centre(), viewport)),
                style(viewport, bodies.inner(), bodies.centre(), alpha));
    }

    private void paintRoute(TreeViewport viewport, float[] worldXs, float[] worldYs, int count, int from, int to, float alpha) {
        for (int point = 0; point < count; point++) {
            pathXs[point] = viewport.screenX(worldXs[point]);
            pathYs[point] = viewport.screenY(worldYs[point]);
        }
        connectors.paintPath(pathXs, pathYs, count, gapOf(from, viewport), gapOf(to, viewport), style(viewport, from, to, alpha));
    }

    private ConnectorStyle style(TreeViewport viewport, int first, int second, float alpha) {
        ConnectorKind kind = allocated[first] && allocated[second] ? ConnectorKind.GLOW : ConnectorKind.DULL;
        return new ConnectorStyle(new ConnectorFade(kind, false, false, false, false), EdgeFill.NONE, viewport.zoom(), alpha);
    }

    private float gapOf(int bodyIndex, TreeViewport viewport) {
        AtlasBodyArt art = system.bodies().get(bodyIndex).art();
        return Math.max(art.size() * viewport.zoom(), MIN_BODY_PIXELS) * LINK_GAP_SCALE;
    }

    private void renderBody(TreeViewport viewport, Body body, States states, boolean hovered, float seconds, float alpha) {
        int index = body.index();
        float screenX = viewport.screenX(positions.x(index));
        float screenY = viewport.screenY(positions.y(index));
        float screenRadius = Math.max(body.art().size() * viewport.zoom(), MIN_BODY_PIXELS);
        if (!viewport.isVisible(screenX, screenY, screenRadius * HOVER_RING_SCALE * 2f)) {
            return;
        }
        boolean lit = allocated[index];
        boolean available = !lit && states.allocatable(body.nodeId());
        AtlasBodyPainter.Lighting lighting = lit ? AtlasBodyPainter.Lighting.LIT
                : available ? AtlasBodyPainter.Lighting.AVAILABLE : AtlasBodyPainter.Lighting.DULL;
        float heading = headingOf(body);
        float spin = spinSeeds[index] + seconds * SPIN_DEGREES_PER_SECOND * body.art().spinRate() * (index % 2 == 0 ? 1f : -1f);
        AtlasBodyPainter.Placement placement = new AtlasBodyPainter.Placement(screenX, screenY, screenRadius, heading, spin, spinSeeds[index]);
        painter.paint(body.art(), colours[index], siteLights[index], placement, lighting, seconds, alpha);
        AtlasShipGroup group = shipGroups[index];
        if (group != null) {
            float shipAlpha = lit ? 1f : available ? AVAILABLE_SHIP_ALPHA : DULL_SHIP_ALPHA;
            group.render(screenX, screenY, viewport.zoom(), heading, alpha * shipAlpha);
        }
        float widthScale = NodeBeams.widthScale(viewport.zoom());
        if (layout.isHighway(index)) {
            NodeBeams.RingLight light = lit ? NodeBeams.RingLight.LIT : available ? NodeBeams.RingLight.BREATHING : NodeBeams.RingLight.DULL;
            beams.nodeRing(screenX, screenY, screenRadius * HIGHWAY_RING_SCALE, CIRCLE, light, colours[index], body.nodeId(), alpha, widthScale);
        } else if (available) {
            beams.nodeRing(screenX, screenY, screenRadius * AVAILABLE_RING_SCALE, CIRCLE, NodeBeams.RingLight.BREATHING, colours[index], body.nodeId(),
                    alpha, widthScale);
        }
        if (hovered) {
            beams.ring(screenX, screenY, screenRadius * HOVER_RING_SCALE, CIRCLE, beams.lit(HOVER_COLOUR), alpha * HOVER_RING_ALPHA, widthScale);
        }
    }

    private float headingOf(Body body) {
        if (!body.orbits()) {
            return 90f;
        }
        if (body.degreesPerSecond() == 0f) {
            return headingOf(system.bodies().get(body.centreIndex()));
        }
        int index = body.index();
        float outwardX = positions.x(index) - positions.x(body.centreIndex());
        float outwardScreenY = positions.y(body.centreIndex()) - positions.y(index);
        float outward = (float) Math.toDegrees(Math.atan2(outwardScreenY, outwardX));
        return body.degreesPerSecond() >= 0f ? outward - 90f : outward + 90f;
    }
}
