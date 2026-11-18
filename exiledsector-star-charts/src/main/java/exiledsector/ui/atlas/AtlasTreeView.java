package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt;
import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.charts.tree.AtlasTreeBodies.Link;
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

import java.awt.Color;

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
    private static final float LINK_GAP_SCALE = 1.05f;
    private static final float HIGHWAY_RING_SCALE = 1.35f;
    private static final float MIN_PATH_STEP_PIXELS = 4f;
    private static final int ARC_POINTS = 24;
    private static final float AVAILABLE_RING_SCALE = 1.45f;
    private static final float HOVER_RING_SCALE = 1.65f;
    private static final float HOVER_RING_ALPHA = 0.8f;
    private static final float SPIN_DEGREES_PER_SECOND = 12f;
    private static final float DULL_SHIP_ALPHA = 0.55f;
    private static final float AVAILABLE_SHIP_ALPHA = 0.8f;
    private static final double ANIMATION_PERIOD_SECONDS = 3600.0;
    private static final float[] CIRCLE = NodeRingArt.outlineRadii((cos, sin) -> 1f);

    private final AtlasTreeBodies bodies;
    private final AtlasBodyPainter painter = new AtlasBodyPainter();
    private final NodeRingArt ringArt = new NodeRingArt();
    private final NodeBeams beams = new NodeBeams(ringArt);
    private final ConnectorPainter connectors;
    private final Color[] colours;
    private final Color[] siteLights;
    private final float[] spinSeeds;
    private final AtlasShipGroup[] shipGroups;
    private final float[] arcXs = new float[ARC_POINTS];
    private final float[] arcYs = new float[ARC_POINTS];
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
        for (Body body : bodies.bodies()) {
            colours[body.index()] = parseColour(body.colour(), DEFAULT_COLOUR);
            siteLights[body.index()] = parseColour(body.art().lightColour(), null);
            spinSeeds[body.index()] = Math.floorMod(body.nodeId().hashCode(), 360);
            shipGroups[body.index()] = shipGroupOf(body);
        }
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
            AtlasShipGroup group = shipGroups[body.index()];
            if (group != null) {
                group.advance(amount, body.art().heading());
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
        for (Body body : bodies.bodies()) {
            allocated[body.index()] = states.allocated(body.nodeId());
        }
        for (Body body : bodies.bodies()) {
            renderBody(viewport, body, states, body.nodeId().equals(hoveredNodeId), seconds, alpha);
        }
        for (Link link : bodies.links()) {
            if (link.alongRing()) {
                addArc(viewport, link, alpha);
            } else {
                addStraight(viewport, link, alpha);
            }
        }
        beams.flush();
    }

    private void addStraight(TreeViewport viewport, Link link, float alpha) {
        Body first = bodies.bodies().get(link.first());
        Body second = bodies.bodies().get(link.second());
        float firstX = viewport.screenX(first.x());
        float firstY = viewport.screenY(first.y());
        float secondX = viewport.screenX(second.x());
        float secondY = viewport.screenY(second.y());
        if (!viewport.overlaps(Math.min(firstX, secondX), Math.min(firstY, secondY), Math.max(firstX, secondX), Math.max(firstY, secondY))) {
            return;
        }
        connectors.paintStraight(new ConnectorEndpoint(firstX, firstY, gapOf(first, viewport)), new ConnectorEndpoint(secondX, secondY,
                gapOf(second, viewport)), style(viewport, link, alpha));
    }

    private void addArc(TreeViewport viewport, Link link, float alpha) {
        Body star = bodies.star();
        Body first = bodies.bodies().get(link.first());
        Body second = bodies.bodies().get(link.second());
        float radius = bodies.ringRadius(link);
        double fromTurn = Math.atan2(first.y() - star.y(), first.x() - star.x());
        double toTurn = Math.atan2(second.y() - star.y(), second.x() - star.x());
        double sweep = Math.IEEEremainder(toTurn - fromTurn, Math.PI * 2.0);
        float middleX = viewport.screenX(star.x() + radius * (float) Math.cos(fromTurn + sweep / 2.0));
        float middleY = viewport.screenY(star.y() + radius * (float) Math.sin(fromTurn + sweep / 2.0));
        if (!viewport.isVisible(middleX, middleY, radius * viewport.zoom() * (float) Math.abs(sweep) * 2f)) {
            return;
        }
        int kept = 0;
        for (int point = 0; point < ARC_POINTS; point++) {
            double turn = fromTurn + sweep * point / (ARC_POINTS - 1);
            float screenX = viewport.screenX(star.x() + radius * (float) Math.cos(turn));
            float screenY = viewport.screenY(star.y() + radius * (float) Math.sin(turn));
            boolean last = point == ARC_POINTS - 1;
            boolean close = kept > 0 && Math.hypot(screenX - arcXs[kept - 1], screenY - arcYs[kept - 1]) < MIN_PATH_STEP_PIXELS;
            if (close && !last) {
                continue;
            }
            if (close && kept > 1) {
                kept--;
            }
            arcXs[kept] = screenX;
            arcYs[kept] = screenY;
            kept++;
        }
        connectors.paintPath(arcXs, arcYs, kept, gapOf(first, viewport), gapOf(second, viewport), style(viewport, link, alpha));
    }

    private ConnectorStyle style(TreeViewport viewport, Link link, float alpha) {
        ConnectorKind kind = allocated[link.first()] && allocated[link.second()] ? ConnectorKind.GLOW : ConnectorKind.DULL;
        return new ConnectorStyle(new ConnectorFade(kind, false, false, false, false), EdgeFill.NONE, viewport.zoom(), alpha);
    }

    private static float gapOf(Body body, TreeViewport viewport) {
        float screenRadius = Math.max(body.art().size() * viewport.zoom(), MIN_BODY_PIXELS);
        return screenRadius * (body.highway() ? HIGHWAY_RING_SCALE : LINK_GAP_SCALE);
    }

    private void renderBody(TreeViewport viewport, Body body, States states, boolean hovered, float seconds, float alpha) {
        int index = body.index();
        float screenX = viewport.screenX(body.x());
        float screenY = viewport.screenY(body.y());
        float screenRadius = Math.max(body.art().size() * viewport.zoom(), MIN_BODY_PIXELS);
        if (!viewport.isVisible(screenX, screenY, screenRadius * HOVER_RING_SCALE * 2f)) {
            return;
        }
        boolean lit = allocated[index];
        boolean available = !lit && states.allocatable(body.nodeId());
        AtlasBodyPainter.Lighting lighting = lit ? AtlasBodyPainter.Lighting.LIT
                : available ? AtlasBodyPainter.Lighting.AVAILABLE : AtlasBodyPainter.Lighting.DULL;
        float heading = body.art().heading();
        float spin = spinSeeds[index] + seconds * SPIN_DEGREES_PER_SECOND * body.art().spinRate() * (index % 2 == 0 ? 1f : -1f);
        AtlasBodyPainter.Placement placement = new AtlasBodyPainter.Placement(screenX, screenY, screenRadius, heading, spin, spinSeeds[index]);
        painter.paint(body.art(), colours[index], siteLights[index], placement, lighting, seconds, alpha);
        AtlasShipGroup group = shipGroups[index];
        if (group != null) {
            float shipAlpha = lit ? 1f : available ? AVAILABLE_SHIP_ALPHA : DULL_SHIP_ALPHA;
            group.render(screenX, screenY, viewport.zoom(), heading, alpha * shipAlpha);
        }
        float widthScale = NodeBeams.widthScale(viewport.zoom());
        if (body.highway()) {
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
}
