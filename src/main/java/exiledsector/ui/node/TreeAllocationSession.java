package exiledsector.ui.node;

import exiledsector.i18n.Translation;
import exiledsector.items.ShipSection;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
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
import exiledsector.items.HullUpgradeRules;
import exiledsector.items.SocketCustody;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.Subroutine;
import exiledsector.items.SubroutineRadius;
import exiledsector.items.SubroutineSocketLimits;
import exiledsector.items.VolumeGrantLinks;
import exiledsector.items.WormholeAnchor;
import exiledsector.items.WormholeAnchorLinks;
import exiledsector.items.WormholeAnchorSocketLimits;
import exiledsector.i18n.Translation;
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
    private final Predicate<SkillNode> respecStep = this::removeForRespec;
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
    private AutoAllocateRun autoAllocateRun;
    private AutoAllocateRun.Summary lastRunSummary;
    private RespecRun respecRun;
    private Runnable planChangeListener = () -> {
    };

    public TreeAllocationSession(FleetMemberAPI member, ShipVariantAPI variant, BaseRefitButton refitButton, NodeSearch nodeSearch) {
        this.refitButton = refitButton;
        this.nodeSearch = nodeSearch;
        this.rootChoice = initialRootChoice(ShipSkillDataManager.get(member.getId()));
        this.allocator = new NodeAllocator(member, variant, () -> rootChoice.rootForAllocation());
        this.stepExecutor = new TemplateStepExecutor(allocator, this::snapshot, node -> afterAllocationChange(node, true));
        this.attemptStep = stepExecutor::attempt;
        this.pointsLeftCheck = stepExecutor::hasPointsLeft;
        this.planner = new TemplatePlanner(allocator, this::snapshot);
        WormholeAnchorLinks.refresh();
        VolumeGrantLinks.refresh();
        allocator.openAnchorWormholes();
        allocator.grantVolumeNotables();
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
        if (allocator.applyPendingPhantomConflicts()) {
            refreshAfterAllocation();
            SkillTreeSounds.refused();
        }
        rootChoice.advance(amount);
        releaseConfirmation.advance(amount);
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
        stopPlanning();
        setTemplate(null);
        nodeSearch.setQuery("");
        rootChoice = StartingRootChoice.returning(SkillTree.topology().roots(), root);
        feedback.rootChanged(null);
        afterAllocationChange(root, false);
    }

    public boolean isAllocated(SkillNode node) {
        return allocator.data().isAllocated(node.getId());
    }

    public boolean isHidden(SkillNode node) {
        return node != null && viewSnapshot().isHidden(node);
    }

    String wormholePartner(SkillNode node) {
        return allocator.data().wormholePartner(node);
    }

    public List<SkillTreeTopology.WormholePair> wormholePairs() {
        return viewSnapshot().skillData().wormholePairs(SkillTree.topology());
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
        if (planner.isActive() && !isStartingRootInputLocked()) {
            clickPlannedNode(node, ctrlDown);
            return;
        }
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
        boolean anchorLinked = allocation.skillData().anchorPartner(node.getId()) != null;
        boolean holdsGrant = allocation.skillData().grantedNodesBySocket().containsKey(node.getId());
        if ((anchorLinked || holdsGrant) && allocation.gate().deallocation(node).allowed()
                && !confirmRelease(DEALLOCATE_ACTION, node, planned -> planned.deallocate(node))) {
            return;
        }
        Set<String> allocatedBefore = Set.copyOf(allocation.skillData().getAllocatedNodeIds());
        if (allocator.deallocate(node, allocation)) {
            afterAllocationChange(node, false);
            reportReleased(allocatedBefore, node);
        } else if (node.getType().isOptional()) {
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
        boolean closedAnchorWormhole = liveData.anchorPartner(node.getId()) != null
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
        if (isBusy() || planner.isActive()) {
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
            } else if (node.getType().isOptional()) {
                dropdownNode = node;
            } else {
                SkillTreeSounds.refused();
            }
        } else if (!plan.canAllocate(node)) {
            if (!plan.isHidden(node)) {
                SkillTreeSounds.refused();
            }
        } else if (node.getType().isOptional()) {
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
            SkillTreeSounds.allocated(node.getType().getTier());
            feedback.pulse(node);
        } else {
            SkillTreeSounds.deallocated();
        }
        planChangeListener.run();
    }

    public SkillTreeTemplate template() {
        return template;
    }

    public boolean startAutoAllocate() {
        if (template == null || isBusy() || planner.isActive()) {
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

    public boolean isAutoAllocateOn() {
        return snapshot().skillData().isAutoAllocatingTemplate();
    }

    public void setAutoAllocate(boolean autoAllocate) {
        allocator.data().setAutoAllocateTemplate(autoAllocate);
        if (!autoAllocate) {
            cancelAutoAllocate();
        }
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

    public String socketRefusal(SkillNode node, Item item) {
        if (node == null || item == null) {
            return null;
        }
        String refusal = blockingSocketRefusal(node, item);
        return refusal != null ? refusal : releaseConfirmation.warningForAction(INSTALL_ACTION + node.getId());
    }

    private String blockingSocketRefusal(SkillNode node, Item item) {
        if (item instanceof WormholeAnchor anchor) {
            String anchorRefusal = anchor.socketRefusal();
            if (anchorRefusal == null) {
                anchorRefusal = WormholeAnchorSocketLimits.refusal(anchor, node.getId(), allocator.data().getSocketedItems());
            }
            if (anchorRefusal != null) {
                return anchorRefusal;
            }
        }
        if (item instanceof Subroutine subroutine && subroutine.volumeSocketRefusal() != null) {
            return subroutine.volumeSocketRefusal();
        }
        return SubroutineSocketLimits.refusal(item, node.getId(), allocator.data().getSocketedItems());
    }

    public boolean installInSocket(SkillNode node, Item item) {
        if (isBusy() || node == null || item == null || !item.canSocketInto(node)
                || SocketCustody.installations(ShipSkillDataManager.all()).containsKey(item.id())
                || blockingSocketRefusal(node, item) != null
                || !confirmRelease(INSTALL_ACTION, node, planned -> planned.socketItem(node.getId(), item.id()))) {
            return false;
        }
        Set<String> allocatedBefore = Set.copyOf(allocator.data().getAllocatedNodeIds());
        return apply(() -> allocator.socketItem(node, item.id()), () -> {
            SkillTreeSounds.socketed();
            feedback.pulse(node);
            reportReleased(allocatedBefore, node);
        });
    }

    public boolean emptySocket(SkillNode node) {
        if (isBusy() || node == null || !confirmRelease(EMPTY_ACTION, node, planned -> planned.unsocketItem(node.getId()))) {
            return false;
        }
        Set<String> allocatedBefore = Set.copyOf(allocator.data().getAllocatedNodeIds());
        return apply(() -> allocator.unsocketItem(node), () -> reportReleased(allocatedBefore, node));
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
        String partnerId = skillData.anchorPartner(node.getId());
        if (partnerId != null) {
            released.remove(partnerId);
        }
        if (released.isEmpty()) {
            return true;
        }
        int returnedOp = skillData.paidNodeCount(released) * snapshot().opCostPerNode();
        String grantedNodeId = skillData.grantedNodesBySocket().get(node.getId());
        boolean onlyGrantReleased = grantedNodeId != null && released.equals(Set.of(grantedNodeId));
        if (onlyGrantReleased && returnedOp == 0) {
            return true;
        }
        String warning = onlyGrantReleased ? volumeReleaseWarning(grantedNodeId, returnedOp)
                : Translation.msg("socketable.anchor.closeConfirm").count(released.size()).arg("op", returnedOp).text();
        releaseConfirmation.arm(actionKey, node.getId(), warning);
        if (!INSTALL_ACTION.equals(action)) {
            SkillTreeSounds.refused();
        }
        feedback.pulse(node);
        return false;
    }

    private static String volumeReleaseWarning(String grantedNodeId, int returnedOp) {
        SkillNode grantedNode = SkillTree.getDeclared(grantedNodeId);
        String nodeName = grantedNode == null ? grantedNodeId : grantedNode.getDisplayName();
        return Translation.msg("socketable.volume.releaseConfirm").arg("node", nodeName).arg("op", returnedOp).text();
    }

    private void reportReleased(Set<String> allocatedBefore, SkillNode node) {
        Set<String> released = new LinkedHashSet<>(allocatedBefore);
        released.removeAll(allocator.data().getAllocatedNodeIds());
        released.remove(node.getId());
        if (!released.isEmpty()) {
            feedback.released(released);
        }
    }

    public List<ShipSections.UnlockedSection> sections() {
        return allocator.sections();
    }

    public int hullUpgradePoints() {
        return allocator.data().getHullUpgradePoints();
    }

    public int unspentHullUpgradePoints() {
        return allocator.data().getUnspentHullUpgradePoints();
    }

    public boolean isSectionUnlocked(ShipSection section) {
        return allocator.data().isSocketTypeUnlocked(section.id());
    }

    public HullSize hullSize() {
        return allocator.hullSize();
    }

    public HullUpgradeRules.UnlockBlock unlockBlock(ShipSection section) {
        return allocator.unlockBlock(section);
    }

    public String unmetRequirement(ShipSection section) {
        return allocator.unmetRequirement(section);
    }

    public boolean installUpgrade() {
        return applySectionChange(allocator::installUpgrade, SkillTreeSounds::socketed);
    }

    public boolean unlockSection(ShipSection section) {
        return applySectionChange(() -> allocator.unlockSection(section), SkillTreeSounds::socketed);
    }

    public boolean lockSection(ShipSection section) {
        return applySectionChange(() -> allocator.lockSection(section), () -> { });
    }

    public boolean installInSection(ShipSection section, Item item) {
        return applySectionChange(() -> allocator.installInSection(section, item), SkillTreeSounds::socketed);
    }

    public boolean removeFromSection(ShipSection section) {
        return applySectionChange(() -> allocator.removeFromSection(section), () -> { });
    }

    private boolean apply(BooleanSupplier change, Runnable afterChange) {
        return !isBusy() && applyChange(change, afterChange);
    }

    private boolean applySectionChange(BooleanSupplier change, Runnable afterChange) {
        return autoAllocateRun == null && respecRun == null && applyChange(change, afterChange);
    }

    private boolean applyChange(BooleanSupplier change, Runnable afterChange) {
        if (!change.getAsBoolean()) {
            return false;
        }
        refreshAfterAllocation();
        afterChange.run();
        return true;
    }

    private void refreshAfterAllocation() {
        allocator.openAnchorWormholes();
        allocator.grantVolumeNotables();
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
