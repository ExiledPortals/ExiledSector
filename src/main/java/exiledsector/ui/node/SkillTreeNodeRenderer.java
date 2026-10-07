package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.template.AutoAllocateRun;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.StepVerdict;
import exiledsector.skills.template.TemplateCapture;
import exiledsector.skills.template.TemplateStep;
import exiledsector.socketables.SocketCustody;
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.SkillTreePanelStyle;
import exiledsector.ui.SkillTreeSounds;
import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.ReusableText;
import lunalib.lunaRefit.BaseRefitButton;
import org.lazywizard.lazylib.ui.LazyFont;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import static exiledsector.ui.node.SkillTreeNodeGeometry.ICON_INSET_RATIO;
import static exiledsector.ui.node.SkillTreeNodeGeometry.NODE_SIZE;

public final class SkillTreeNodeRenderer {

    private static final Color ALLOCATED_TINT = Color.WHITE;
    private static final Color UNALLOCATED_TINT = new Color(90, 90, 90);

    private final BaseRefitButton refitButton;
    private final SkillTreePanelStyle panelStyle;
    private StartingRootChoice rootChoice;
    private final NodeAllocator allocator;

    private final SkillTreeNodeRingRenderer ringRenderer;
    private final SkillTreeNodeIconRenderer iconRenderer;
    private final SkillTreeSocketRenderer socketRenderer = new SkillTreeSocketRenderer();
    private final SkillTreeNodeGhostRenderer ghostRenderer;
    private final SkillTreeWormholeGhostFlights wormholeGhostFlights;
    private final SkillTreeNodeConnectorRenderer connectorRenderer;
    private final SkillTreeNodeTooltipRenderer tooltipRenderer;
    private final SkillTreeNodeDropdownRenderer dropdownRenderer;
    private final NodeSearch nodeSearch;
    private final WormholeOpenness wormholeOpenness = new WormholeOpenness();
    private final ConnectorFills connectorFills = new ConnectorFills();
    private final Consumer<String> startPulse;

    private final TemplateStepExecutor stepExecutor;
    private final Function<TemplateStep, StepVerdict> attemptStep;
    private final BooleanSupplier pointsLeftCheck;

    private SkillType lastChosenOptionalOption;
    private String targetedSocketId;
    private final ReusableText startingRootPrompt = new ReusableText(SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE,
            SkillTreePanelStyle.TOOLTIP_TITLE_COLOR, LazyFont.TextAnchor.BOTTOM_CENTER).set(Translation.text("ui.node.startingRootPrompt"));
    private NodeAllocator.Snapshot allocationSnapshot;
    private SkillTreeTemplate template;
    private Set<String> templateNodeIds = Set.of();
    private AutoAllocateRun autoAllocateRun;
    private AutoAllocateRun.Summary lastRunSummary;
    private RespecRun respecRun;

    public SkillTreeNodeRenderer(FleetMemberAPI member, ShipVariantAPI variant, SkillTreePanelStyle panelStyle, BaseRefitButton refitButton,
                                 NodeSearch nodeSearch) {
        this.refitButton = refitButton;
        this.nodeSearch = nodeSearch;
        this.panelStyle = panelStyle;
        this.rootChoice = initialRootChoice(ShipSkillDataManager.get(member.getId()));
        this.allocator = new NodeAllocator(member, variant, () -> rootChoice.rootForAllocation());
        SkillNode chosenRoot = rootChoice.chosen();
        panelStyle.setAccentIconPath(chosenRoot != null ? chosenRoot.getType().getIconPath() : null);

        this.ringRenderer = new SkillTreeNodeRingRenderer(panelStyle, wormholeOpenness);
        this.startPulse = ringRenderer::startPulse;
        this.iconRenderer = new SkillTreeNodeIconRenderer();
        this.ghostRenderer = new SkillTreeNodeGhostRenderer();
        this.wormholeGhostFlights = new SkillTreeWormholeGhostFlights(new Random());
        this.connectorRenderer = new SkillTreeNodeConnectorRenderer(panelStyle, nodeSearch, wormholeOpenness);
        this.tooltipRenderer = new SkillTreeNodeTooltipRenderer(member, panelStyle);
        this.dropdownRenderer = new SkillTreeNodeDropdownRenderer(panelStyle);
        this.stepExecutor = new TemplateStepExecutor(allocator, this::snapshot, node -> afterAllocationChange(node, true));
        this.attemptStep = stepExecutor::attempt;
        this.pointsLeftCheck = stepExecutor::hasPointsLeft;
    }

