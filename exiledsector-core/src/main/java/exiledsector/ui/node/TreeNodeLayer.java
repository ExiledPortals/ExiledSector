package exiledsector.ui.node;

import exiledsector.skills.SkillTier;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.TreeNode;
import exiledsector.skills.layout.TreeTopology;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.node.ConnectorPainter.ConnectorEndpoint;
import exiledsector.ui.node.ConnectorPainter.ConnectorFade;
import exiledsector.ui.node.ConnectorPainter.ConnectorStyle;
import exiledsector.ui.node.ConnectorPainter.EdgeFill;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;

import static exiledsector.ui.node.NodeMetrics.ICON_INSET_RATIO;
import static exiledsector.ui.node.NodeMetrics.NODE_SIZE;
import static exiledsector.ui.node.SkillTreeNodeGeometry.laneWidthScale;

public final class TreeNodeLayer<N extends TreeNode> {

    public interface NodeStates<N> {

        boolean allocated(N node);

        boolean allocatable(N node);
    }

    private static final Color ALLOCATED_TINT = Color.WHITE;
    private static final Color UNALLOCATED_TINT = new Color(90, 90, 90);
    private static final float ROOT_ICON_SHARE = 0.68f;

    private final SpriteCache spriteCache = new SpriteCache(TreeNodeLayer.class);
    private final WormholeOpenness wormholeOpenness = new WormholeOpenness();
    private final SkillTreeNodeRingRenderer ringRenderer;
    private final ConnectorPainter connectorPainter;

    public TreeNodeLayer(SkillTreePanelStyle panelStyle) {
        this.ringRenderer = new SkillTreeNodeRingRenderer(panelStyle, wormholeOpenness);
        this.connectorPainter = new ConnectorPainter(panelStyle, ringRenderer.beams());
    }

    public void advance(float amount, TreeTopology<N> topology, NodeStates<N> states) {
        ringRenderer.advance(amount);
        wormholeOpenness.advance(amount, topology.wormholes(), nodeId -> {
            N node = topology.node(nodeId);
            return node != null && states.allocated(node);
        });
    }

    public void pulse(String nodeId) {
        ringRenderer.startPulse(nodeId);
    }

    public void render(TreeViewport viewport, TreeTopology<N> topology, NodeStates<N> states, float alphaMult) {
        if (alphaMult <= 0f) {
            return;
        }
        for (TreeTopology.Connector<N> connector : topology.connectors()) {
            renderConnector(viewport, connector, states, alphaMult);
        }
        for (N node : topology.nodes()) {
            renderNode(viewport, node, states, alphaMult);
        }
    }

    public N nodeAt(TreeViewport viewport, TreeTopology<N> topology, float screenX, float screenY) {
        for (N node : topology.nodes()) {
            float halfSize = NODE_SIZE * viewport.zoom() * node.getTier().getSizeMultiplier() / 2f;
            if (Math.abs(screenX - viewport.screenX(node.getX())) <= halfSize && Math.abs(screenY - viewport.screenY(node.getY())) <= halfSize) {
                return node;
            }
        }
        return null;
    }

    private void renderNode(TreeViewport viewport, N node, NodeStates<N> states, float alphaMult) {
        SkillTier tier = node.getTier();
        float zoom = viewport.zoom();
        float nodeX = viewport.screenX(node.getX());
        float nodeY = viewport.screenY(node.getY());
        float footprintSize = NODE_SIZE * zoom * tier.getSizeMultiplier();
        if (!viewport.isVisible(nodeX, nodeY, ringRenderer.reach(footprintSize, node))) {
            return;
        }
        boolean allocated = states.allocated(node);
        boolean breathing = !allocated && states.allocatable(node);
        ringRenderer.draw(nodeX, nodeY, footprintSize, alphaMult, SkillTreeNodeRingRenderer.RingState.of(allocated, breathing), zoom, node);
        if (tier.isWormhole()) {
            return;
        }
        String iconPath = node.getIconPath();
        if (iconPath == null || iconPath.isBlank()) {
            return;
        }
        float iconSize = footprintSize * (tier == SkillTier.ROOT ? ROOT_ICON_SHARE : ICON_INSET_RATIO);
        SpriteDraw.drawAtCenter(spriteCache, iconPath, nodeX, nodeY, iconSize, iconSize, allocated ? ALLOCATED_TINT : UNALLOCATED_TINT, alphaMult);
    }

    private void renderConnector(TreeViewport viewport, TreeTopology.Connector<N> connector, NodeStates<N> states, float alphaMult) {
        float cullMargin = 2f * NodeBeams.laneEdge(laneWidthScale(viewport.zoom()));
        if (!viewport.overlaps(viewport.screenX(connector.minX()) - cullMargin, viewport.screenY(connector.maxY()) - cullMargin,
                viewport.screenX(connector.maxX()) + cullMargin, viewport.screenY(connector.minY()) + cullMargin)) {
            return;
        }
        N node = connector.from();
        N other = connector.to();
        ConnectorCurve curve = connector.curve();
        float zoom = viewport.zoom();
        float nodeX = viewport.screenX(node.getX());
        float nodeY = viewport.screenY(node.getY());
        float otherX = viewport.screenX(other.getX());
        float otherY = viewport.screenY(other.getY());
        boolean nodeAllocated = states.allocated(node);
        boolean otherAllocated = states.allocated(other);
        ConnectorFade fade = new ConnectorFade(ConnectorKind.of(nodeAllocated && otherAllocated, false, false),
                other.getTier().isWormhole(), node.getTier().isWormhole(),
                other.getTier().isWormhole() && otherAllocated, node.getTier().isWormhole() && nodeAllocated);
        ConnectorEndpoint nodeEndpoint = new ConnectorEndpoint(nodeX, nodeY, ConnectorPainter.nodeEndpointRadius(node.getTier(), zoom,
                wormholeOpenness.of(node.getId())));
        ConnectorEndpoint otherEndpoint = new ConnectorEndpoint(otherX, otherY, ConnectorPainter.nodeEndpointRadius(other.getTier(), zoom,
                wormholeOpenness.of(other.getId())));
        connectorPainter.clearBubbles();
        ConnectorStyle style = new ConnectorStyle(fade, EdgeFill.NONE, zoom, alphaMult);
        if (curve == null) {
            connectorPainter.paintStraight(otherEndpoint, nodeEndpoint, style);
        } else {
            connectorPainter.paintCurved(otherEndpoint, viewport.screenX(curve.getControlOffsetX()), viewport.screenY(curve.getControlOffsetY()), nodeEndpoint,
                    style);
        }
    }
}
