package exiledsector.ui.node;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import exiledsector.items.DoctrineOverrides;
import exiledsector.items.DoctrineRewrite;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TreeLayers;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static exiledsector.ui.node.NodeMetrics.ICON_INSET_RATIO;
import static exiledsector.ui.node.NodeMetrics.NODE_SIZE;
import static exiledsector.ui.node.NodeMetrics.donutRadius;

public final class SkillTreeNodeDrawer {

    private static final Color ALLOCATED_TINT = Color.WHITE;
    private static final Color UNALLOCATED_TINT = new Color(90, 90, 90);
    private static final Color TRANSFORMED_TINT = new Color(255, 196, 120);
    private static final Color AUGMENTED_TINT = new Color(150, 220, 255);
    private static final float UNALLOCATED_TINT_SCALE = UNALLOCATED_TINT.getRed() / 255f;
    private static final Color TRANSFORMED_UNALLOCATED_TINT = scaled(TRANSFORMED_TINT, UNALLOCATED_TINT_SCALE);
    private static final Color AUGMENTED_UNALLOCATED_TINT = scaled(AUGMENTED_TINT, UNALLOCATED_TINT_SCALE);
    private static final float EXOSHIP_HIT_SHARE = 0.6f;
    private static final float SOCKET_COLLAPSE_SHARE = 0.4f;
    private static final float ROOT_ICON_SHARE = 0.68f;
    private static final float ROOT_ICON_ALPHA = 0.3f;
    private static final float SOCKET_WORMHOLE_START_SCALE = 0.3f;

    private final TreeAllocationSession session;
    private final SkillTreePanelStyle panelStyle;
    private final NodeSearch nodeSearch;
    private final SkillTreeNodeRingRenderer ringRenderer;
    private final NodeBeams beams;
    private final SkillTreeNodeIconRenderer iconRenderer = new SkillTreeNodeIconRenderer();
    private final RootCircuitRenderer rootCircuitRenderer = new RootCircuitRenderer();
    private final RootCircuitGrowth rootCircuitGrowth = new RootCircuitGrowth();
    private final RootAffinityNumber rootAffinityNumber;
    private final SkillTreeSocketRenderer socketRenderer = new SkillTreeSocketRenderer();
    private SkillTreeTopology socketTopology;
    private List<SkillNode> socketNodesCache = List.of();
    private final SkillTreeNodeGhostRenderer ghostRenderer = new SkillTreeNodeGhostRenderer();
    private final SkillTreeWormholeGhostFlights wormholeGhostFlights = new SkillTreeWormholeGhostFlights(new Random());
    private final SkillTreeNodeConnectorRenderer connectorRenderer;
    private final SkillTreeNodeTooltipRenderer tooltipRenderer;
    private final SkillTreeNodeDropdownRenderer dropdownRenderer;
    private final WormholeOpenness wormholeOpenness = new WormholeOpenness();
    private final SocketWormholes socketWormholes = new SocketWormholes();
    private final AllocationAnimation allocationAnimation;
    private final GrantMarkers grantMarkers = GrantMarkers.fromItemStore();
    private final TreeFeedback feedback = new DrawerFeedback();
    private final TextLabel startingRootPrompt = new TextLabel(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE,
            SkillTreePanelStyle.TOOLTIP_TITLE_COLOR, LazyFont.TextAnchor.BOTTOM_CENTER).set(Translation.text("ui.node.startingRootPrompt"));

    private SkillTreeNodeDrawer(TreeAllocationSession session, FleetMemberAPI member, SkillTreePanelStyle panelStyle, NodeSearch nodeSearch) {
        this.session = session;
        this.panelStyle = panelStyle;
        this.nodeSearch = nodeSearch;
        this.ringRenderer = new SkillTreeNodeRingRenderer(panelStyle, wormholeOpenness);
        this.beams = ringRenderer.beams();
        this.allocationAnimation = new AllocationAnimation(new AnimatedTree(), new AnimationSounds());
        this.connectorRenderer = new SkillTreeNodeConnectorRenderer(panelStyle, nodeSearch, wormholeOpenness, socketWormholes, beams);
        this.tooltipRenderer = new SkillTreeNodeTooltipRenderer(member, panelStyle);
        this.rootAffinityNumber = new RootAffinityNumber(member.getHullSpec().getHullSize());
        this.dropdownRenderer = new SkillTreeNodeDropdownRenderer(panelStyle);
    }

