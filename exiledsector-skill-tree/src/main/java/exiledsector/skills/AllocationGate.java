package exiledsector.skills;

import exiledsector.items.DoctrineOverrides;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AllocationGate {

    public enum Refusal {
        ALREADY_ALLOCATED, NOT_ALLOCATED, NOT_CONNECTED, NODE_CAP, OUT_OF_OP, INELIGIBLE, STARTING_ROOT, STRANDS_NODES, SAME_OPTION,
        HELD_BY_SUBROUTINE
    }

    public record Verdict(Refusal refusal, NodeEligibility.Block block) {

        public static final Verdict GRANTED = new Verdict(null, null);

        static Verdict refused(Refusal refusal) {
            return new Verdict(refusal, null);
        }

        static Verdict fromBlock(NodeEligibility.Block block) {
            return block == null ? GRANTED : new Verdict(Refusal.INELIGIBLE, block);
        }

        public boolean allowed() {
            return refusal == null;
        }

        public boolean worthExplaining() {
            return refusal == Refusal.NODE_CAP || refusal == Refusal.OUT_OF_OP || refusal == Refusal.INELIGIBLE;
        }
    }

    public record Budget(int totalOp, int opCostPerNode, int maxAllocatedNodes) {
    }

    private static final class NodeVerdicts {
        private Verdict allocation;
        private Verdict deallocation;
        private Map<String, Verdict> optionAllocations;
        private Map<String, Verdict> optionEligibilities;
        private Map<String, Verdict> optionSwitches;
    }

    private final ShipSkillData data;
    private final SkillTreeTopology topology;
    private final String rootId;
    private final Budget budget;
    private final NodeEligibility.Context eligibility;
    private final int spentOp;
    private final Map<String, NodeVerdicts> verdictsByNodeId = new HashMap<>();
    private final Map<String, Verdict> grantVerdictsByNodeId = new HashMap<>();
    private Set<String> grantOnlyNodeIds;

    public AllocationGate(ShipSkillData data, SkillTreeTopology topology, String rootId, Budget budget, NodeEligibility.Context eligibility) {
        this.data = data;
        this.topology = topology;
        this.rootId = rootId;
        this.budget = budget;
        this.eligibility = eligibility;
        this.spentOp = ShipTrees.getSpentOp(data, budget.opCostPerNode());
    }

    public ShipSkillData data() {
        return data;
    }

    public String rootId() {
        return rootId;
    }

    public Budget budget() {
        return budget;
    }

    public int spentOp() {
        return spentOp;
    }

    public int freeOp() {
        return Math.max(0, budget.totalOp() - spentOp);
    }

    public int opCostFor(SkillNode node) {
        return node.getId().equals(rootId) ? 0 : budget.opCostPerNode();
    }

    public Verdict allocation(SkillNode node) {
        NodeVerdicts verdicts = verdictsFor(node);
        if (verdicts.allocation == null) {
            verdicts.allocation = computeAllocation(node);
        }
        return verdicts.allocation;
    }

    public Verdict allocation(SkillNode node, SkillType option) {
        if (option == null && !offersOptions(node)) {
            return allocation(node);
        }
        NodeVerdicts verdicts = verdictsFor(node);
        if (verdicts.optionAllocations == null) {
            verdicts.optionAllocations = new HashMap<>();
        }
        return verdicts.optionAllocations.computeIfAbsent(optionKey(option), key -> {
            Verdict placement = placement(node);
            return placement.allowed() ? eligibility(node, option) : placement;
        });
    }

    public Verdict eligibility(SkillNode node, SkillType option) {
        NodeVerdicts verdicts = verdictsFor(node);
        if (verdicts.optionEligibilities == null) {
            verdicts.optionEligibilities = new HashMap<>();
        }
        return verdicts.optionEligibilities.computeIfAbsent(optionKey(option),
                key -> Verdict.fromBlock(NodeEligibility.check(checkedNode(node), option, eligibility)));
    }

    public boolean offersOptions(SkillNode node) {
        return node.offersOptions(data);
    }

    public SkillItemCost itemCost(SkillNode node, SkillType option) {
        return NodeEligibility.itemCost(checkedNode(node), option);
    }

    public Verdict grant(SkillNode node) {
        return grantVerdictsByNodeId.computeIfAbsent(node.getId(), id -> {
            if (data.isAllocated(node.getId())) {
                return Verdict.refused(Refusal.ALREADY_ALLOCATED);
            }
            Verdict eligibilityVerdict = eligibility(node, null);
            boolean onlyItemCost = eligibilityVerdict.block() != null && eligibilityVerdict.block().kind() == NodeEligibility.Kind.ITEM_COST;
            return onlyItemCost ? Verdict.GRANTED : eligibilityVerdict;
        });
    }

    public Set<String> grantOnlyNodeIds() {
        if (grantOnlyNodeIds == null) {
            grantOnlyNodeIds = ShipTrees.grantOnlyNodeIds(data, topology, rootId);
        }
        return grantOnlyNodeIds;
    }

    public Verdict deallocation(SkillNode node) {
        NodeVerdicts verdicts = verdictsFor(node);
        if (verdicts.deallocation == null) {
            verdicts.deallocation = computeDeallocation(node);
        }
        return verdicts.deallocation;
    }

    public Verdict optionSwitch(SkillNode node, SkillType option) {
        NodeVerdicts verdicts = verdictsFor(node);
        if (verdicts.optionSwitches == null) {
            verdicts.optionSwitches = new HashMap<>();
        }
        return verdicts.optionSwitches.computeIfAbsent(optionKey(option), key -> computeOptionSwitch(node, option));
    }

    public List<SkillType> options(SkillNode node) {
        List<SkillType> options = new ArrayList<>();
        if (!offersOptions(node)) {
            return options;
        }
        for (String optionId : node.getType().getOptionalOptionIds()) {
            SkillType option = SkillTree.getType(optionId);
            if (option != null) {
                options.add(option);
            }
        }
        return options;
    }

    private Verdict computeAllocation(SkillNode node) {
        if (!offersOptions(node)) {
            Verdict placement = placement(node);
            return placement.allowed() ? eligibility(node, null) : placement;
        }
        Verdict firstRefusal = null;
        for (SkillType option : options(node)) {
            Verdict optionVerdict = allocation(node, option);
            if (optionVerdict.allowed()) {
                return Verdict.GRANTED;
            }
            if (firstRefusal == null) {
                firstRefusal = optionVerdict;
            }
        }
        return firstRefusal != null ? firstRefusal : allocation(node, null);
    }

    private Verdict placement(SkillNode node) {
        if (data.isAllocated(node.getId())) {
            return Verdict.refused(Refusal.ALREADY_ALLOCATED);
        }
        if (!isConnected(node)) {
            return Verdict.refused(Refusal.NOT_CONNECTED);
        }
        String pairedId = ShipTrees.wormholePartner(data, node);
        int slotsNeeded = pairedId != null && !data.isAllocated(pairedId) ? 2 : 1;
        if (data.usedNodeSlots() + slotsNeeded > budget.maxAllocatedNodes()) {
            return Verdict.refused(Refusal.NODE_CAP);
        }
        int opCost = opCostFor(node);
        if (opCost > 0 && data.getBankedFreeAllocations() <= 0 && spentOp + opCost > budget.totalOp()) {
            return Verdict.refused(Refusal.OUT_OF_OP);
        }
        return Verdict.GRANTED;
    }

    private boolean isConnected(SkillNode node) {
        if (node.getType().getTier() == SkillTier.WORMHOLE_EXIT) {
            return false;
        }
        if (node.getConnectedNodeIds().isEmpty()) {
            return true;
        }
        for (String connectedId : node.getConnectedNodeIds()) {
            if (data.isSatisfied(connectedId, rootId) && !grantOnlyNodeIds().contains(connectedId)) {
                return true;
            }
        }
        return false;
    }

    private Verdict computeDeallocation(SkillNode node) {
        if (!data.isAllocated(node.getId())) {
            return Verdict.refused(Refusal.NOT_ALLOCATED);
        }
        if (node.getId().equals(rootId)) {
            return Verdict.refused(Refusal.STARTING_ROOT);
        }
        if (grantOnlyNodeIds().contains(node.getId())) {
            return Verdict.refused(Refusal.HELD_BY_SUBROUTINE);
        }
        return ShipTrees.canDeallocate(data, node, topology, rootId) ? Verdict.GRANTED : Verdict.refused(Refusal.STRANDS_NODES);
    }

    private Verdict computeOptionSwitch(SkillNode node, SkillType option) {
        if (!data.isAllocated(node.getId())) {
            return Verdict.refused(Refusal.NOT_ALLOCATED);
        }
        if (option != null && option.getId().equals(data.getOptionalSelection(node.getId()))) {
            return Verdict.refused(Refusal.SAME_OPTION);
        }
        return Verdict.fromBlock(NodeEligibility.check(checkedNode(node), option, eligibility.excluding(node.getId(), data)));
    }

    private SkillNode checkedNode(SkillNode node) {
        return DoctrineOverrides.asTransformed(data, node);
    }

    private NodeVerdicts verdictsFor(SkillNode node) {
        return verdictsByNodeId.computeIfAbsent(node.getId(), id -> new NodeVerdicts());
    }

    private static String optionKey(SkillType option) {
        return option == null ? "" : option.getId();
    }
}
