package exiledsector.ui.node;

import exiledlib.i18n.Translation;
import exiledsector.items.NodeSocketable;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.PhantomConflictWatch;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import exiledsector.skills.progression.ShipOpBudget;
import exiledsector.skills.template.AutoAllocateRun;
import exiledsector.skills.template.SkillTreeTemplate;
import exiledsector.skills.template.StepVerdict;
import exiledsector.skills.template.TemplateCapture;
import exiledsector.skills.template.TemplateStep;
import exiledsector.skills.unlock.NodeGates;
import exiledsector.skills.unlock.SkillTypeUnlockStatus;
import exiledsector.items.SocketCustody;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.Subroutine;
import exiledsector.items.SubroutineKind;
import exiledsector.items.SubroutineRadius;
import exiledsector.items.SubroutineSocketLimits;
import exiledsector.items.NodeGrantLinks;
import exiledsector.items.WormholeAnchor;
import exiledsector.items.WormholeAnchorLinks;
import exiledsector.items.WormholeAnchorSocketLimits;
import exiledsector.items.UseVerdict;
import exiledsector.ui.SkillTreeSounds;
import lunalib.lunaRefit.BaseRefitButton;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TreeAllocationSession {

    private static final String DEALLOCATE_ACTION = "deallocate:";
    private static final String INSTALL_ACTION = "install:";
    private static final String EMPTY_ACTION = "empty:";

    private final BaseRefitButton refitButton;
    private final NodeSearch nodeSearch;
    private final NodeAllocator allocator;
    private final TemplateStepExecutor stepExecutor;
    private final Function<TemplateStep, StepVerdict> attemptStep;
    private final BooleanSupplier pointsLeftCheck;
    private final Predicate<SkillType> dropdownOptionUsable = this::isDropdownOptionUsable;
    private final TemplatePlanner planner;
    private final ReleaseConfirmation releaseConfirmation = new ReleaseConfirmation();

    private TreeFeedback feedback = TreeFeedback.NONE;
    private StartingRootChoice rootChoice;
    private NodeAllocator.Snapshot allocationSnapshot;
    private SkillType lastChosenOptionalOption;
    private SkillNode dropdownNode;
    private String targetedSocketId;
    private SkillTreeTemplate template;
    private Set<String> templateNodeIds = Set.of();
    private AutoAllocateRun.Summary lastRunSummary;
    private Runnable planChangeListener = () -> {
    };

    public TreeAllocationSession(FleetMemberAPI member, ShipVariantAPI variant, BaseRefitButton refitButton, NodeSearch nodeSearch) {
        this.refitButton = refitButton;
        this.nodeSearch = nodeSearch;
        this.rootChoice = initialRootChoice(ShipSkillDataManager.get(member.getId()));
        this.allocator = new NodeAllocator(member, variant, () -> rootChoice.rootForAllocation());
        this.stepExecutor = new TemplateStepExecutor(allocator, this::snapshot, node -> settleAllocation());
        this.attemptStep = stepExecutor::attempt;
        this.pointsLeftCheck = stepExecutor::hasPointsLeft;
        this.planner = new TemplatePlanner(allocator, this::snapshot);
        WormholeAnchorLinks.refresh();
        NodeGrantLinks.refresh();
        allocator.openAnchorWormholes();
        allocator.grantSocketedNodes();
    }

    static SkillTreeSounds.NodeSize allocationSound(SkillTier tier) {
        return switch (tier) {
            case KEYSTONE, ROOT -> SkillTreeSounds.NodeSize.LARGE;
            case NOTABLE, WORMHOLE, WORMHOLE_EXIT -> SkillTreeSounds.NodeSize.MEDIUM;
            case SMALL, SOCKET -> SkillTreeSounds.NodeSize.SMALL;
        };
    }

    private static StartingRootChoice initialRootChoice(ShipSkillData skillData) {
        String startingRootId = ShipTrees.resolveStartingRootId(skillData);
        SkillNode startingRoot = startingRootId == null ? null : SkillTree.get(startingRootId);
        if (startingRoot != null) {
            return StartingRootChoice.alreadyChosen(startingRoot);
        }
        return StartingRootChoice.pending(SkillTree.topology().roots());
    }

    void listen(TreeFeedback treeFeedback) {
        this.feedback = treeFeedback;
    }

    StartingRootChoice rootChoice() {
        return rootChoice;
    }

    NodeAllocator.Snapshot snapshot() {
        if (allocationSnapshot == null || !allocationSnapshot.isCurrent(allocator.data())) {
            allocationSnapshot = allocator.snapshot();
        }
        return allocationSnapshot;
    }

    NodeAllocator.Snapshot viewSnapshot() {
        return planner.isActive() ? planner.snapshot() : snapshot();
    }

    Set<String> templateNodeIds() {
        return planner.isActive() ? planner.nodeIds() : templateNodeIds;
    }

    Set<String> plannedOnlyNodeIds() {
        return planner.plannedOnlyNodeIds();
    }

    boolean isPlannedOnly(SkillNode node) {
        return planner.isPlannedOnly(node);
    }

    SkillNode dropdownNode() {
        return dropdownNode;
    }

    Predicate<SkillType> dropdownOptionUsable() {
        return dropdownOptionUsable;
    }

    String targetedSocketId() {
        return targetedSocketId;
    }

    public void advance(float amount) {
        if (PhantomConflictWatch.hasPendingReverts()) {
            Set<String> allocatedBefore = allocatedNodeIds();
            if (allocator.applyPendingPhantomConflicts()) {
                afterAllocationChange(allocatedBefore, null);
                SkillTreeSounds.refused();
            }
        }
        rootChoice.advance(amount);
        releaseConfirmation.advance(amount);
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
        Set<String> allocatedBefore = allocatedNodeIds();
        if (!isChoosingStartingRoot() || !allocator.chooseStartingRoot(root)) {
            return;
        }
        rootChoice.choose(root);
        feedback.rootChanged(root);
        afterAllocationChange(allocatedBefore, root);
    }

    private boolean unchooseStartingRoot(SkillNode root) {
        if (!allocator.unchooseStartingRoot(root)) {
            return false;
        }
        dropdownNode = null;
        stopPlanning();
        setTemplate(null);
        nodeSearch.setQuery("");
        rootChoice = StartingRootChoice.returning(SkillTree.topology().roots(), root);
        feedback.rootChanged(null);
        return true;
    }

    public boolean isAllocated(SkillNode node) {
        return allocator.data().isAllocated(node.getId());
    }

    public boolean isHidden(SkillNode node) {
        return node != null && viewSnapshot().isHidden(node);
    }

    public boolean isLocked(SkillNode node) {
        ShipSkillData skillData = allocator.data();
        return node != null && (SkillTypeUnlockStatus.isLocked(node.getType(), skillData) || NodeGates.isLocked(node, skillData));
    }

    public SkillNode nextSearchMatch() {
        return nodeSearch.nextMatch(SkillTree.getAllNodes().values(), viewSnapshot());
    }

    String wormholePartner(SkillNode node) {
        return ShipTreeLinks.wormholePartner(allocator.data(), node);
    }

    public List<SkillTreeTopology.WormholePair> wormholePairs() {
        return ShipTreeLinks.wormholePairs(viewSnapshot().skillData(), SkillTree.topology());
    }

    public Item socketedItem(SkillNode node) {
        return node == null ? null : ItemStore.lookup(allocator.data().getSocketedItem(node.getId()));
    }

    public SubroutineRadius.Footprint socketedRadiusFootprint() {
        for (Map.Entry<String, String> socketed : allocator.data().getSocketedItems().entrySet()) {
            SubroutineRadius.Footprint footprint = SubroutineRadius.footprint(ItemStore.lookup(socketed.getValue()), socketed.getKey());
            if (footprint != null) {
                return footprint;
            }
        }
        return null;
    }

    public String doctrineRemovalWarning(SkillNode socket) {
        List<String> lostHullModNames = allocator.hullModNamesLostOnDoctrineRemoval(socket);
        return lostHullModNames.isEmpty() ? null
                : Translation.msg("socketable.doctrine.revertConflict").arg("hullMods", String.join(", ", lostHullModNames)).text();
    }

    public String socketHolding(Item item) {
        if (item == null) {
            return null;
        }
        for (Map.Entry<String, String> socketed : allocator.data().getSocketedItems().entrySet()) {
            if (item.id().equals(socketed.getValue())) {
                return socketed.getKey();
            }
        }
        return null;
    }

    public boolean isAllocatedSocket(SkillNode node) {
        return node != null && node.getType().getTier() == SkillTier.SOCKET && isAllocated(node);
    }

    public ShipOpBudget budget() {
        return snapshot().opBudget();
    }

    public int statsRevision() {
        return snapshot().statsRevision();
    }

    public int allocatedNodeCount() {
        return snapshot().skillData().usedNodeSlots();
    }

    public List<TemplateStep> captureTemplateSteps() {
        SkillNode root = getStartingRoot();
        return TemplateCapture.capture(snapshot().skillData(), root == null ? null : root.getId(), SkillTree.getAllNodes());
    }

    public boolean isBusy() {
        return isStartingRootInputLocked();
    }

    public void clickNode(SkillNode node, boolean ctrlDown) {
        if (planner.isActive() && !isStartingRootInputLocked()) {
            clickPlannedNode(node, ctrlDown);
            return;
        }
        if (isBusy()) {
            return;
        }
        Set<String> allocatedBefore = allocatedNodeIds();
        if (allocator.canUnchooseStartingRoot(node)) {
            if (unchooseStartingRoot(node)) {
                afterAllocationChange(allocatedBefore, node);
            }
            return;
        }
        NodeAllocator.Snapshot allocation = snapshot();
        if (allocation.skillData().isAllocated(node.getId())) {
            removeOrSwitchOption(node, allocation);
        } else if (!allocation.canAllocate(node)) {
            if (!allocation.isHidden(node)) {
                SkillTreeSounds.refused();
            }
        } else if (allocation.gate().offersOptions(node)) {
            chooseOption(node, ctrlDown);
        } else if (allocator.allocate(node, null, allocation)) {
            afterAllocationChange(allocatedBefore, node);
        }
    }

    private void removeOrSwitchOption(SkillNode node, NodeAllocator.Snapshot allocation) {
        boolean anchorLinked = ShipTreeLinks.anchorPartner(allocation.skillData(), node.getId()) != null;
        boolean holdsGrant = ShipTreeLinks.grantedNodesBySocket(allocation.skillData()).containsKey(node.getId());
        boolean holdsDoctrine = holdsDoctrine(allocation.skillData(), node);
        if ((anchorLinked || holdsGrant || holdsDoctrine) && allocation.gate().deallocation(node).allowed()
                && !confirmRelease(DEALLOCATE_ACTION, node, planned -> ShipTrees.deallocate(planned, node))) {
            return;
        }
        Set<String> allocatedBefore = allocatedNodeIds();
        if (allocator.deallocate(node, allocation)) {
            afterAllocationChange(allocatedBefore, node);
        } else if (allocation.gate().offersOptions(node)) {
            dropdownNode = node;
        } else {
            SkillTreeSounds.refused();
        }
    }

    public SkillNode wormholeJumpTarget(SkillNode node, boolean ctrlDown) {
        if (!ctrlDown) {
            return null;
        }
        ShipSkillData liveData = allocator.data();
        String pairedId = wormholePartner(node);
        SkillNode paired = pairedId == null ? null : SkillTree.get(pairedId);
        boolean closedAnchorWormhole = ShipTreeLinks.anchorPartner(liveData, node.getId()) != null
                && !(liveData.isAllocated(node.getId()) && liveData.isAllocated(pairedId));
        if (paired == null || closedAnchorWormhole) {
            return null;
        }
        NodeAllocator.Snapshot allocation = snapshot();
        return allocation.isHidden(node) || allocation.isHidden(paired) ? null : paired;
    }

    public boolean isDropdownOpen() {
        return dropdownNode != null;
    }

    public void closeDropdown() {
        dropdownNode = null;
    }

    public void commitDropdownSelection(SkillType chosenOption) {
        SkillNode node = dropdownNode;
        dropdownNode = null;
        if (node != null) {
            allocateOptionalNode(node, chosenOption);
        }
    }

    private void chooseOption(SkillNode node, boolean ctrlDown) {
        SkillType repeatedOption = ctrlDown ? repeatableOptionFor(node) : null;
        if (repeatedOption != null) {
            allocateOptionalNode(node, repeatedOption);
        } else {
            dropdownNode = node;
        }
    }

    private SkillType repeatableOptionFor(SkillNode node) {
        if (lastChosenOptionalOption == null || !node.getType().getOptionalOptionIds().contains(lastChosenOptionalOption.getId())) {
            return null;
        }
        return viewSnapshot().gate().allocation(node, lastChosenOptionalOption).allowed() ? lastChosenOptionalOption : null;
    }

    private boolean isDropdownOptionUsable(SkillType option) {
        if (dropdownNode == null) {
            return false;
        }
        AllocationGate.Verdict verdict = optionVerdict(dropdownNode, option, viewSnapshot());
        return verdict.allowed() || verdict.refusal() == AllocationGate.Refusal.SAME_OPTION;
    }

    String dropdownOptionRefusalReason(SkillType option) {
        return dropdownNode == null ? null : viewSnapshot().optionRefusalReason(dropdownNode, option);
    }

    private static AllocationGate.Verdict optionVerdict(SkillNode node, SkillType option, NodeAllocator.Snapshot allocation) {
        return allocation.skillData().isAllocated(node.getId())
                ? allocation.gate().optionSwitch(node, option) : allocation.gate().allocation(node, option);
    }

    private void allocateOptionalNode(SkillNode node, SkillType chosenOption) {
        if (planner.isActive()) {
            planOptionalNode(node, chosenOption);
            return;
        }
        NodeAllocator.Snapshot allocation = snapshot();
        Set<String> allocatedBefore = allocatedNodeIds();
        boolean wasAllocated = allocation.skillData().isAllocated(node.getId());
        if (optionVerdict(node, chosenOption, allocation).refusal() == AllocationGate.Refusal.SAME_OPTION) {
            return;
        }
        boolean changed = wasAllocated ? allocator.switchOption(node, chosenOption, allocation) : allocator.allocate(node, chosenOption, allocation);
        if (!changed) {
            SkillTreeSounds.refused();
            return;
        }
        lastChosenOptionalOption = chosenOption;
        if (wasAllocated) {
            refreshAfterAllocation();
            SkillTreeSounds.allocated(allocationSound(node.getType().getTier()));
            feedback.pulse(node);
        } else {
            afterAllocationChange(allocatedBefore, node);
        }
    }

    public boolean respec(SkillNode node) {
        if (isBusy() || planner.isActive()) {
            return false;
        }
        List<SkillNode> respecPlan = allocator.respecPlan(node);
        if (respecPlan.isEmpty()) {
            return false;
        }
        dropdownNode = null;
        Set<String> allocatedBefore = allocatedNodeIds();
        for (SkillNode step : respecPlan) {
            if (!removeForRespec(step)) {
                break;
            }
        }
        afterAllocationChange(allocatedBefore, node);
        return true;
    }

    private boolean removeForRespec(SkillNode node) {
        if (allocator.canUnchooseStartingRoot(node)) {
            return unchooseStartingRoot(node);
        }
        if (!allocator.data().isAllocated(node.getId())) {
            return true;
        }
        if (!allocator.deallocate(node, snapshot())) {
            return false;
        }
        settleAllocation();
        return true;
    }

    public void setTemplate(SkillTreeTemplate newTemplate) {
        this.template = newTemplate;
        this.templateNodeIds = newTemplate == null ? Set.of() : newTemplate.nodeIds();
        if (planner.isActive()) {
            dropdownNode = null;
            planner.start(newTemplate);
        }
    }

    public void onPlanChange(Runnable listener) {
        planChangeListener = listener;
    }

    public boolean startPlanning() {
        if (planner.isActive() || isBusy() || getStartingRoot() == null) {
            return false;
        }
        dropdownNode = null;
        planner.start(template);
        return true;
    }

    public void stopPlanning() {
        if (planner.isActive()) {
            dropdownNode = null;
            planner.stop();
        }
    }

    public boolean isPlanning() {
        return planner.isActive();
    }

    public String plannedTemplateId() {
        return planner.editedTemplateId();
    }

    public List<TemplateStep> capturePlanSteps() {
        SkillNode root = getStartingRoot();
        return planner.captureSteps(root == null ? null : root.getId());
    }

    public void planSaved(SkillTreeTemplate savedTemplate) {
        this.template = savedTemplate;
        this.templateNodeIds = savedTemplate == null ? Set.of() : savedTemplate.nodeIds();
        planner.templateSaved(savedTemplate);
    }

    private void clickPlannedNode(SkillNode node, boolean ctrlDown) {
        if (allocator.data().isAllocated(node.getId())) {
            SkillTreeSounds.refused();
            return;
        }
        NodeAllocator.Snapshot plan = planner.snapshot();
        if (plan.skillData().isAllocated(node.getId())) {
            if (planner.remove(node)) {
                planChanged(node, false);
            } else if (plan.gate().offersOptions(node)) {
                dropdownNode = node;
            } else {
                SkillTreeSounds.refused();
            }
        } else if (!plan.canAllocate(node)) {
            if (!plan.isHidden(node)) {
                SkillTreeSounds.refused();
            }
        } else if (plan.gate().offersOptions(node)) {
            chooseOption(node, ctrlDown);
        } else if (planner.add(node, null)) {
            planChanged(node, true);
        }
    }

    private void planOptionalNode(SkillNode node, SkillType chosenOption) {
        boolean wasPlanned = planner.isPlannedOnly(node);
        if (wasPlanned && chosenOption.getId().equals(planner.snapshot().skillData().getOptionalSelection(node.getId()))) {
            return;
        }
        if (!(wasPlanned ? planner.switchOption(node, chosenOption) : planner.add(node, chosenOption))) {
            SkillTreeSounds.refused();
            return;
        }
        lastChosenOptionalOption = chosenOption;
        planChanged(node, true);
    }

    private void planChanged(SkillNode node, boolean plannedNow) {
        if (plannedNow) {
            SkillTreeSounds.allocated(allocationSound(node.getType().getTier()));
            feedback.pulse(node);
        } else {
            SkillTreeSounds.deallocated();
        }
        planChangeListener.run();
    }

    public SkillTreeTemplate template() {
        return template;
    }

    public boolean autoAllocate() {
        if (template == null || isBusy() || planner.isActive()) {
            return false;
        }
        dropdownNode = null;
        Set<String> allocatedBefore = allocatedNodeIds();
        lastRunSummary = AutoAllocateRun.run(template.steps(), attemptStep, pointsLeftCheck);
        afterAllocationChange(allocatedBefore, null);
        return true;
    }

    public boolean isAutoAllocateOn() {
        return snapshot().skillData().isAutoAllocatingTemplate();
    }

    public void setAutoAllocate(boolean autoAllocate) {
        allocator.data().setAutoAllocateTemplate(autoAllocate);
    }

    public AutoAllocateRun.Summary takeLastRunSummary() {
        AutoAllocateRun.Summary summary = lastRunSummary;
        lastRunSummary = null;
        return summary;
    }

    public boolean hasPointsLeft() {
        return stepExecutor.hasPointsLeft();
    }

    public void setTargetedSocket(SkillNode socket) {
        targetedSocketId = socket == null ? null : socket.getId();
        if (socket != null) {
            feedback.pulse(socket);
        }
    }

    public String socketRefusal(SkillNode node, Item item) {
        if (node == null || item == null) {
            return null;
        }
        UseVerdict refusal = blockingSocketRefusal(node, item);
        return !refusal.allowed() ? refusal.text(item.name()) : releaseConfirmation.warningForAction(INSTALL_ACTION + node.getId());
    }

    private UseVerdict blockingSocketRefusal(SkillNode node, Item item) {
        if (item instanceof WormholeAnchor anchor) {
            UseVerdict anchorRefusal = anchor.socketRefusal();
            if (anchorRefusal.allowed()) {
                anchorRefusal = WormholeAnchorSocketLimits.refusal(anchor, node.getId(), allocator.data().getSocketedItems());
            }
            if (!anchorRefusal.allowed()) {
                return anchorRefusal;
            }
        }
        return SubroutineSocketLimits.refusal(item, node.getId(), allocator.data().getSocketedItems());
    }

    public boolean installInSocket(SkillNode node, Item item) {
        if (isBusy() || node == null || item == null || !NodeSocketable.fits(item, node)
                || SocketCustody.installations(ShipSkillDataManager.all()).containsKey(item.id())
                || !blockingSocketRefusal(node, item).allowed()
                || !confirmRelease(INSTALL_ACTION, node, planned -> planned.socketItem(node.getId(), item.id()))) {
            return false;
        }
        if (!apply(node, () -> allocator.socketItem(node, item.id()))) {
            return false;
        }
        SkillTreeSounds.socketed();
        feedback.pulse(node);
        return true;
    }

    public boolean emptySocket(SkillNode node) {
        if (isBusy() || node == null || !confirmRelease(EMPTY_ACTION, node, planned -> planned.unsocketItem(node.getId()))) {
            return false;
        }
        return apply(node, () -> allocator.unsocketItem(node));
    }

    public String releaseWarning(SkillNode node) {
        return node == null ? null : releaseConfirmation.warningFor(node.getId());
    }

    private boolean confirmRelease(String action, SkillNode node, Consumer<ShipSkillData> change) {
        String actionKey = action + node.getId();
        if (releaseConfirmation.confirm(actionKey)) {
            return true;
        }
        ShipSkillData skillData = allocator.data();
        Set<String> released = new LinkedHashSet<>(allocator.previewRelease(change));
        released.remove(node.getId());
        String partnerId = ShipTreeLinks.anchorPartner(skillData, node.getId());
        if (partnerId != null) {
            released.remove(partnerId);
        }
        List<String> grantedNodeIds = ShipTreeLinks.grantedNodesBySocket(skillData).getOrDefault(node.getId(), List.of());
        grantedNodeIds.stream().filter(skillData::isGrantFree).forEach(released::remove);
        if (released.isEmpty()) {
            return true;
        }
        int returnedOp = ShipTrees.paidNodeCount(skillData, released) * snapshot().opCostPerNode();
        String warningKey;
        if (holdsDoctrine(skillData, node)) {
            warningKey = "socketable.doctrine.releaseConfirm";
        } else {
            warningKey = partnerId == null && !grantedNodeIds.isEmpty() ? "socketable.cache.releaseConfirm" : "socketable.anchor.closeConfirm";
        }
        String warning = Translation.msg(warningKey).count(released.size()).arg("op", returnedOp).text();
        releaseConfirmation.arm(actionKey, node.getId(), warning);
        if (!INSTALL_ACTION.equals(action)) {
            SkillTreeSounds.refused();
        }
        feedback.pulse(node);
        return false;
    }

    private static boolean holdsDoctrine(ShipSkillData skillData, SkillNode node) {
        return ItemStore.lookup(skillData.getSocketedItem(node.getId())) instanceof Subroutine subroutine
                && subroutine.subroutineKind() == SubroutineKind.DOCTRINE;
    }

    private boolean apply(SkillNode node, BooleanSupplier change) {
        if (isBusy()) {
            return false;
        }
        Set<String> allocatedBefore = allocatedNodeIds();
        if (!change.getAsBoolean()) {
            return false;
        }
        afterAllocationChange(allocatedBefore, node);
        return true;
    }

    private Set<String> allocatedNodeIds() {
        return Set.copyOf(allocator.data().getAllocatedNodeIds());
    }

    private void settleAllocation() {
        allocator.openAnchorWormholes();
        allocator.grantSocketedNodes();
        allocationSnapshot = null;
    }

    private void refreshAfterAllocation() {
        settleAllocation();
        if (refitButton != null) {
            refitButton.refreshVariant();
        }
    }

    private void afterAllocationChange(Set<String> allocatedBefore, SkillNode origin) {
        refreshAfterAllocation();
        Set<String> allocatedNow = allocator.data().getAllocatedNodeIds();
        Set<String> released = new LinkedHashSet<>(allocatedBefore);
        released.removeAll(allocatedNow);
        Set<String> added = new LinkedHashSet<>(allocatedNow);
        added.removeAll(allocatedBefore);
        String originId = origin == null ? null : origin.getId();
        if (!released.isEmpty()) {
            feedback.deallocated(originId, released);
        }
        if (!added.isEmpty()) {
            feedback.allocated(originId, added);
        }
    }
}