    private static StartingRootChoice initialRootChoice(ShipSkillData skillData) {
        String startingRootId = skillData.resolveStartingRootId();
        SkillNode startingRoot = startingRootId == null ? null : SkillTree.get(startingRootId);
        if (startingRoot != null) {
            return StartingRootChoice.alreadyChosen(startingRoot);
        }
        return StartingRootChoice.pending(SkillTree.topology().roots());
    }

    private void unchooseStartingRoot(SkillNode root) {
        if (!allocator.unchooseStartingRoot(root)) {
            return;
        }
        dropdownRenderer.close();
        setTemplate(null);
        nodeSearch.setQuery("");
        rootChoice = StartingRootChoice.returning(SkillTree.topology().roots(), root);
        panelStyle.setAccentIconPath(null);
        afterAllocationChange(root, false);
    }

    public SkillNode getStartingRoot() {
        return rootChoice.phase() == StartingRootChoice.Phase.CHOSEN ? rootChoice.chosen() : null;
    }

    public boolean isChoosingStartingRoot() {
        return rootChoice.phase() == StartingRootChoice.Phase.CHOOSING;
    }

    public boolean isStartingRootMoving() {
        return rootChoice.isMoving();
    }

    public boolean isStartingRootInputLocked() {
        return rootChoice.isInputLocked();
    }

    public float treeAlpha() {
        return rootChoice.treeAlpha();
    }

    public boolean isStartingRootFlyingOut() {
        return rootChoice.phase() == StartingRootChoice.Phase.FLYING;
    }

    public float startingRootCameraProgress() {
        return rootChoice.cameraProgress();
    }

    public float startingRootCameraTargetX() {
        return rootChoice.cameraTargetX();
    }

    public float startingRootCameraTargetY() {
        return rootChoice.cameraTargetY();
    }

    public void chooseStartingRoot(SkillNode root) {
        if (!isChoosingStartingRoot() || !allocator.chooseStartingRoot(root)) {
            return;
        }
        rootChoice.choose(root);
        panelStyle.setAccentIconPath(root.getType().getIconPath());
        afterAllocationChange(root, true);
    }

    public void advance(float amount) {
        rootChoice.advance(amount);
        ringRenderer.advance(amount);
        if (targetedSocketId != null && !ringRenderer.isPulsing(targetedSocketId)) {
            ringRenderer.startPulse(targetedSocketId);
        }
        connectorFills.advance(amount, startPulse);
        ghostRenderer.advance(amount);
        socketRenderer.advance(amount);
        ShipSkillData skillData = snapshot().skillData();
        wormholeGhostFlights.advance(amount, skillData);
        wormholeOpenness.advance(amount, skillData);
        advanceAutoAllocate(amount);
        advanceRespec(amount);
    }

    private void advanceAutoAllocate(float amount) {
        if (autoAllocateRun == null || rootChoice.isInputLocked()) {
            return;
        }
        autoAllocateRun.advance(amount, attemptStep, pointsLeftCheck);
        if (autoAllocateRun.isFinished()) {
            lastRunSummary = autoAllocateRun.summary();
            autoAllocateRun = null;
        }
    }

    private void advanceRespec(float amount) {
        if (respecRun == null || rootChoice.isInputLocked()) {
            return;
        }
        respecRun.advance(amount, this::removeForRespec);
        if (respecRun.isFinished()) {
            respecRun = null;
        }
    }

    public boolean startRespec(SkillNode node) {
        if (isStartingRootInputLocked() || autoAllocateRun != null || respecRun != null) {
            return false;
        }
        List<SkillNode> respecPlan = allocator.respecPlan(node);
        if (respecPlan.isEmpty() || respecPlan.stream().anyMatch(allocator::hasDeallocationCondition)) {
            return false;
        }
        dropdownRenderer.close();
        respecRun = new RespecRun(respecPlan);
        return true;
    }

    public boolean isAllocatedSocket(SkillNode node) {
        return node != null && node.getType().getTier() == SkillTier.SOCKET && isAllocated(node);
    }

    public void setTargetedSocket(SkillNode socket) {
        targetedSocketId = socket == null ? null : socket.getId();
        if (targetedSocketId != null) {
            ringRenderer.startPulse(targetedSocketId);
        }
    }

    public boolean isAllocated(SkillNode node) {
        return allocator.data().isAllocated(node.getId());
    }

    public boolean isRespeccing() {
        return respecRun != null;
    }

    public void cancelRespec() {
        if (respecRun != null) {
            respecRun.cancel();
            respecRun = null;
        }
    }

