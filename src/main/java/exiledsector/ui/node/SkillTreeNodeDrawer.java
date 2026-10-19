package exiledsector.ui.node;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.i18n.Translation;
import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.TreeLayers;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.TextLabel;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static exiledsector.ui.node.SkillTreeNodeGeometry.ICON_INSET_RATIO;
import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_SIZE;

public final class SkillTreeNodeDrawer {

    private static final Color ALLOCATED_TINT = Color.WHITE;
    private static final Color UNALLOCATED_TINT = new Color(90, 90, 90);
    private static final float EXOSHIP_HIT_SHARE = 0.6f;

    private final TreeAllocationSession session;
    private final SkillTreePanelStyle panelStyle;
    private final NodeSearch nodeSearch;
    private final SkillTreeNodeRingRenderer ringRenderer;
    private final SkillTreeNodeIconRenderer iconRenderer = new SkillTreeNodeIconRenderer();
    private final SkillTreeSocketRenderer socketRenderer = new SkillTreeSocketRenderer();
    private final SkillTreeBeaconRenderer beaconRenderer = new SkillTreeBeaconRenderer();
    private final List<SkillNode> beaconsOverConnectors = new ArrayList<>();
    private final SkillTreeNodeGhostRenderer ghostRenderer = new SkillTreeNodeGhostRenderer();
    private final SkillTreeWormholeGhostFlights wormholeGhostFlights = new SkillTreeWormholeGhostFlights(new Random());
    private final SkillTreeNodeConnectorRenderer connectorRenderer;
    private final SkillTreeNodeTooltipRenderer tooltipRenderer;
    private final SkillTreeNodeDropdownRenderer dropdownRenderer;
    private final WormholeOpenness wormholeOpenness = new WormholeOpenness();
    private final ConnectorFills connectorFills = new ConnectorFills();
    private final Consumer<String> startPulse;
    private final TreeFeedback feedback = new DrawerFeedback();
    private final TextLabel startingRootPrompt = new TextLabel(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE,
            SkillTreePanelStyle.TOOLTIP_TITLE_COLOR, LazyFont.TextAnchor.BOTTOM_CENTER).set(Translation.text("ui.node.startingRootPrompt"));

    private SkillTreeNodeDrawer(TreeAllocationSession session, FleetMemberAPI member, SkillTreePanelStyle panelStyle, NodeSearch nodeSearch) {
        this.session = session;
        this.panelStyle = panelStyle;
        this.nodeSearch = nodeSearch;
        this.ringRenderer = new SkillTreeNodeRingRenderer(panelStyle, wormholeOpenness);
        this.startPulse = ringRenderer::startPulse;
        this.connectorRenderer = new SkillTreeNodeConnectorRenderer(panelStyle, nodeSearch, wormholeOpenness);
        this.tooltipRenderer = new SkillTreeNodeTooltipRenderer(member, panelStyle);
        this.dropdownRenderer = new SkillTreeNodeDropdownRenderer(panelStyle);
    }

    public static SkillTreeNodeDrawer attachedTo(TreeAllocationSession session, FleetMemberAPI member, SkillTreePanelStyle panelStyle,
                                                 NodeSearch nodeSearch) {
        SkillTreeNodeDrawer drawer = new SkillTreeNodeDrawer(session, member, panelStyle, nodeSearch);
        drawer.feedback.rootChanged(session.rootChoice().chosen());
        session.listen(drawer.feedback);
        return drawer;
    }

    private boolean startFillsInto(SkillNode node) {
        NodeAllocator.Snapshot allocation = session.snapshot();
        boolean started = false;
        for (SkillNode neighbour : SkillTree.topology().drawnNeighbours(node.getId())) {
            if (allocation.skillData().isSatisfied(neighbour.getId(), allocation.satisfiedRootId())) {
                connectorFills.start(neighbour.getId(), node.getId());
                started = true;
            }
        }
        return started;
    }

