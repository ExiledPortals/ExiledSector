package exiledsector.ui.node;

import com.fs.starfarer.api.Global;
import exiledsector.i18n.Translation;
import exiledsector.items.NodeSocketable;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.PhantomConflictWatch;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocationCheck;
import exiledsector.skills.AllocationPath;
import exiledsector.skills.AnchorRoute;
import exiledsector.skills.IntegratedNodes;
import exiledsector.skills.IntegrationLines;
import exiledsector.skills.NodeIntegration;
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
import exiledsector.skills.template.TemplateAnchors;
import exiledsector.skills.template.TemplateBridge;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private final Predicate<SkillType> choicePickerOptionUsable = this::isChoicePickerOptionUsable;
    private final TemplatePlanner planner;
    private final ReleaseConfirmation releaseConfirmation = new ReleaseConfirmation();
    private final IntegrationHold integrationHold = new IntegrationHold();
    private final IntegrationHold integrationMarkHold = new IntegrationHold(IntegrationHold.MARK_HOLD_SECONDS);
    private final Random pathRandom = new Random();

    private TreeFeedback feedback = TreeFeedback.NONE;
    private StartingRootChoice rootChoice;
    private NodeAllocator.Snapshot allocationSnapshot;
    private SkillType lastChosenOption;
    private SkillNode choicePickerNode;
    private String targetedSocketId;
    private SkillTreeTemplate template;
    private Set<String> templateNodeIds = Set.of();
    private List<SkillType> templateSelections = List.of();
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

    SkillNode choicePickerNode() {
        return choicePickerNode;
    }

    Predicate<SkillType> choicePickerOptionUsable() {
        return choicePickerOptionUsable;
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
        advanceIntegrationHold(amount);
        advanceIntegrationMarkHold(amount);
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
        choicePickerNode = null;
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

    public boolean isConcealed(SkillNode node) {
        return node != null && viewSnapshot().isConcealed(node);
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

    public Map<String, String> captureTemplateAnchors() {
        return TemplateAnchors.socketedOn(snapshot().skillData());
    }

    public Set<String> captureTemplateIntegrationMarks() {
        return snapshot().skillData().getIntegratedNodeIds();
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
            choicePickerNode = node;
        } else {
            SkillTreeSounds.refused();
        }
    }

    public SkillNode wormholeJumpTarget(SkillNode node) {
        if (node == null) {
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

    public boolean isChoicePickerOpen() {
        return choicePickerNode != null;
    }

    public void closeChoicePicker() {
        choicePickerNode = null;
    }

    public void commitChoicePickerSelection(SkillType chosenOption) {
        SkillNode node = choicePickerNode;
        choicePickerNode = null;
        if (node != null) {
            allocateChoiceNode(node, chosenOption);
        }
    }

    private void chooseOption(SkillNode node, boolean ctrlDown) {
        SkillType repeatedOption = ctrlDown ? repeatableOptionFor(node) : null;
        if (repeatedOption != null) {
            allocateChoiceNode(node, repeatedOption);
        } else {
            choicePickerNode = node;
        }
    }

    private SkillType repeatableOptionFor(SkillNode node) {
        if (lastChosenOption == null || !node.getType().getChoiceOptionIds().contains(lastChosenOption.getId())) {
            return null;
        }
        return viewSnapshot().gate().allocation(node, lastChosenOption).allowed() ? lastChosenOption : null;
    }

    private boolean isChoicePickerOptionUsable(SkillType option) {
        if (choicePickerNode == null) {
            return false;
        }
        AllocationCheck.Verdict verdict = optionVerdict(choicePickerNode, option, viewSnapshot());
        return verdict.allowed() || verdict.refusal() == AllocationCheck.Refusal.SAME_OPTION;
    }

    String choicePickerOptionRefusalReason(SkillType option) {
        return choicePickerNode == null ? null : viewSnapshot().optionRefusalReason(choicePickerNode, option);
    }

    static AllocationCheck.Verdict optionVerdict(SkillNode node, SkillType option, NodeAllocator.Snapshot allocation) {
        return allocation.skillData().isAllocated(node.getId())
                ? allocation.gate().optionSwitch(node, option) : allocation.gate().allocation(node, option);
    }

    private void allocateChoiceNode(SkillNode node, SkillType chosenOption) {
        if (planner.isActive()) {
            plaChoiceNode(node, chosenOption);
            return;
        }
        NodeAllocator.Snapshot allocation = snapshot();
        Set<String> allocatedBefore = allocatedNodeIds();
        boolean wasAllocated = allocation.skillData().isAllocated(node.getId());
        if (optionVerdict(node, chosenOption, allocation).refusal() == AllocationCheck.Refusal.SAME_OPTION) {
            return;
        }
        boolean changed = wasAllocated ? allocator.switchOption(node, chosenOption, allocation) : allocator.allocate(node, chosenOption, allocation);
        if (!changed) {
            SkillTreeSounds.refused();
            return;
        }
        lastChosenOption = chosenOption;
        if (wasAllocated) {
            refreshAfterAllocation();
            SkillTreeSounds.allocated(allocationSound(node.getType().getTier()));
            feedback.pulse(node);
        } else {
            afterAllocationChange(allocatedBefore, node);
        }
    }

    public boolean allocatePath(SkillNode target) {
        if (isBusy() || getStartingRoot() == null) {
            return false;
        }
        NodeAllocator.Snapshot view = viewSnapshot();
        List<AllocationPath.Step> path = AllocationPath.to(target, view.gate(), SkillTree.topology(), view::isHidden, pathChoices(), pathRandom);
        if (path.isEmpty()) {
            return false;
        }
        choicePickerNode = null;
        if (planner.isActive()) {
            planPath(target, path);
        } else {
            allocateLivePath(target, path);
        }
        return true;
    }

    private void allocateLivePath(SkillNode target, List<AllocationPath.Step> path) {
        Set<String> allocatedBefore = allocatedNodeIds();
        if (allocateSteps(path) == 0) {
            SkillTreeSounds.refused();
            return;
        }
        afterAllocationChange(allocatedBefore, target);
    }

    private int allocateSteps(List<AllocationPath.Step> path) {
        int allocatedCount = 0;
        for (AllocationPath.Step step : path) {
            if (!allocator.allocate(step.node(), step.option(), snapshot())) {
                break;
            }
            settleAllocation();
            allocatedCount++;
        }
        return allocatedCount;
    }

    private List<AllocationPath.Step> livePathTo(SkillNode target) {
        NodeAllocator.Snapshot live = snapshot();
        return AllocationPath.to(target, live.gate(), SkillTree.topology(), live::isHidden, pathChoices(), pathRandom);
    }

    private AllocationPath.Choices pathChoices() {
        return new AllocationPath.Choices(lastChosenOption, templateSelections);
    }

    public boolean allocateAnchoredPath(SkillNode target) {
        if (isBusy() || planner.isActive() || getStartingRoot() == null) {
            return false;
        }
        NodeAllocator.Snapshot live = snapshot();
        AnchorRoute.Route route = AnchorRoute.cheapest(target, openableAnchors(), this::acceptsAnchor, live.gate(), SkillTree.topology(), live::isHidden);
        if (route == null) {
            return false;
        }
        choicePickerNode = null;
        Set<String> allocatedBefore = allocatedNodeIds();
        SkillNode socket = route.socket();
        allocateSteps(livePathTo(socket));
        if (isAllocated(socket) && allocator.socketItem(socket, route.anchor().itemId())) {
            settleAllocation();
            SkillTreeSounds.socketed();
            feedback.pulse(socket);
            allocateSteps(livePathTo(target));
        } else if (allocatedBefore.equals(allocatedNodeIds())) {
            SkillTreeSounds.refused();
            return true;
        }
        afterAllocationChange(allocatedBefore, target);
        return true;
    }

    private static List<AnchorRoute.Anchor> openableAnchors() {
        List<AnchorRoute.Anchor> anchors = new ArrayList<>();
        for (WormholeAnchor anchor : WormholeAnchor.unusedInStorage()) {
            String exitNodeId = anchor.linkedExitNodeId();
            if (exitNodeId != null && anchor.socketRefusal().allowed() && anchor.canOpen(Global.getSector())) {
                anchors.add(new AnchorRoute.Anchor(anchor.id(), exitNodeId));
            }
        }
        return anchors;
    }

    private boolean acceptsAnchor(SkillNode socket, AnchorRoute.Anchor anchor) {
        Item item = ItemStore.lookup(anchor.itemId());
        return NodeSocketable.fits(item, socket) && blockingSocketRefusal(socket, item).allowed();
    }

    private void planPath(SkillNode target, List<AllocationPath.Step> path) {
        boolean plannedAny = false;
        for (AllocationPath.Step step : path) {
            if (!planner.add(step.node(), step.option())) {
                break;
            }
            plannedAny = true;
        }
        if (plannedAny) {
            planChanged(target, true);
        } else {
            SkillTreeSounds.refused();
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
        choicePickerNode = null;
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
        useTemplate(newTemplate);
        if (planner.isActive()) {
            choicePickerNode = null;
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
        choicePickerNode = null;
        planner.start(template);
        return true;
    }

    public void stopPlanning() {
        if (planner.isActive()) {
            choicePickerNode = null;
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
        useTemplate(savedTemplate);
        planner.templateSaved(savedTemplate);
    }

    private void useTemplate(SkillTreeTemplate newTemplate) {
        this.template = newTemplate;
        this.templateNodeIds = newTemplate == null ? Set.of() : newTemplate.nodeIds(allocator.satisfiedRootId());
        this.templateSelections = newTemplate == null ? List.of() : TemplateBridge.selections(newTemplate.steps());
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
                choicePickerNode = node;
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

    private void plaChoiceNode(SkillNode node, SkillType chosenOption) {
        boolean wasPlanned = planner.isPlannedOnly(node);
        if (wasPlanned && chosenOption.getId().equals(planner.snapshot().skillData().getChoiceSelection(node.getId()))) {
            return;
        }
        if (!(wasPlanned ? planner.switchOption(node, chosenOption) : planner.add(node, chosenOption))) {
            SkillTreeSounds.refused();
            return;
        }
        lastChosenOption = chosenOption;
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
        choicePickerNode = null;
        Set<String> allocatedBefore = allocatedNodeIds();
        TemplateAnchors anchors = TemplateAnchors.fromStorage(template, (socket, anchor) -> {
            if (!allocator.socketItem(socket, anchor.id())) {
                return false;
            }
            settleAllocation();
            return true;
        });
        anchors.fill(allocator.data());
        lastRunSummary = AutoAllocateRun.run(template.steps(), anchors.fillingAfter(attemptStep, allocator.data()), rootBridge(), pointsLeftCheck);
        afterAllocationChange(allocatedBefore, null);
        return true;
    }

    private AutoAllocateRun.Bridge rootBridge() {
        if (!TemplateBridge.crossesRoots(template, allocator.satisfiedRootId())) {
            return AutoAllocateRun.Bridge.NONE;
        }
        TemplateBridge templateBridge = new TemplateBridge(template.steps());
        return unreachedSteps -> {
            NodeAllocator.Snapshot live = snapshot();
            AllocationPath.Step step = templateBridge.nextStep(unreachedSteps, live.gate(), SkillTree.topology(), live::isHidden, pathRandom);
            if (step == null || !allocator.allocate(step.node(), step.option(), live)) {
                return false;
            }
            settleAllocation();
            return true;
        };
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

    public void beginIntegrationHold(SkillNode node) {
        if (canIntegrate(node)) {
            integrationHold.begin(node.getId(), NodeIntegration.availableStoryPoints() >= IntegratedNodes.nextCost(allocator.data()));
        } else if (canToggleIntegrationMark(node)) {
            integrationMarkHold.begin(node.getId(), true);
        }
    }

    public boolean releaseIntegrationHold() {
        boolean integrationClaimed = integrationHold.release();
        boolean markClaimed = integrationMarkHold.release();
        return integrationClaimed || markClaimed;
    }

    public void cancelIntegrationHold() {
        integrationHold.cancel();
        integrationMarkHold.cancel();
    }

    public float integrationHoldProgress(SkillNode node) {
        return integrationHold.progressFor(node.getId());
    }

    float integrationMarkHoldProgress(SkillNode node) {
        return integrationMarkHold.progressFor(node.getId());
    }

    Set<String> integrationMarks() {
        if (planner.isActive()) {
            return planner.integrationMarks();
        }
        return template == null ? Set.of() : template.integrationMarks();
    }

    public Set<String> capturePlanIntegrationMarks() {
        return planner.captureIntegrationMarks();
    }

    IntegrationLines.TemplateMark templateMark(SkillNode node) {
        boolean marked = integrationMarks().contains(node.getId());
        if (!planner.isActive()) {
            return marked ? IntegrationLines.TemplateMark.MARKED : IntegrationLines.TemplateMark.NONE;
        }
        if (!planner.canToggleIntegrationMark(node)) {
            return IntegrationLines.TemplateMark.NONE;
        }
        return marked ? IntegrationLines.TemplateMark.UNMARKABLE : IntegrationLines.TemplateMark.MARKABLE;
    }

    private boolean canToggleIntegrationMark(SkillNode node) {
        return planner.isActive() && !isBusy() && planner.canToggleIntegrationMark(node);
    }

    private void advanceIntegrationMarkHold(float amount) {
        SkillNode heldNode = SkillTree.get(integrationMarkHold.heldNodeId());
        if (integrationMarkHold.advance(amount) != IntegrationHold.Step.COMPLETED || heldNode == null) {
            return;
        }
        if (!canToggleIntegrationMark(heldNode) || !planner.toggleIntegrationMark(heldNode)) {
            SkillTreeSounds.refused();
            return;
        }
        if (planner.integrationMarks().contains(heldNode.getId())) {
            SkillTreeSounds.allocated(allocationSound(heldNode.getType().getTier()));
            feedback.pulse(heldNode);
        } else {
            SkillTreeSounds.deallocated();
        }
        planChangeListener.run();
    }

    private boolean canIntegrate(SkillNode node) {
        return !planner.isActive() && !isBusy() && IntegratedNodes.canIntegrate(allocator.data(), node);
    }

    private void advanceIntegrationHold(float amount) {
        SkillNode heldNode = SkillTree.get(integrationHold.heldNodeId());
        IntegrationHold.Step step = integrationHold.advance(amount);
        if (heldNode == null) {
            return;
        }
        if (step == IntegrationHold.Step.CLAIMED) {
            warnIntegration(heldNode);
        } else if (step == IntegrationHold.Step.COMPLETED) {
            integrate(heldNode);
        }
    }

    private void warnIntegration(SkillNode node) {
        int cost = IntegratedNodes.nextCost(allocator.data());
        if (integrationHold.isAffordable()) {
            integrationHold.warn(Translation.msg("node.integration.confirm").count(cost).text());
        } else {
            integrationHold.warn(Translation.msg("node.integration.notEnough").count(cost).text());
            SkillTreeSounds.refused();
        }
        feedback.pulse(node);
    }

    private void integrate(SkillNode node) {
        ShipSkillData skillData = allocator.data();
        FleetMemberAPI member = allocator.member();
        if (!canIntegrate(node) || !NodeIntegration.integrate(skillData, node, member.getShipName())) {
            SkillTreeSounds.refused();
            return;
        }
        refreshAfterAllocation();
        SkillTreeSounds.integrated();
        feedback.integrated(node);
    }

    public void refreshAfterIntegrationReset() {
        cancelIntegrationHold();
        planner.clearIntegrations();
        refreshAfterAllocation();
    }

    public String releaseWarning(SkillNode node) {
        if (node == null) {
            return null;
        }
        String integrationWarning = integrationHold.warningFor(node.getId());
        return integrationWarning != null ? integrationWarning : releaseConfirmation.warningFor(node.getId());
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
            warningKey = partnerId == null && !grantedNodeIds.isEmpty() ? "subroutine.grant.releaseConfirm" : "socketable.anchor.closeConfirm";
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
