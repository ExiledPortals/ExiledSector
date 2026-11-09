package exiledsector.ui.node;

import exiledsector.items.RootConduits;
import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.TreeTopology;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeLayers;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.node.ConnectorPainter.ConnectorEndpoint;
import exiledsector.ui.node.ConnectorPainter.ConnectorFade;
import exiledsector.ui.node.ConnectorPainter.ConnectorStyle;
import exiledsector.ui.node.ConnectorPainter.EdgeFill;

import java.awt.Color;
import java.util.List;
import java.util.Set;

import static exiledsector.ui.node.NodeMetrics.NODE_SIZE;
import static exiledsector.ui.node.SkillTreeNodeGeometry.connectorEndpointRadius;
import static exiledsector.ui.node.SkillTreeNodeGeometry.laneWidthScale;

final class SkillTreeNodeConnectorRenderer {

    private static final float EXOSHIP_ENDPOINT_SHARE = 0.4f;
    private static final float GRANT_TETHER_ALPHA = 0.35f;

    private final NodeSearch nodeSearch;
    private final NodeBeams beams;
    private final ConnectorPainter painter;
    private final ConnectorFlow flow = new ConnectorFlow();
    private final WormholeOpenness wormholeOpenness;
    private final SocketWormholes socketWormholes;
    private float exoshipRadius;
    private List<String> conduitPath = List.of();
    private ShipSkillData conduitData;
    private int conduitRevision = -1;
    private Set<String> conduitNodeIds = Set.of();
    private Set<String> conduitConnectorKeys = Set.of();

    SkillTreeNodeConnectorRenderer(SkillTreePanelStyle panelStyle, NodeSearch nodeSearch, WormholeOpenness wormholeOpenness,
                                    SocketWormholes socketWormholes, NodeBeams beams) {
        this.nodeSearch = nodeSearch;
        this.wormholeOpenness = wormholeOpenness;
        this.socketWormholes = socketWormholes;
        this.beams = beams;
        this.painter = new ConnectorPainter(panelStyle, beams);
    }

    void advance(float amount) {
        flow.advance(amount);
    }

    void draw(TreeLayers layers, NodeAllocator.Snapshot allocation, Set<String> templateNodeIds, Set<String> plannedOnlyNodeIds,
              AllocationAnimation allocationAnimation) {
        TemplateHighlight templateHighlight = new TemplateHighlight(templateNodeIds, plannedOnlyNodeIds);
        exoshipRadius = layers.exoshipRadius();
        flow.refresh(allocation, plannedOnlyNodeIds);
        ShipSkillData skillData = allocation.skillData();
        if (skillData != conduitData || skillData.revision() != conduitRevision) {
            conduitData = skillData;
            conduitRevision = skillData.revision();
            refreshConduit(RootConduits.of(skillData));
        }

        for (TreeTopology.Connector<SkillNode> connector : SkillTree.topology().connectors()) {
            boolean carried = ExoshipNodes.isCarried(connector.from());
            float alphaMult = carried ? layers.carriedAlpha() : layers.treeAlpha();
            if (alphaMult > 0f && carried == ExoshipNodes.isCarried(connector.to())) {
                drawConnector(connector, carried ? layers.carriedViewport() : layers.treeViewport(), allocation, templateHighlight,
                        allocationAnimation, alphaMult);
            }
        }
    }

    private void refreshConduit(RootConduits.Conduit conduit) {
        List<String> path = conduit == null ? List.of() : conduit.path();
        if (path != conduitPath) {
            conduitPath = path;
            conduitNodeIds = Set.copyOf(path);
            conduitConnectorKeys = conduit == null ? Set.of() : conduit.connectorKeys();
        }
        if (conduit != null) {
            painter.setConduitColor(AffinityColours.of(conduit.affinity()));
        }
    }

    private boolean onConduit(SkillNode node, SkillNode other) {
        return conduitNodeIds.contains(node.getId()) && conduitNodeIds.contains(other.getId())
                && conduitConnectorKeys.contains(ConnectorCurve.key(node.getId(), other.getId()));
    }

