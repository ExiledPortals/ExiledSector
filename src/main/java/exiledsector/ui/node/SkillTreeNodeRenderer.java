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
import exiledsector.ui.TreeViewport;
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
    private final SkillTreePanelStyle style;
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
    private final NodeSearch search;
    private final WormholeOpenness wormholeOpenness = new WormholeOpenness();
    private final ConnectorFills connectorFills = new ConnectorFills();
    private final Consumer<String> startPulse;

    private final TemplateStepExecutor stepExecutor;
    private final Function<TemplateStep, StepVerdict> attemptStep;
    private final BooleanSupplier pointsLeft;

    private SkillType lastChosenOptionalOption;
    private LazyFont.DrawableString startingRootPrompt;
    private NodeAllocator.Snapshot snapshot;
    private SkillTreeTemplate template;
    private Set<String> templateNodeIds = Set.of();
    private AutoAllocateRun autoRun;
    private AutoAllocateRun.Summary lastRunSummary;
    private RespecRun respecRun;

    public SkillTreeNodeRenderer(FleetMemberAPI member, ShipVariantAPI variant, SkillTreePanelStyle style, BaseRefitButton refitButton,
                                 NodeSearch search) {
        this.refitButton = refitButton;
        this.search = search;
        this.style = style;
        this.rootChoice = initialRootChoice(ShipSkillDataManager.get(member.getId()));
        this.allocator = new NodeAllocator(member, variant, () -> rootChoice.rootForAllocation());
        SkillNode chosenRoot = rootChoice.chosen();
        style.setAccentIconPath(chosenRoot != null ? chosenRoot.getType().getIconPath() : null);

        this.ringRenderer = new SkillTreeNodeRingRenderer(style, wormholeOpenness);
        this.startPulse = ringRenderer::startPulse;
        this.iconRenderer = new SkillTreeNodeIconRenderer();
        this.ghostRenderer = new SkillTreeNodeGhostRenderer();
        this.wormholeGhostFlights = new SkillTreeWormholeGhostFlights(new Random());
        this.connectorRenderer = new SkillTreeNodeConnectorRenderer(style, search, wormholeOpenness);
        this.tooltipRenderer = new SkillTreeNodeTooltipRenderer(member, style);
        this.dropdownRenderer = new SkillTreeNodeDropdownRenderer(style);
        this.stepExecutor = new TemplateStepExecutor(allocator, this::snapshot, node -> afterAllocationChange(node, true));
        this.attemptStep = stepExecutor::attempt;
        this.pointsLeft = stepExecutor::hasPointsLeft;
    }

    private static StartingRootChoice initialRootChoice(ShipSkillData data) {
        String startingRootId = data.resolveStartingRootId();
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
        search.setQuery("");
        rootChoice = StartingRootChoice.returning(SkillTree.topology().roots(), root);
        style.setAccentIconPath(null);
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
        style.setAccentIconPath(root.getType().getIconPath());
        afterAllocationChange(root, true);
    }

    public void advance(float amount) {
        rootChoice.advance(amount);
        ringRenderer.advance(amount);
        connectorFills.advance(amount, startPulse);
        ghostRenderer.advance(amount);
        socketRenderer.advance(amount);
        ShipSkillData data = snapshot().data();
        wormholeGhostFlights.advance(amount, data);
        wormholeOpenness.advance(amount, data);
        advanceAutoAllocate(amount);
        advanceRespec(amount);
    }

    private void advanceAutoAllocate(float amount) {
        if (autoRun == null || rootChoice.isInputLocked()) {
            return;
        }
        autoRun.advance(amount, attemptStep, pointsLeft);
        if (autoRun.isFinished()) {
            lastRunSummary = autoRun.summary();
            autoRun = null;
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
        if (isStartingRootInputLocked() || autoRun != null || respecRun != null) {
            return false;
        }
        List<SkillNode> plan = allocator.respecPlan(node);
        if (plan.isEmpty() || plan.stream().anyMatch(allocator::hasDeallocationCondition)) {
            return false;
        }
        dropdownRenderer.close();
        respecRun = new RespecRun(plan);
        return true;
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
        if (template == null || autoRun != null || respecRun != null || rootChoice.isInputLocked()) {
            return false;
        }
        dropdownRenderer.close();
        ShipSkillData data = snapshot().data();
        int pending = 0;
        for (TemplateStep step : template.steps()) {
            if (SkillTree.get(step.nodeId()) != null && !data.isAllocated(step.nodeId())) {
                pending++;
            }
        }
        autoRun = new AutoAllocateRun(template.steps(), pending);
        lastRunSummary = null;
        return true;
    }

    public boolean isAutoAllocating() {
        return autoRun != null;
    }

    public void cancelAutoAllocate() {
        if (autoRun != null) {
            autoRun.cancel();
            lastRunSummary = autoRun.summary();
            autoRun = null;
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
        return snapshot().data().getAllocatedNodeIds().size();
    }

    public List<TemplateStep> captureTemplateSteps() {
        SkillNode root = getStartingRoot();
        return TemplateCapture.capture(snapshot().data(), root == null ? null : root.getId(), SkillTree.getAllNodes());
    }

    private NodeAllocator.Snapshot snapshot() {
        if (snapshot == null) {
            snapshot = allocator.snapshot();
        }
        return snapshot;
    }

    public ShipOpBudget budget() {
        return snapshot().budget();
    }

    public int statsRevision() {
        return snapshot().revision();
    }

    public void render(TreeViewport viewport, float alphaMult, float mouseX, float mouseY, boolean mouseKnown) {
        NodeAllocator.Snapshot allocation = snapshot();
        float treeAlphaMult = alphaMult * rootChoice.treeAlpha();

        for (SkillNode node : SkillTree.topology().nonRoots()) {
            renderNode(node, viewport, treeAlphaMult, allocation);
        }

        connectorRenderer.draw(viewport, allocation, templateNodeIds, connectorFills, treeAlphaMult);
        wormholeGhostFlights.draw(viewport, treeAlphaMult * search.backgroundAlpha());

        for (SkillNode node : SkillTree.topology().roots()) {
            renderRootNode(node, viewport, alphaMult, allocation);
        }

        if (isChoosingStartingRoot()) {
            renderStartingRootPrompt(viewport.centerX(), viewport.screenY(rootChoice.promptOffsetY()));
        }

        dropdownRenderer.render(viewport, mouseX, mouseY, mouseKnown, alphaMult);
    }

    private void renderStartingRootPrompt(float x, float y) {
        LazyFont font = SkillTreePanelStyle.font();
        if (font == null) {
            return;
        }
        if (startingRootPrompt == null) {
            startingRootPrompt = SkillTreePanelStyle.buildSimpleText(font, Translation.text("ui.node.startingRootPrompt"),
                    SkillTreePanelStyle.TOOLTIP_TITLE_FONT_SIZE, SkillTreePanelStyle.TOOLTIP_TITLE_COLOR, LazyFont.TextAnchor.BOTTOM_CENTER);
        }
        startingRootPrompt.draw(x, y);
    }

    private void renderNode(SkillNode node, TreeViewport viewport, float alphaMult, NodeAllocator.Snapshot allocation) {
        ShipSkillData data = allocation.data();
        SkillTier tier = node.getType().getTier();
        float zoom = viewport.zoom();
        float nodeX = viewport.screenX(node.getOffsetX());
        float nodeY = viewport.screenY(node.getOffsetY());
        float footprintSize = NODE_SIZE * zoom * tier.getSizeMultiplier();
        if (!viewport.isVisible(nodeX, nodeY, ringRenderer.reach(footprintSize, node))) {
            return;
        }

        if (allocation.isHidden(node)) {
            ghostRenderer.draw(nodeX, nodeY, footprintSize, alphaMult * search.backgroundAlpha(), node.getId());
            return;
        }

        boolean allocated = data.isAllocated(node.getId());
        boolean breathing = !allocated && allocation.canAllocate(node);
        SkillType effectiveType = node.resolveEffectiveType(data);
        float iconSize = footprintSize * ICON_INSET_RATIO;

        float nodeAlpha = alphaMult * search.nodeAlpha(node, allocation);
        if (tier == SkillTier.SOCKET) {
            socketRenderer.drawFrame(nodeX, nodeY, footprintSize, allocated, style.getAccentColor(), nodeAlpha);
            socketRenderer.drawContent(nodeX, nodeY, footprintSize, socketedIcon(data, node), iconTint(node, allocation, allocated),
                    nodeAlpha);
        }
        ringRenderer.draw(nodeX, nodeY, footprintSize, nodeAlpha, SkillTreeNodeRingRenderer.RingState.of(allocated, breathing), zoom, node);

        if (tier == SkillTier.SOCKET) {
            if (allocated) {
                socketRenderer.drawArcs(node.getId(), nodeX, nodeY, footprintSize, style.getAccentColor(), nodeAlpha);
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
        ShipSkillData data = allocation.data();
        float zoom = viewport.zoom();
        float nodeX = viewport.screenX(rootChoice.offsetX(node));
        float nodeY = viewport.screenY(rootChoice.offsetY(node));
        float footprintSize = NODE_SIZE * zoom * SkillTier.ROOT.getSizeMultiplier();
        if (!viewport.isVisible(nodeX, nodeY, ringRenderer.reach(footprintSize, node))) {
            return;
        }
        boolean choosing = isChoosingStartingRoot();
        boolean allocated = data.isAllocated(node.getId());
        boolean breathing = choosing || (!allocated && allocation.canAllocate(node));
        float nodeAlpha = alphaMult * search.nodeAlpha(node, allocation);
        ringRenderer.draw(nodeX, nodeY, footprintSize, nodeAlpha, SkillTreeNodeRingRenderer.RingState.of(allocated, breathing), zoom, node);

        Color tint = choosing ? ALLOCATED_TINT : iconTint(node, allocation, allocated);
        iconRenderer.drawIcon(node.getType().getIconPath(), nodeX, nodeY, footprintSize, nodeAlpha, tint);
    }

    private Color iconTint(SkillNode node, NodeAllocator.Snapshot tree, boolean allocated) {
        return allocated || search.matches(node, tree) ? ALLOCATED_TINT : UNALLOCATED_TINT;
    }

    public void renderHoverTooltip(TreeViewport viewport, float mouseX, float mouseY, float alphaMult) {
        if (dropdownRenderer.isOpen()) {
            SkillType hovered = dropdownRenderer.findOptionAt(viewport, mouseX, mouseY);
            if (hovered != null) {
                tooltipRenderer.renderTooltipForType(hovered, mouseX, mouseY, alphaMult);
            }
            return;
        }

        SkillNode hovered = findNodeAt(viewport, mouseX, mouseY);
        if (hovered != null) {
            tooltipRenderer.renderTooltip(hovered, snapshot(), mouseX, mouseY, alphaMult);
        }
    }

    public SkillNode findNodeAt(TreeViewport viewport, float x, float y) {
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
            if (Math.abs(x - nodeX) <= halfSize && Math.abs(y - nodeY) <= halfSize) {
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
        NodeAllocator.Snapshot tree = snapshot();
        if (tree.isHidden(node) || tree.isHidden(paired)) return null;
        return paired;
    }

    public void launchWormholeGhosts(SkillNode from, SkillNode to) {
        wormholeGhostFlights.launchFrom(from, to);
    }

    public void toggleAllocation(SkillNode node, boolean ctrlDown) {
        if (isStartingRootInputLocked() || autoRun != null || respecRun != null) {
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

    private static String socketedIcon(ShipSkillData data, SkillNode node) {
        Socketable socketed = SocketableStore.lookup(data.getSocketedItem(node.getId()));
        return socketed == null ? null : socketed.iconPath();
    }

    public Socketable socketedItem(SkillNode node) {
        return SocketableStore.lookup(allocator.data().getSocketedItem(node.getId()));
    }

    public boolean installInSocket(SkillNode node, Socketable socketable) {
        if (isSocketEditLocked() || !socketable.canSocketInto(node)
                || SocketCustody.installations(ShipSkillDataManager.all()).containsKey(socketable.id())
                || !allocator.socketItem(node, socketable.id())) {
            return false;
        }
        refreshAfterAllocation();
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
        return isStartingRootInputLocked() || autoRun != null || respecRun != null;
    }

    private void toggleOptionalAllocation(SkillNode node, boolean ctrlDown) {
        if (!allocator.canAllocate(node)) {
            return;
        }
        SkillType repeated = ctrlDown ? repeatableOptionFor(node) : null;
        if (repeated != null) {
            allocateOptionalNode(node, repeated);
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
            ringRenderer.startPulse(node.getId());
        } else {
            afterAllocationChange(node, true);
        }
    }

    private void refreshAfterAllocation() {
        snapshot = null;
        if (refitButton != null) {
            refitButton.refreshVariant();
        }
    }

    private void afterAllocationChange(SkillNode node, boolean isAllocatedNow) {
        refreshAfterAllocation();
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
        NodeAllocator.Snapshot tree = snapshot();
        boolean started = false;
        for (SkillNode neighbour : SkillTree.topology().drawnNeighbours(node.getId())) {
            if (tree.data().isSatisfied(neighbour.getId(), tree.satisfiedRootId())) {
                connectorFills.start(neighbour.getId(), node.getId());
                started = true;
            }
        }
        return started;
    }
}