    public static SkillTreeNodeDrawer attachedTo(TreeAllocationSession session, FleetMemberAPI member, SkillTreePanelStyle panelStyle,
                                                 NodeSearch nodeSearch) {
        SkillTreeNodeDrawer drawer = new SkillTreeNodeDrawer(session, member, panelStyle, nodeSearch);
        drawer.feedback.rootChanged(session.rootChoice().chosen());
        session.listen(drawer.feedback);
        return drawer;
    }

    public void advance(float amount) {
        ringRenderer.advance(amount);
        rootCircuitRenderer.advance(amount);
        connectorRenderer.advance(amount);
        rootCircuitGrowth.advance(amount, session.rootChoice());
        String targetedSocketId = session.targetedSocketId();
        if (targetedSocketId != null && !ringRenderer.isPulsing(targetedSocketId)) {
            ringRenderer.startPulse(targetedSocketId);
        }
        allocationAnimation.advance(amount);
        ghostRenderer.advance(amount);
        socketRenderer.advance(amount);
        ShipSkillData drawnData = session.viewSnapshot().skillData();
        wormholeGhostFlights.advance(amount, drawnData);
        wormholeOpenness.advance(amount, drawnData);
        socketWormholes.advance(amount, drawnData);
    }

    public void launchWormholeGhosts(SkillNode from, SkillNode to) {
        wormholeGhostFlights.launchFrom(from, to);
    }

    public void render(TreeLayers layers, float mouseX, float mouseY, boolean hovered) {
        NodeAllocator.Snapshot allocation = session.viewSnapshot();
        StartingRootChoice rootChoice = session.rootChoice();
        TreeViewport viewport = layers.treeViewport();
        float alphaMult = layers.treeAlpha();
        TreeLayers shownLayers = layers.withAlpha(rootChoice.treeAlpha());
        grantMarkers.refresh(allocation);
        String tetheredSocketId = hovered ? hoveredGrantSocketId(layers, rootChoice, mouseX, mouseY) : null;
        if (tetheredSocketId != null) {
            connectorRenderer.drawGrantTethers(shownLayers, SkillTree.get(tetheredSocketId), grantMarkers.grantedBy(tetheredSocketId),
                    grantMarkers.colorForSocket(tetheredSocketId));
            beams.flush();
        }

        for (SkillNode node : SkillTree.topology().nonRoots()) {
            float nodeAlpha = shownLayers.alphaFor(node);
            if (nodeAlpha > 0f && !ExoshipNodes.isExoship(node)) {
                renderNode(node, shownLayers.viewportFor(node), nodeAlpha, allocation);
            }
        }

        connectorRenderer.draw(shownLayers, allocation, session.templateNodeIds(), session.plannedOnlyNodeIds(), allocationAnimation);
        beams.flush();
        wormholeGhostFlights.draw(viewport, shownLayers.treeAlpha() * nodeSearch.backgroundAlpha());

        boolean choosing = rootChoice.phase() == StartingRootChoice.Phase.CHOOSING;
        for (SkillNode node : SkillTree.topology().roots()) {
            renderRootNode(node, viewport, alphaMult, allocation, rootChoice, choosing);
        }
        beams.flush();

        if (choosing) {
            startingRootPrompt.setAlpha(alphaMult).draw(viewport.centerX(), viewport.screenY(rootChoice.promptOffsetY()));
        }

        SkillNode dropdownNode = session.dropdownNode();
        if (dropdownNode != null) {
            dropdownRenderer.render(dropdownNode, layers.viewportFor(dropdownNode), mouseX, mouseY, hovered, layers.alphaFor(dropdownNode),
                    session.dropdownOptionUsable());
        }
    }