    void drawGrantTethers(TreeLayers layers, NodeAllocator.Snapshot allocation, SkillNode socket, List<String> grantedNodeIds, Color tetherColor) {
        if (socket == null || tetherColor == null || grantedNodeIds.isEmpty()) {
            return;
        }
        exoshipRadius = layers.exoshipRadius();
        TreeViewport socketViewport = layers.viewportFor(socket);
        float socketX = socketViewport.screenX(socket.getOffsetX());
        float socketY = socketViewport.screenY(socket.getOffsetY());
        BeamStroke.Look tetherLook = beams.lit(tetherColor);
        float widthScale = laneWidthScale(socketViewport.zoom());
        for (int grantedIndex = 0; grantedIndex < grantedNodeIds.size(); grantedIndex++) {
            SkillNode granted = SkillTree.get(grantedNodeIds.get(grantedIndex));
            if (granted == null || allocation.isConcealed(granted)) {
                continue;
            }
            TreeViewport grantedViewport = layers.viewportFor(granted);
            float grantedX = grantedViewport.screenX(granted.getOffsetX());
            float grantedY = grantedViewport.screenY(granted.getOffsetY());
            float towardX = grantedX - socketX;
            float towardY = grantedY - socketY;
            float socketRadius = endpointRadius(socket, socketViewport.zoom(), towardX, towardY);
            float grantedRadius = endpointRadius(granted, grantedViewport.zoom(), -towardX, -towardY);
            float tetherAlpha = GRANT_TETHER_ALPHA * Math.min(layers.alphaFor(socket), layers.alphaFor(granted));
            beams.connector(socketX, socketY, socketRadius, grantedX, grantedY, grantedRadius, tetherLook, tetherAlpha, tetherAlpha, widthScale);
        }
    }

    private void drawConnector(TreeTopology.Connector<SkillNode> connector, TreeViewport viewport, NodeAllocator.Snapshot allocation,
                               TemplateHighlight templateHighlight, AllocationAnimation allocationAnimation, float alphaMult) {
        float cullMargin = 2f * NodeBeams.laneEdge(laneWidthScale(viewport.zoom()));
        if (!viewport.overlaps(viewport.screenX(connector.minX()) - cullMargin, viewport.screenY(connector.maxY()) - cullMargin,
                viewport.screenX(connector.maxX()) + cullMargin, viewport.screenY(connector.minY()) + cullMargin)) {
            return;
        }
        SkillNode node = connector.from();
        SkillNode other = connector.to();
        if (allocation.isConcealed(node) || allocation.isConcealed(other)) {
            return;
        }
        ConnectorCurve curve = connector.curve();
        float nodeX = viewport.screenX(node.getOffsetX());
        float nodeY = viewport.screenY(node.getOffsetY());
        float otherX = viewport.screenX(other.getOffsetX());
        float otherY = viewport.screenY(other.getOffsetY());

        float zoom = viewport.zoom();
        ShipSkillData skillData = allocation.skillData();
        String satisfiedRootId = allocation.satisfiedRootId();
        float nodeTowardX = curve == null ? otherX : 2f * viewport.screenX(curve.getControlOffsetX()) - (nodeX + otherX) / 2f;
        float nodeTowardY = curve == null ? otherY : 2f * viewport.screenY(curve.getControlOffsetY()) - (nodeY + otherY) / 2f;
        float otherTowardX = curve == null ? nodeX : nodeTowardX;
        float otherTowardY = curve == null ? nodeY : nodeTowardY;
        ConnectorEndpoint nodeEndpoint = new ConnectorEndpoint(nodeX, nodeY,
                endpointRadius(node, zoom, nodeTowardX - nodeX, nodeTowardY - nodeY));
        ConnectorEndpoint otherEndpoint = new ConnectorEndpoint(otherX, otherY,
                endpointRadius(other, zoom, otherTowardX - otherX, otherTowardY - otherY));
        AllocationAnimation.FillRange animatedRange = allocationAnimation.animatedRange(other.getId(), node.getId());
        boolean bothShown = allocationAnimation.shown(node.getId(), skillData.isSatisfied(node.getId(), satisfiedRootId))
                && allocationAnimation.shown(other.getId(), skillData.isSatisfied(other.getId(), satisfiedRootId));
        boolean bothSatisfied = (bothShown || animatedRange != null)
                && !templateHighlight.plannedOnlyNodeIds().contains(node.getId()) && !templateHighlight.plannedOnlyNodeIds().contains(other.getId());
        boolean nodeInTemplate = templateHighlight.templateNodeIds().contains(node.getId());
        boolean otherInTemplate = templateHighlight.templateNodeIds().contains(other.getId());

        ConnectorFade fade = new ConnectorFade(
                onConduit(node, other) ? ConnectorKind.CONDUIT : ConnectorKind.of(bothSatisfied, nodeInTemplate, otherInTemplate),
                isWormhole(other) || allocation.isHidden(other),
                isWormhole(node) || allocation.isHidden(node),
                isOpenWormhole(other, skillData, satisfiedRootId),
                isOpenWormhole(node, skillData, satisfiedRootId));
        AllocationAnimation.FillRange fillRange = bothSatisfied && animatedRange != null ? animatedRange : AllocationAnimation.FillRange.FULL;
        EdgeFill edgeFill = fillRange.isFull() ? EdgeFill.NONE : new EdgeFill(fillRange, ConnectorKind.of(false, nodeInTemplate, otherInTemplate));
        prepareBubbles(fade.glowing() && edgeFill == EdgeFill.NONE, node, other);

        ConnectorStyle connectorStyle = new ConnectorStyle(fade, edgeFill, zoom, alphaMult * nodeSearch.connectorAlpha(node, other, allocation));
        if (curve == null) {
            painter.paintStraight(otherEndpoint, nodeEndpoint, connectorStyle);
        } else {
            painter.paintCurved(otherEndpoint, viewport.screenX(curve.getControlOffsetX()), viewport.screenY(curve.getControlOffsetY()), nodeEndpoint,
                    connectorStyle);
        }
    }