    private boolean removeForRespec(SkillNode node) {
        if (allocator.canUnchooseStartingRoot(node)) {
            unchooseStartingRoot(node);
            return true;
        }
        if (!allocator.data().isAllocated(node.getId())) {
            return true;
        }
        if (!allocator.canDeallocate(node) || !allocator.toggle(node)) {
            return false;
        }
        afterAllocationChange(node, false);
        return true;
    }

    public void setTemplate(SkillTreeTemplate template) {
        cancelAutoAllocate();
        this.template = template;
        this.templateNodeIds = template == null ? Set.of() : template.nodeIds();
    }

    public SkillTreeTemplate template() {
        return template;
    }

    public boolean startAutoAllocate() {
        if (template == null || autoAllocateRun != null || respecRun != null || rootChoice.isInputLocked()) {
            return false;
        }
        dropdownRenderer.close();
        ShipSkillData skillData = snapshot().skillData();
        int pendingStepCount = 0;
        for (TemplateStep step : template.steps()) {
            if (SkillTree.get(step.nodeId()) != null && !skillData.isAllocated(step.nodeId())) {
                pendingStepCount++;
            }
        }
        autoAllocateRun = new AutoAllocateRun(template.steps(), pendingStepCount);
        lastRunSummary = null;
        return true;
    }

    public boolean isAutoAllocating() {
        return autoAllocateRun != null;
    }

    public void cancelAutoAllocate() {
        if (autoAllocateRun != null) {
            autoAllocateRun.cancel();
            lastRunSummary = autoAllocateRun.summary();
            autoAllocateRun = null;
        }
    }

    public AutoAllocateRun.Summary takeLastRunSummary() {
        AutoAllocateRun.Summary summary = lastRunSummary;
        lastRunSummary = null;
        return summary;
    }

    public boolean hasPointsLeft() {
        return stepExecutor.hasPointsLeft();
    }

    public int allocatedNodeCount() {
        return snapshot().skillData().getAllocatedNodeIds().size();
    }

    public List<TemplateStep> captureTemplateSteps() {
        SkillNode root = getStartingRoot();
        return TemplateCapture.capture(snapshot().skillData(), root == null ? null : root.getId(), SkillTree.getAllNodes());
    }

    private NodeAllocator.Snapshot snapshot() {
        if (allocationSnapshot == null) {
            allocationSnapshot = allocator.snapshot();
        }
        return allocationSnapshot;
    }

    public ShipOpBudget budget() {
        return snapshot().opBudget();
    }

    public int statsRevision() {
        return snapshot().statsRevision();
    }

    public void render(TreeViewport viewport, float alphaMult, float mouseX, float mouseY, boolean mouseKnown) {
        NodeAllocator.Snapshot allocation = snapshot();
        float treeAlphaMult = alphaMult * rootChoice.treeAlpha();

        for (SkillNode node : SkillTree.topology().nonRoots()) {
            renderNode(node, viewport, treeAlphaMult, allocation);
        }

        connectorRenderer.draw(viewport, allocation, templateNodeIds, connectorFills, treeAlphaMult);
        wormholeGhostFlights.draw(viewport, treeAlphaMult * nodeSearch.backgroundAlpha());

        for (SkillNode node : SkillTree.topology().roots()) {
            renderRootNode(node, viewport, alphaMult, allocation);
        }

        if (isChoosingStartingRoot()) {
            renderStartingRootPrompt(viewport.centerX(), viewport.screenY(rootChoice.promptOffsetY()), alphaMult);
        }

        dropdownRenderer.render(viewport, mouseX, mouseY, mouseKnown, alphaMult);
    }