    public void advance(float amount) {
        ringRenderer.advance(amount);
        String targetedSocketId = session.targetedSocketId();
        if (targetedSocketId != null && !ringRenderer.isPulsing(targetedSocketId)) {
            ringRenderer.startPulse(targetedSocketId);
        }
        connectorFills.advance(amount, startPulse);
        ghostRenderer.advance(amount);
        socketRenderer.advance(amount);
        beaconRenderer.advance(amount);
        ShipSkillData skillData = session.snapshot().skillData();
        wormholeGhostFlights.advance(amount, skillData);
        wormholeOpenness.advance(amount, session.viewSnapshot().skillData());
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

        beaconsOverConnectors.clear();
        for (SkillNode node : SkillTree.topology().nonRoots()) {
            float nodeAlpha = shownLayers.alphaFor(node);
            if (ExoshipNodes.isBeacon(node)) {
                beaconsOverConnectors.add(node);
            } else if (nodeAlpha > 0f && !ExoshipNodes.isExoship(node)) {
                renderNode(node, shownLayers.viewportFor(node), nodeAlpha, allocation);
            }
        }

        connectorRenderer.draw(shownLayers, allocation, session.templateNodeIds(), session.plannedOnlyNodeIds(), connectorFills);
        for (SkillNode beacon : beaconsOverConnectors) {
            float beaconAlpha = shownLayers.alphaFor(beacon);
            if (beaconAlpha > 0f) {
                renderNode(beacon, shownLayers.viewportFor(beacon), beaconAlpha, allocation);
            }
        }
        wormholeGhostFlights.draw(viewport, shownLayers.treeAlpha() * nodeSearch.backgroundAlpha());

        boolean choosing = rootChoice.phase() == StartingRootChoice.Phase.CHOOSING;
        for (SkillNode node : SkillTree.topology().roots()) {
            renderRootNode(node, viewport, alphaMult, allocation, rootChoice, choosing);
        }

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

        boolean allocated = skillData.isAllocated(node.getId());
        boolean live = allocated && !session.isPlannedOnly(node);
        boolean breathing = !allocated && allocation.canAllocate(node);
        SkillType effectiveType = node.resolveEffectiveType(skillData);
        float iconSize = footprintSize * ICON_INSET_RATIO;

        float nodeAlpha = alphaMult * nodeSearch.nodeAlpha(node, allocation);
        if (ExoshipNodes.isBeacon(node)) {
            beaconRenderer.draw(nodeX, nodeY, footprintSize, live, breathing, nodeAlpha);
            return;
        }
        if (tier == SkillTier.SOCKET) {
            socketRenderer.drawFrame(nodeX, nodeY, footprintSize, live, panelStyle.getAccentColor(), nodeAlpha);
            socketRenderer.drawContent(nodeX, nodeY, footprintSize, socketedIcon(skillData, node), iconTint(node, allocation, allocated),
                    nodeAlpha);
        }
        ringRenderer.draw(nodeX, nodeY, footprintSize, nodeAlpha, SkillTreeNodeRingRenderer.RingState.of(live, breathing), zoom, node);

        if (tier == SkillTier.SOCKET) {
            if (live) {
                socketRenderer.drawArcs(node.getId(), nodeX, nodeY, footprintSize, panelStyle.getAccentColor(), nodeAlpha);
            }
        } else if (tier != SkillTier.WORMHOLE) {
            Color tint = iconTint(node, allocation, allocated);
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
        if (!viewport.isVisible(nodeX, nodeY, ringRenderer.reach(footprintSize, node))) {
            return;
        }
        boolean allocated = allocation.skillData().isAllocated(node.getId());
        boolean breathing = choosing || (!allocated && allocation.canAllocate(node));
        float nodeAlpha = alphaMult * nodeSearch.nodeAlpha(node, allocation);
        ringRenderer.draw(nodeX, nodeY, footprintSize, nodeAlpha, SkillTreeNodeRingRenderer.RingState.of(allocated, breathing), zoom, node);

        Color tint = choosing ? ALLOCATED_TINT : iconTint(node, allocation, allocated);
        iconRenderer.drawIcon(node.getType().getIconPath(), nodeX, nodeY, footprintSize, nodeAlpha, tint);
    }

    private Color iconTint(SkillNode node, NodeAllocator.Snapshot allocation, boolean allocated) {
        return allocated || nodeSearch.matches(node, allocation) ? ALLOCATED_TINT : UNALLOCATED_TINT;
    }

    private static String socketedIcon(ShipSkillData skillData, SkillNode node) {
        Item socketed = ItemStore.lookup(skillData.getSocketedItem(node.getId()));
        return socketed == null ? null : socketed.iconPath();
    }

    public void renderHoverTooltip(TreeLayers layers, float mouseX, float mouseY, float alphaMult) {
        SkillNode openDropdownNode = session.dropdownNode();
        if (openDropdownNode != null) {
            SkillType hoveredOption = dropdownRenderer.findOptionAt(openDropdownNode, layers.viewportFor(openDropdownNode), mouseX, mouseY);
            if (hoveredOption != null) {
                tooltipRenderer.renderTooltipForType(hoveredOption, session.dropdownOptionRefusalReason(hoveredOption), mouseX, mouseY,
                        alphaMult);
            }
            return;
        }

        SkillNode hoveredNode = findNodeAt(layers, mouseX, mouseY);
        if (hoveredNode != null) {
            tooltipRenderer.renderTooltip(hoveredNode, session.viewSnapshot(), mouseX, mouseY, alphaMult);
        }
    }

    public SkillNode findNodeAt(TreeLayers layers, float screenX, float screenY) {
        StartingRootChoice rootChoice = session.rootChoice();
        if (rootChoice.isMoving()) {
            return null;
        }
        boolean choosing = rootChoice.phase() == StartingRootChoice.Phase.CHOOSING;
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if ((choosing && node.getType().getTier() != SkillTier.ROOT) || !layers.isInteractive(node)) {
                continue;
            }
            TreeViewport viewport = layers.viewportFor(node);
            float nodeX = viewport.screenX(rootChoice.offsetX(node));
            float nodeY = viewport.screenY(rootChoice.offsetY(node));
            float halfSize = ExoshipNodes.isExoship(node) ? layers.exoshipRadius() * viewport.zoom() * EXOSHIP_HIT_SHARE
                    : NODE_SIZE * viewport.zoom() * node.getType().getTier().getSizeMultiplier() / 2f;
            if (Math.abs(screenX - nodeX) <= halfSize && Math.abs(screenY - nodeY) <= halfSize) {
                return node;
            }
        }
        return null;
    }

    public SkillType findDropdownOptionAt(TreeLayers layers, float screenX, float screenY) {
        SkillNode openDropdownNode = session.dropdownNode();
        return openDropdownNode == null ? null
                : dropdownRenderer.findOptionAt(openDropdownNode, layers.viewportFor(openDropdownNode), screenX, screenY);
    }

    private final class DrawerFeedback implements TreeFeedback {

        @Override
        public void allocated(SkillNode node) {
            if (startFillsInto(node)) {
                connectorFills.schedulePulse(node.getId());
            } else {
                ringRenderer.startPulse(node.getId());
            }
        }

        @Override
        public void deallocated(SkillNode node) {
            connectorFills.cancel(node.getId());
            String partnerId = session.wormholePartner(node);
            if (partnerId != null) {
                connectorFills.cancel(partnerId);
            }
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
}