    private void renderNode(SkillNode node, TreeViewport viewport, float alphaMult, NodeAllocator.Snapshot allocation) {
        ShipSkillData skillData = allocation.skillData();
        SkillTier tier = node.getType().getTier();
        float zoom = viewport.zoom();
        float nodeX = viewport.screenX(node.getOffsetX());
        float nodeY = viewport.screenY(node.getOffsetY());
        float footprintSize = NODE_SIZE * zoom * tier.getSizeMultiplier();
        if (!viewport.isVisible(nodeX, nodeY, ringRenderer.reach(footprintSize, node))) {
            return;
        }

        if (allocation.isHidden(node)) {
            ghostRenderer.draw(nodeX, nodeY, footprintSize, alphaMult * nodeSearch.backgroundAlpha(), node.getId());
            return;
        }

        boolean allocated = allocationAnimation.shown(node.getId(), skillData.isAllocated(node.getId()));
        boolean live = allocated && !session.isPlannedOnly(node);
        boolean breathing = !allocated && allocation.canAllocate(node);
        SkillType effectiveType = node.resolveEffectiveType(skillData);
        float iconSize = footprintSize * ICON_INSET_RATIO;

        float nodeAlpha = alphaMult * nodeSearch.nodeAlpha(node, allocation);
        float wormholeMorph = tier == SkillTier.SOCKET ? socketWormholes.morph(node.getId()) : 0f;
        float socketAlpha = nodeAlpha * (1f - wormholeMorph);
        float socketSize = footprintSize * (1f - SOCKET_COLLAPSE_SHARE * wormholeMorph);
        if (wormholeMorph > 0f) {
            float wormholeSize = NODE_SIZE * zoom * SkillTier.WORMHOLE_EXIT.getSizeMultiplier()
                    * (SOCKET_WORMHOLE_START_SCALE + (1f - SOCKET_WORMHOLE_START_SCALE) * wormholeMorph);
            ringRenderer.drawSocketWormhole(nodeX, nodeY, wormholeSize, nodeAlpha * wormholeMorph, node.getId(),
                    socketWormholes.exit(node.getId()), socketWormholes.openness(node.getId()));
        }
        DoctrineRewrite rewrite = tier == SkillTier.SOCKET || tier.isWormhole() ? null : DoctrineOverrides.rewrite(skillData, node.getId());
        if (tier == SkillTier.SOCKET && socketAlpha > 0f) {
            Item socketed = ItemStore.lookup(skillData.getSocketedItem(node.getId()));
            Color socketDoctrineColor = socketed == null ? null : DoctrineColors.forDoctrine(socketed.definitionId());
            if (socketDoctrineColor != null) {
                ringRenderer.drawDoctrineSocketBelt(nodeX, nodeY, socketSize, socketDoctrineColor, live, socketAlpha, zoom);
            }
            socketRenderer.drawFrame(nodeX, nodeY, socketSize, live, panelStyle.getAccentColor(), socketAlpha);
            socketRenderer.drawContent(nodeX, nodeY, socketSize, socketed == null ? null : socketed.iconPath(),
                    iconTint(node, allocation, allocated), socketAlpha);
        }
        Color notableDoctrineColor = rewrite != null && tier == SkillTier.NOTABLE ? DoctrineColors.forDoctrine(rewrite.doctrineDefinitionId()) : null;
        Color grantColor = allocated ? grantMarkers.colorFor(node.getId()) : null;
        ringRenderer.draw(nodeX, nodeY, footprintSize, nodeAlpha, SkillTreeNodeRingRenderer.RingState.of(live, breathing), zoom, node,
                notableDoctrineColor, grantColor);

        if (tier == SkillTier.SOCKET) {
            if (live && socketAlpha > 0f) {
                socketRenderer.drawArcs(node.getId(), nodeX, nodeY, socketSize, panelStyle.getAccentColor(), socketAlpha);
            }
        } else if (!tier.isWormhole()) {
            Color tint = doctrineTint(rewrite, iconTint(node, allocation, allocated));
            if (effectiveType.isOptional()) {
                iconRenderer.drawSplitIcon(effectiveType, nodeX, nodeY, iconSize, nodeAlpha, tint);
            } else {
                iconRenderer.drawIcon(effectiveType.getIconPath(), nodeX, nodeY, iconSize, nodeAlpha, tint);
            }
        }
    }

    private void renderRootNode(SkillNode node, TreeViewport viewport, float alphaMult, NodeAllocator.Snapshot allocation,
                                StartingRootChoice rootChoice, boolean choosing) {
        float zoom = viewport.zoom();
        float nodeX = viewport.screenX(rootChoice.offsetX(node));
        float nodeY = viewport.screenY(rootChoice.offsetY(node));
        float footprintSize = NODE_SIZE * zoom * SkillTier.ROOT.getSizeMultiplier();
        float reach = Math.max(ringRenderer.reach(footprintSize, node), rootCircuitRenderer.reachRadius(donutRadius(footprintSize)));
        if (!viewport.isVisible(nodeX, nodeY, reach)) {
            return;
        }
        boolean allocated = allocationAnimation.shown(node.getId(), allocation.skillData().isAllocated(node.getId()));
        boolean breathing = choosing || (!allocated && allocation.canAllocate(node));
        float nodeAlpha = alphaMult * nodeSearch.nodeAlpha(node, allocation);
        float rootRadius = donutRadius(footprintSize);
        Color rootColour = RootColours.of(node.getType().getId());
        rootCircuitRenderer.draw(nodeX, nodeY, rootRadius, nodeAlpha, rootColour, rootCircuitGrowth.grownRadii(node, rootChoice));
        ringRenderer.draw(nodeX, nodeY, footprintSize, nodeAlpha, SkillTreeNodeRingRenderer.RingState.of(allocated, breathing), zoom, node);

        Color tint = choosing ? ALLOCATED_TINT : iconTint(node, allocation, allocated);
        iconRenderer.drawIcon(node.getType().getIconPath(), nodeX, nodeY, footprintSize * ROOT_ICON_SHARE, nodeAlpha * ROOT_ICON_ALPHA, tint);
        rootAffinityNumber.draw(node, allocation.skillData(), rootChoice.phase() != StartingRootChoice.Phase.CHOSEN,nodeX, nodeY, rootRadius, nodeAlpha);
    }