    private void renderStartingRootPrompt(float x, float y, float alphaMult) {
        startingRootPrompt.setAlpha(alphaMult).draw(x, y);
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
        boolean breathing = !allocated && allocation.canAllocate(node);
        SkillType effectiveType = node.resolveEffectiveType(skillData);
        float iconSize = footprintSize * ICON_INSET_RATIO;

        float nodeAlpha = alphaMult * nodeSearch.nodeAlpha(node, allocation);
        if (tier == SkillTier.SOCKET) {
            socketRenderer.drawFrame(nodeX, nodeY, footprintSize, allocated, panelStyle.getAccentColor(), nodeAlpha);
            socketRenderer.drawContent(nodeX, nodeY, footprintSize, socketedIcon(skillData, node), iconTint(node, allocation, allocated),
                    nodeAlpha);
        }
        ringRenderer.draw(nodeX, nodeY, footprintSize, nodeAlpha, SkillTreeNodeRingRenderer.RingState.of(allocated, breathing), zoom, node);

        if (tier == SkillTier.SOCKET) {
            if (allocated) {
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

    private void renderRootNode(SkillNode node, TreeViewport viewport, float alphaMult, NodeAllocator.Snapshot allocation) {
        ShipSkillData skillData = allocation.skillData();
        float zoom = viewport.zoom();
        float nodeX = viewport.screenX(rootChoice.offsetX(node));
        float nodeY = viewport.screenY(rootChoice.offsetY(node));
        float footprintSize = NODE_SIZE * zoom * SkillTier.ROOT.getSizeMultiplier();
        if (!viewport.isVisible(nodeX, nodeY, ringRenderer.reach(footprintSize, node))) {
            return;
        }
        boolean choosing = isChoosingStartingRoot();
        boolean allocated = skillData.isAllocated(node.getId());
        boolean breathing = choosing || (!allocated && allocation.canAllocate(node));
        float nodeAlpha = alphaMult * nodeSearch.nodeAlpha(node, allocation);
        ringRenderer.draw(nodeX, nodeY, footprintSize, nodeAlpha, SkillTreeNodeRingRenderer.RingState.of(allocated, breathing), zoom, node);

        Color tint = choosing ? ALLOCATED_TINT : iconTint(node, allocation, allocated);
        iconRenderer.drawIcon(node.getType().getIconPath(), nodeX, nodeY, footprintSize, nodeAlpha, tint);
    }

    private Color iconTint(SkillNode node, NodeAllocator.Snapshot allocation, boolean allocated) {
        return allocated || nodeSearch.matches(node, allocation) ? ALLOCATED_TINT : UNALLOCATED_TINT;
    }

    public void renderHoverTooltip(TreeViewport viewport, float mouseX, float mouseY, float alphaMult) {
        if (dropdownRenderer.isOpen()) {
            SkillType hoveredOption = dropdownRenderer.findOptionAt(viewport, mouseX, mouseY);
            if (hoveredOption != null) {
                tooltipRenderer.renderTooltipForType(hoveredOption, mouseX, mouseY, alphaMult);
            }
            return;
        }

        SkillNode hoveredNode = findNodeAt(viewport, mouseX, mouseY);
        if (hoveredNode != null) {
            tooltipRenderer.renderTooltip(hoveredNode, snapshot(), mouseX, mouseY, alphaMult);
        }
    }

    public SkillNode findNodeAt(TreeViewport viewport, float screenX, float screenY) {
        if (isStartingRootMoving()) {
            return null;
        }
        boolean choosing = isChoosingStartingRoot();
        for (SkillNode node : SkillTree.getAllNodes().values()) {
            if (choosing && node.getType().getTier() != SkillTier.ROOT) {
                continue;
            }
            float nodeX = viewport.screenX(rootChoice.offsetX(node));
            float nodeY = viewport.screenY(rootChoice.offsetY(node));
            float halfSize = NODE_SIZE * viewport.zoom() * node.getType().getTier().getSizeMultiplier() / 2f;
            if (Math.abs(screenX - nodeX) <= halfSize && Math.abs(screenY - nodeY) <= halfSize) {
                return node;
            }
        }
        return null;
    }

    public SkillNode wormholeJumpTarget(SkillNode node, boolean ctrlDown) {
        if (!ctrlDown) return null;
        if (node.getType().getTier() != SkillTier.WORMHOLE) return null;
        String pairedId = node.getPairedNodeId();
        SkillNode paired = pairedId == null ? null : SkillTree.get(pairedId);
        if (paired == null) return null;
        NodeAllocator.Snapshot allocation = snapshot();
        if (allocation.isHidden(node) || allocation.isHidden(paired)) return null;
        return paired;
    }

    public void launchWormholeGhosts(SkillNode from, SkillNode to) {
        wormholeGhostFlights.launchFrom(from, to);
    }

    public void toggleAllocation(SkillNode node, boolean ctrlDown) {
        if (isStartingRootInputLocked() || autoAllocateRun != null || respecRun != null) {
            return;
        }
        if (allocator.canUnchooseStartingRoot(node)) {
            unchooseStartingRoot(node);
            return;
        }
        boolean wasAllocated = allocator.data().isAllocated(node.getId());
        boolean isOptional = node.getType().isOptional();

        if (!wasAllocated && isOptional) {
            toggleOptionalAllocation(node, ctrlDown);
            return;
        }

        if (!canToggle(node, wasAllocated, isOptional)) {
            return;
        }

        if (allocator.toggle(node)) {
            afterAllocationChange(node, !wasAllocated);
        }
    }

    private static String socketedIcon(ShipSkillData skillData, SkillNode node) {
        Socketable socketed = SocketableStore.lookup(skillData.getSocketedItem(node.getId()));
        return socketed == null ? null : socketed.iconPath();
    }

    public boolean installInSocket(SkillNode node, Socketable socketable) {
        if (isSocketEditLocked() || !socketable.canSocketInto(node)
                || SocketCustody.installations(ShipSkillDataManager.all()).containsKey(socketable.id())
                || !allocator.socketItem(node, socketable.id())) {
            return false;
        }
        refreshAfterAllocation();
        SkillTreeSounds.socketed();
        ringRenderer.startPulse(node.getId());
        return true;
    }

    public boolean emptySocket(SkillNode node) {
        if (isSocketEditLocked() || !allocator.unsocketItem(node)) {
            return false;
        }
        refreshAfterAllocation();
        return true;
    }

    private boolean isSocketEditLocked() {
        return isStartingRootInputLocked() || autoAllocateRun != null || respecRun != null;
    }

    private void toggleOptionalAllocation(SkillNode node, boolean ctrlDown) {
        if (!allocator.canAllocate(node)) {
            return;
        }
        SkillType repeatedOption = ctrlDown ? repeatableOptionFor(node) : null;
        if (repeatedOption != null) {
            allocateOptionalNode(node, repeatedOption);
        } else {
            dropdownRenderer.open(node);
        }
    }

    private boolean canToggle(SkillNode node, boolean wasAllocated, boolean isOptional) {
        if (!wasAllocated) {
            return allocator.blockAllocationReason(node, null) == null;
        }

        boolean canDeallocate = allocator.canDeallocate(node);
        if (!canDeallocate && isOptional) {
            dropdownRenderer.open(node);
        }
        return canDeallocate;
    }

    public boolean isDropdownOpen() {
        return dropdownRenderer.isOpen();
    }

    public void closeDropdown() {
        dropdownRenderer.close();
    }

    public SkillType findDropdownOptionAt(TreeViewport viewport, float x, float y) {
        return dropdownRenderer.findOptionAt(viewport, x, y);
    }

    public void commitDropdownSelection(SkillType chosenOption) {
        SkillNode node = dropdownRenderer.getOpenNode();
        dropdownRenderer.close();
        if (node == null) return;
        if (allocator.blockAllocationReason(node, chosenOption) != null) return;

        allocateOptionalNode(node, chosenOption);
    }

    private SkillType repeatableOptionFor(SkillNode node) {
        if (lastChosenOptionalOption == null) return null;
        if (!node.getType().getOptionalOptionIds().contains(lastChosenOptionalOption.getId())) return null;
        if (allocator.blockAllocationReason(node, lastChosenOptionalOption) != null) return null;
        return lastChosenOptionalOption;
    }

    private void allocateOptionalNode(SkillNode node, SkillType chosenOption) {
        boolean wasAllocated = allocator.data().isAllocated(node.getId());
        allocator.allocateOption(node, chosenOption);
        lastChosenOptionalOption = chosenOption;
        if (wasAllocated) {
            refreshAfterAllocation();
            SkillTreeSounds.allocated(node.getType().getTier());
            ringRenderer.startPulse(node.getId());
        } else {
            afterAllocationChange(node, true);
        }
    }

    private void refreshAfterAllocation() {
        allocationSnapshot = null;
        if (refitButton != null) {
            refitButton.refreshVariant();
        }
    }

    private void afterAllocationChange(SkillNode node, boolean isAllocatedNow) {
        refreshAfterAllocation();
        if (isAllocatedNow) {
            SkillTreeSounds.allocated(node.getType().getTier());
        } else {
            SkillTreeSounds.deallocated();
        }
        if (!isAllocatedNow) {
            connectorFills.cancel(node.getId());
            if (node.getPairedNodeId() != null) {
                connectorFills.cancel(node.getPairedNodeId());
            }
        } else if (startFillsInto(node)) {
            connectorFills.schedulePulse(node.getId());
        } else {
            ringRenderer.startPulse(node.getId());
        }
    }

    private boolean startFillsInto(SkillNode node) {
        NodeAllocator.Snapshot allocation = snapshot();
        boolean started = false;
        for (SkillNode neighbour : SkillTree.topology().drawnNeighbours(node.getId())) {
            if (allocation.skillData().isSatisfied(neighbour.getId(), allocation.satisfiedRootId())) {
                connectorFills.start(neighbour.getId(), node.getId());
                started = true;
            }
        }
        return started;
    }
}