    private void prepareBubbles(boolean flowing, SkillNode node, SkillNode other) {
        painter.clearBubbles();
        if (!flowing) {
            return;
        }
        float nodeDistance = flow.distance(node.getId());
        float otherDistance = flow.distance(other.getId());
        if (Float.isNaN(nodeDistance) || Float.isNaN(otherDistance)) {
            return;
        }
        float length = (float) Math.hypot(node.getOffsetX() - other.getOffsetX(), node.getOffsetY() - other.getOffsetY());
        painter.setBubbles(flow.bubbles(Math.min(nodeDistance, otherDistance), length, painter.bubbleFractions()), otherDistance <= nodeDistance);
    }

    private float endpointRadius(SkillNode node, float zoom, float towardX, float towardY) {
        if (ExoshipNodes.isExoship(node)) return exoshipRadius * zoom * EXOSHIP_ENDPOINT_SHARE;
        SkillTier tier = node.getTier();
        if (tier == SkillTier.SOCKET) {
            return socketEndpointRadius(node.getId(), NODE_SIZE * zoom * tier.getSizeMultiplier(), zoom, towardX, towardY);
        }
        return ConnectorPainter.nodeEndpointRadius(tier, zoom, wormholeOpenness.of(node.getId()));
    }

    private float socketEndpointRadius(String socketId, float footprintSize, float zoom, float towardX, float towardY) {
        float socketEdge = SkillTreeSocketRenderer.visibleEdgeDistance(footprintSize, towardX, towardY);
        float morph = socketWormholes.morph(socketId);
        if (morph <= 0f) return socketEdge;
        float wormholeFootprint = NODE_SIZE * zoom * SkillTier.WORMHOLE_EXIT.getSizeMultiplier();
        float wormholeEdge = connectorEndpointRadius(SkillTier.WORMHOLE_EXIT, wormholeFootprint, zoom) * (1f - socketWormholes.openness(socketId));
        return socketEdge + (wormholeEdge - socketEdge) * morph;
    }

    private static boolean isOpenWormhole(SkillNode node, ShipSkillData skillData, String satisfiedRootId) {
        return isWormhole(node) && skillData.isSatisfied(node.getId(), satisfiedRootId);
    }

    private static boolean isWormhole(SkillNode node) {
        return node.getType().getTier().isWormhole() && !ExoshipNodes.isExoship(node);
    }

    private record TemplateHighlight(Set<String> templateNodeIds, Set<String> plannedOnlyNodeIds) {
    }

}