    private Color iconTint(SkillNode node, NodeAllocator.Snapshot allocation, boolean allocated) {
        return allocated || nodeSearch.matches(node, allocation) ? ALLOCATED_TINT : UNALLOCATED_TINT;
    }

    private static Color doctrineTint(DoctrineRewrite rewrite, Color tint) {
        if (rewrite == null) {
            return tint;
        }
        boolean lit = tint == ALLOCATED_TINT;
        if (rewrite.transformed()) {
            return lit ? TRANSFORMED_TINT : TRANSFORMED_UNALLOCATED_TINT;
        }
        return lit ? AUGMENTED_TINT : AUGMENTED_UNALLOCATED_TINT;
    }

    private static Color scaled(Color color, float scale) {
        return new Color(Math.round(color.getRed() * scale), Math.round(color.getGreen() * scale), Math.round(color.getBlue() * scale));
    }

    public void endHover() {
        tooltipRenderer.endHover();
    }

    public void renderHoverTooltip(TreeLayers layers, float mouseX, float mouseY, float alphaMult) {
        SkillNode openDropdownNode = session.dropdownNode();
        if (openDropdownNode != null) {
            SkillType hoveredOption = dropdownRenderer.findOptionAt(openDropdownNode, layers.viewportFor(openDropdownNode), mouseX, mouseY);
            if (hoveredOption != null) {
                tooltipRenderer.renderTooltipForType(openDropdownNode, hoveredOption, session.viewSnapshot(),
                        session.dropdownOptionRefusalReason(hoveredOption), mouseX, mouseY, alphaMult);
            } else {
                tooltipRenderer.endHover();
            }
            return;
        }

        SkillNode hoveredNode = findNodeAt(layers, mouseX, mouseY);
        if (hoveredNode != null) {
            String tooltipWarning = session.releaseWarning(hoveredNode);
            if (tooltipWarning == null && hoveredNode.getType().getTier() == SkillTier.SOCKET) {
                tooltipWarning = session.doctrineRemovalWarning(hoveredNode);
            }
            tooltipRenderer.renderTooltip(hoveredNode, session.viewSnapshot(), tooltipWarning, mouseX, mouseY, alphaMult);
        } else {
            tooltipRenderer.endHover();
        }
    }

