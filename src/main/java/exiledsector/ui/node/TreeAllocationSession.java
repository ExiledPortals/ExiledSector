package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocationGate;
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
import exiledsector.ui.SkillTreeSounds;
import lunalib.lunaRefit.BaseRefitButton;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TreeAllocationSession {

    private final BaseRefitButton refitButton;
    private final NodeSearch nodeSearch;
    private final NodeAllocator allocator;
    private final TemplateStepExecutor stepExecutor;
    private final Function<TemplateStep, StepVerdict> attemptStep;
    private final BooleanSupplier pointsLeftCheck;
    private final Predicate<SkillNode> respecStep = this::removeForRespec;
    private final Predicate<SkillType> dropdownOptionUsable = this::isDropdownOptionUsable;

    private TreeFeedback feedback = TreeFeedback.NONE;
    private StartingRootChoice rootChoice;
    private NodeAllocator.Snapshot allocationSnapshot;
    private SkillType lastChosenOptionalOption;
    private SkillNode dropdownNode;
    private String targetedSocketId;
    private SkillTreeTemplate template;
    private Set<String> templateNodeIds = Set.of();
    private AutoAllocateRun autoAllocateRun;
    private AutoAllocateRun.Summary lastRunSummary;
    private RespecRun respecRun;

    public TreeAllocationSession(FleetMemberAPI member, ShipVariantAPI variant, BaseRefitButton refitButton, NodeSearch nodeSearch) {
        this.refitButton = refitButton;
        this.nodeSearch = nodeSearch;
        this.rootChoice = initialRootChoice(ShipSkillDataManager.get(member.getId()));
        this.allocator = new NodeAllocator(member, variant, () -> rootChoice.rootForAllocation());
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

    Set<String> templateNodeIds() {
        return templateNodeIds;
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
        rootChoice.advance(amount);
        advanceAutoAllocate(amount);
        advanceRespec(amount);
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
        feedback.rootChanged(root);
        afterAllocationChange(root, true);
    }

    private void unchooseStartingRoot(SkillNode root) {
        if (!allocator.unchooseStartingRoot(root)) {
            return;
        }
        dropdownNode = null;
        setTemplate(null);
        nodeSearch.setQuery("");
        rootChoice = StartingRootChoice.returning(SkillTree.topology().roots(), root);
        feedback.rootChanged(null);
        afterAllocationChange(root, false);
    }

    public boolean isAllocated(SkillNode node) {
        return allocator.data().isAllocated(node.getId());
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
        return snapshot().skillData().getAllocatedNodeIds().size();
    }

    public List<TemplateStep> captureTemplateSteps() {
        SkillNode root = getStartingRoot();
        return TemplateCapture.capture(snapshot().skillData(), root == null ? null : root.getId(), SkillTree.getAllNodes());
    }

    public boolean isBusy() {
        return isStartingRootInputLocked() || autoAllocateRun != null || respecRun != null;
    }

    public void clickNode(SkillNode node, boolean ctrlDown) {
        if (isBusy()) {
            return;
        }
        if (allocator.canUnchooseStartingRoot(node)) {
            unchooseStartingRoot(node);
            return;
        }
        NodeAllocator.Snapshot allocation = snapshot();
        if (allocation.skillData().isAllocated(node.getId())) {
            removeOrSwitchOption(node, allocation);
        } else if (!allocation.canAllocate(node)) {
            if (!allocation.isHidden(node)) {
                SkillTreeSounds.refused();
            }
        } else if (node.getType().isOptional()) {
            chooseOption(node, ctrlDown);
        } else if (allocator.allocate(node, null, allocation)) {
            afterAllocationChange(node, true);
        }
    }

    private void removeOrSwitchOption(SkillNode node, NodeAllocator.Snapshot allocation) {
        if (allocator.deallocate(node, allocation)) {
            afterAllocationChange(node, false);
        } else if (node.getType().isOptional()) {
            dropdownNode = node;
        } else {
            SkillTreeSounds.refused();
        }
    }

    public SkillNode wormholeJumpTarget(SkillNode node, boolean ctrlDown) {
        if (!ctrlDown || node.getType().getTier() != SkillTier.WORMHOLE) {
            return null;
        }
        String pairedId = node.getPairedNodeId();
        SkillNode paired = pairedId == null ? null : SkillTree.get(pairedId);
        if (paired == null) {
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
        return snapshot().gate().allocation(node, lastChosenOptionalOption).allowed() ? lastChosenOptionalOption : null;
    }

    private boolean isDropdownOptionUsable(SkillType option) {
        if (dropdownNode == null) {
            return false;
        }
        AllocationGate.Verdict verdict = optionVerdict(dropdownNode, option, snapshot());
        return verdict.allowed() || verdict.refusal() == AllocationGate.Refusal.SAME_OPTION;
    }

    String dropdownOptionRefusalReason(SkillType option) {
        return dropdownNode == null ? null : snapshot().optionRefusalReason(dropdownNode, option);
    }

    private static AllocationGate.Verdict optionVerdict(SkillNode node, SkillType option, NodeAllocator.Snapshot allocation) {
        return allocation.skillData().isAllocated(node.getId())
                ? allocation.gate().optionSwitch(node, option) : allocation.gate().allocation(node, option);
    }

    private void allocateOptionalNode(SkillNode node, SkillType chosenOption) {
        NodeAllocator.Snapshot allocation = snapshot();
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
            SkillTreeSounds.allocated(node.getType().getTier());
            feedback.pulse(node);
        } else {
            afterAllocationChange(node, true);
        }
    }

    public boolean startRespec(SkillNode node) {
        if (isBusy()) {
            return false;
        }
        List<SkillNode> respecPlan = allocator.respecPlan(node);
        if (respecPlan.isEmpty()) {
            return false;
        }
        dropdownNode = null;
        respecRun = new RespecRun(respecPlan);
        return true;
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

    private void advanceRespec(float amount) {
        if (respecRun == null || rootChoice.isInputLocked()) {
            return;
        }
        respecRun.advance(amount, respecStep);
        if (respecRun.isFinished()) {
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
        if (!allocator.deallocate(node, snapshot())) {
            return false;
        }
        afterAllocationChange(node, false);
        return true;
    }

    public void setTemplate(SkillTreeTemplate newTemplate) {
        cancelAutoAllocate();
        this.template = newTemplate;
        this.templateNodeIds = newTemplate == null ? Set.of() : newTemplate.nodeIds();
    }

    public SkillTreeTemplate template() {
        return template;
    }

    public boolean startAutoAllocate() {
        if (template == null || isBusy()) {
            return false;
        }
        dropdownNode = null;
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

    public boolean installInSocket(SkillNode node, Socketable socketable) {
        if (isBusy() || !socketable.canSocketInto(node)
                || SocketCustody.installations(ShipSkillDataManager.all()).containsKey(socketable.id())
                || !allocator.socketItem(node, socketable.id())) {
            return false;
        }
        refreshAfterAllocation();
        SkillTreeSounds.socketed();
        feedback.pulse(node);
        return true;
    }

    public boolean emptySocket(SkillNode node) {
        if (isBusy() || !allocator.unsocketItem(node)) {
            return false;
        }
        refreshAfterAllocation();
        return true;
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
            feedback.allocated(node);
        } else {
            SkillTreeSounds.deallocated();
            feedback.deallocated(node);
        }
    }
}
