package exiledsector.skills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AllocationGate {

    public enum Refusal {
        ALREADY_ALLOCATED, NOT_ALLOCATED, NOT_CONNECTED, NODE_CAP, OUT_OF_OP, INELIGIBLE, STARTING_ROOT, STRANDS_NODES, SAME_OPTION
    }

    public record Verdict(Refusal refusal, NodeEligibility.Block block) {

        public static final Verdict ALLOWED = new Verdict(null, null);

        static Verdict refused(Refusal refusal) {
            return new Verdict(refusal, null);
        }

        static Verdict fromBlock(NodeEligibility.Block block) {
            return block == null ? ALLOWED : new Verdict(Refusal.INELIGIBLE, block);
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

    public AllocationGate(ShipSkillData data, SkillTreeTopology topology, String rootId, Budget budget, NodeEligibility.Context eligibility) {
        this.data = data;
        this.topology = topology;
        this.rootId = rootId;
        this.budget = budget;
        this.eligibility = eligibility;
        this.spentOp = data.getSpentOp(budget.opCostPerNode());
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
        if (option == null && !node.getType().isOptional()) {
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
        return verdicts.optionEligibilities.computeIfAbsent(optionKey(option), key -> Verdict.fromBlock(eligibilityBlock(node, option)));
    }

    private NodeEligibility.Block eligibilityBlock(SkillNode node, SkillType option) {
        if (option == null && node.getType().isOptional()) {
            return new NodeEligibility.Block(NodeEligibility.Kind.INVALID_OPTION, NodeEligibility.OptionProblem.MISSING.name(), null);
        }
        return NodeEligibility.check(node, option, eligibility);
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
        for (String optionId : node.getType().getOptionalOptionIds()) {
            SkillType option = SkillTree.getType(optionId);
            if (option != null) {
                options.add(option);
            }
        }
        return options;
    }

    private Verdict computeAllocation(SkillNode node) {
        if (!node.getType().isOptional()) {
            Verdict placement = placement(node);
            return placement.allowed() ? eligibility(node, null) : placement;
        }
        Verdict firstRefusal = null;
        for (SkillType option : options(node)) {
            Verdict optionVerdict = allocation(node, option);
            if (optionVerdict.allowed()) {
                return Verdict.ALLOWED;
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
        String pairedId = node.getPairedNodeId();
        int slotsNeeded = pairedId != null && !data.isAllocated(pairedId) ? 2 : 1;
        if (data.getAllocatedNodeIds().size() + slotsNeeded > budget.maxAllocatedNodes()) {
            return Verdict.refused(Refusal.NODE_CAP);
        }
        int opCost = opCostFor(node);
        if (opCost > 0 && data.getBankedFreeAllocations() <= 0 && spentOp + opCost > budget.totalOp()) {
            return Verdict.refused(Refusal.OUT_OF_OP);
        }
        return Verdict.ALLOWED;
    }

    private boolean isConnected(SkillNode node) {
        if (node.getConnectedNodeIds().isEmpty()) {
            return true;
        }
        for (String connectedId : node.getConnectedNodeIds()) {
            if (data.isSatisfied(connectedId, rootId)) {
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
        return data.canDeallocate(node, topology, rootId) ? Verdict.ALLOWED : Verdict.refused(Refusal.STRANDS_NODES);
    }

    private Verdict computeOptionSwitch(SkillNode node, SkillType option) {
        if (!data.isAllocated(node.getId())) {
            return Verdict.refused(Refusal.NOT_ALLOCATED);
        }
        if (option != null && option.getId().equals(data.getOptionalSelection(node.getId()))) {
            return Verdict.refused(Refusal.SAME_OPTION);
        }
        return Verdict.fromBlock(NodeEligibility.check(node, option, eligibility.excluding(node.getId(), data)));
    }

    private NodeVerdicts verdictsFor(SkillNode node) {
        return verdictsByNodeId.computeIfAbsent(node.getId(), id -> new NodeVerdicts());
    }

    private static String optionKey(SkillType option) {
        return option == null ? "" : option.getId();
    }
}