    public SkillNode findNodeAt(TreeLayers layers, float screenX, float screenY) {
        StartingRootChoice rootChoice = session.rootChoice();
        if (rootChoice.isMoving()) {
            return null;
        }
        boolean choosing = rootChoice.phase() == StartingRootChoice.Phase.CHOOSING;
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if ((!choosing || node.getType().getTier() == SkillTier.ROOT) && isUnder(layers, rootChoice, node, screenX, screenY)) {
                return node;
            }
        }
        return null;
    }

    private String hoveredGrantSocketId(TreeLayers layers, StartingRootChoice rootChoice, float screenX, float screenY) {
        if (grantMarkers.isEmpty() || rootChoice.isMoving() || session.dropdownNode() != null) {
            return null;
        }
        List<String> grantingSocketIds = grantMarkers.grantingSocketIds();
        for (int socketIndex = 0; socketIndex < grantingSocketIds.size(); socketIndex++) {
            String socketId = grantingSocketIds.get(socketIndex);
            if (isUnder(layers, rootChoice, SkillTree.get(socketId), screenX, screenY)) {
                return socketId;
            }
            List<String> grantedNodeIds = grantMarkers.grantedBy(socketId);
            for (int grantedIndex = 0; grantedIndex < grantedNodeIds.size(); grantedIndex++) {
                if (isUnder(layers, rootChoice, SkillTree.get(grantedNodeIds.get(grantedIndex)), screenX, screenY)) {
                    return socketId;
                }
            }
        }
        return null;
    }

    private static boolean isUnder(TreeLayers layers, StartingRootChoice rootChoice, SkillNode node, float screenX, float screenY) {
        if (node == null || !layers.isInteractive(node)) {
            return false;
        }
        TreeViewport viewport = layers.viewportFor(node);
        float nodeX = viewport.screenX(rootChoice.offsetX(node));
        float nodeY = viewport.screenY(rootChoice.offsetY(node));
        float halfSize = ExoshipNodes.isExoship(node) ? layers.exoshipRadius() * viewport.zoom() * EXOSHIP_HIT_SHARE
                : NODE_SIZE * viewport.zoom() * node.getType().getTier().getSizeMultiplier() / 2f;
        return Math.abs(screenX - nodeX) <= halfSize && Math.abs(screenY - nodeY) <= halfSize;
    }

    public SkillNode findSocketAt(TreeLayers layers, float screenX, float screenY) {
        if (session.rootChoice().isMoving()) {
            return null;
        }
        for (SkillNode socket : socketNodes()) {
            if (!layers.isInteractive(socket)) {
                continue;
            }
            TreeViewport viewport = layers.viewportFor(socket);
            float halfSize = NODE_SIZE * viewport.zoom() * SkillTier.SOCKET.getSizeMultiplier() / 2f;
            if (Math.abs(screenX - viewport.screenX(socket.getOffsetX())) <= halfSize
                    && Math.abs(screenY - viewport.screenY(socket.getOffsetY())) <= halfSize) {
                return socket;
            }
        }
        return null;
    }

    private List<SkillNode> socketNodes() {
        SkillTreeTopology topology = SkillTree.topology();
        if (socketTopology != topology) {
            socketTopology = topology;
            socketNodesCache = SkillTree.getAllNodes().values().stream().filter(node -> node.getType().getTier() == SkillTier.SOCKET).toList();
        }
        return socketNodesCache;
    }

    public SkillType findDropdownOptionAt(TreeLayers layers, float screenX, float screenY) {
        SkillNode openDropdownNode = session.dropdownNode();
        return openDropdownNode == null ? null
                : dropdownRenderer.findOptionAt(openDropdownNode, layers.viewportFor(openDropdownNode), screenX, screenY);
    }

    private final class DrawerFeedback implements TreeFeedback {

        @Override
        public void allocated(String originId, Collection<String> nodeIds) {
            allocationAnimation.allocated(nodeIds, originId);
        }

        @Override
        public void deallocated(String originId, Collection<String> nodeIds) {
            allocationAnimation.deallocated(nodeIds, originId);
        }

        @Override
        public void pulse(SkillNode node) {
            ringRenderer.startPulse(node.getId());
        }

        @Override
        public void rootChanged(SkillNode chosenRoot) {
            panelStyle.setAccentIconPath(chosenRoot != null ? chosenRoot.getType().getIconPath() : null);
        }
    }

    private final class AnimatedTree implements AllocationAnimation.Tree {

        @Override
        public List<String> neighbours(String nodeId) {
            List<SkillNode> drawn = SkillTree.topology().drawnNeighbours(nodeId);
            SkillNode node = SkillTree.get(nodeId);
            String partnerId = node == null ? null : session.wormholePartner(node);
            List<String> neighbourIds = new ArrayList<>(drawn.size() + 1);
            for (SkillNode neighbour : drawn) {
                neighbourIds.add(neighbour.getId());
            }
            if (partnerId != null) {
                neighbourIds.add(partnerId);
            }
            return neighbourIds;
        }

        @Override
        public boolean isSatisfied(String nodeId) {
            NodeAllocator.Snapshot allocation = session.snapshot();
            return allocation.skillData().isSatisfied(nodeId, allocation.satisfiedRootId());
        }
    }

    private final class AnimationSounds implements AllocationAnimation.Listener {

        @Override
        public void started(String nodeId, boolean allocating) {
            SkillNode node = SkillTree.get(nodeId);
            if (!allocating) {
                SkillTreeSounds.deallocated();
            } else if (node != null) {
                SkillTreeSounds.allocated(TreeAllocationSession.allocationSound(node.getType().getTier()));
            }
        }

        @Override
        public void lit(String nodeId) {
            ringRenderer.startPulse(nodeId);
        }
    }
}
