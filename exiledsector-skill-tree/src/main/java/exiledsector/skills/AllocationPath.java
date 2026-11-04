package exiledsector.skills;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

public final class AllocationPath {

    public record Step(SkillNode node, SkillType option) {
    }

    private AllocationPath() {
    }

    public static List<Step> to(SkillNode target, AllocationCheck gate, SkillTreeTopology topology, Predicate<SkillNode> hidden,
                                SkillType preferredOption, RandomGenerator random) {
        if (target == null || gate.data().isAllocated(target.getId()) || hidden.test(target) || !eligible(target, gate)) {
            return List.of();
        }
        List<SkillNode> nodes = shortestPath(target, gate, topology, hidden);
        if (nodes.isEmpty()) {
            return List.of();
        }
        Map<String, SkillType> pickedByChoiceTypeId = new HashMap<>();
        List<Step> steps = new ArrayList<>(nodes.size());
        for (SkillNode node : nodes) {
            SkillType option = gate.offersOptions(node) ? option(node, gate, topology, preferredOption, pickedByChoiceTypeId, random) : null;
            steps.add(new Step(node, option));
        }
        return List.copyOf(steps);
    }

    private static List<SkillNode> shortestPath(SkillNode target, AllocationCheck gate, SkillTreeTopology topology, Predicate<SkillNode> hidden) {
        ShipSkillData data = gate.data();
        Set<String> seeds = new TreeSet<>(data.getAllocatedNodeIds());
        seeds.removeAll(gate.grantOnlyNodeIds());
        if (gate.rootId() != null) {
            seeds.add(gate.rootId());
        }
        String targetId = target.getId();
        TreeSearch search = TreeSearch.from(topology, seeds, candidate -> passable(candidate, gate, hidden),
                entered -> unallocatedPartner(entered, data));
        if (!search.reaches(targetId) || search.isSeed(targetId)) {
            return List.of();
        }
        Deque<SkillNode> path = new ArrayDeque<>();
        for (String id = targetId; !search.isSeed(id); id = search.parent(id)) {
            if (!search.isJump(id)) {
                path.addFirst(topology.node(id));
            }
        }
        return List.copyOf(path);
    }

    private static String unallocatedPartner(SkillNode node, ShipSkillData data) {
        String partnerId = ShipTreeLinks.wormholePartner(data, node);
        return partnerId != null && !data.isAllocated(partnerId) ? partnerId : null;
    }

    private static boolean passable(SkillNode node, AllocationCheck gate, Predicate<SkillNode> hidden) {
        SkillTier tier = node.getType().getTier();
        return tier != SkillTier.WORMHOLE_EXIT && tier != SkillTier.ROOT && !gate.data().isAllocated(node.getId()) && !hidden.test(node)
                && eligible(node, gate);
    }

    private static boolean eligible(SkillNode node, AllocationCheck gate) {
        if (!gate.offersOptions(node)) {
            return gate.eligibility(node, null).allowed();
        }
        return !usableOptions(node, gate).isEmpty();
    }

    private static List<SkillType> usableOptions(SkillNode node, AllocationCheck gate) {
        List<SkillType> usable = new ArrayList<>();
        for (SkillType option : gate.options(node)) {
            if (gate.eligibility(node, option).allowed()) {
                usable.add(option);
            }
        }
        return usable;
    }

    private static SkillType option(SkillNode node, AllocationCheck gate, SkillTreeTopology topology, SkillType preferredOption,
                                    Map<String, SkillType> pickedByChoiceTypeId, RandomGenerator random) {
        List<SkillType> usable = usableOptions(node, gate);
        String choiceTypeId = node.getType().getId();
        List<SkillType> candidates = new ArrayList<>();
        candidates.add(preferredOption);
        candidates.add(pickedByChoiceTypeId.get(choiceTypeId));
        candidates.addAll(allocatedSelections(choiceTypeId, gate.data(), topology));
        SkillType picked = firstUsable(candidates, usable);
        if (picked == null) {
            picked = usable.get(random.nextInt(usable.size()));
        }
        pickedByChoiceTypeId.put(choiceTypeId, picked);
        return picked;
    }

    private static List<SkillType> allocatedSelections(String choiceTypeId, ShipSkillData data, SkillTreeTopology topology) {
        List<SkillType> selections = new ArrayList<>();
        for (String allocatedId : new TreeSet<>(data.getAllocatedNodeIds())) {
            SkillNode allocated = topology.node(allocatedId);
            String selectionId = data.getChoiceSelection(allocatedId);
            if (allocated != null && selectionId != null && allocated.getType().getId().equals(choiceTypeId)) {
                selections.add(SkillTree.getType(selectionId));
            }
        }
        return selections;
    }

    private static SkillType firstUsable(List<SkillType> candidates, List<SkillType> usable) {
        for (SkillType candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            for (SkillType option : usable) {
                if (option.getId().equals(candidate.getId())) {
                    return option;
                }
            }
        }
        return null;
    }
}
