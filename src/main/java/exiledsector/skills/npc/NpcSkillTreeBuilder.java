package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocatedSkillEffects;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.ShieldSkillEffect;
import exiledsector.skills.tags.NodeRequirements;
import exiledsector.skills.tags.ShipProfile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class NpcSkillTreeBuilder {

    public static final int MAX_NODE_COUNT = 60;

    private NpcSkillTreeBuilder() {
    }

    public static NpcTreeBuild build(NpcLayout layout, int nodeCount, ShipProfile profile, NpcHullMods hullMods) {
        return build(layout, nodeCount, profile, hullMods, NpcFreedOp.NONE);
    }

    public static NpcTreeBuild build(NpcLayout layout, int nodeCount, ShipProfile profile, NpcHullMods hullMods,
                                     NpcFreedOp freedOp) {
        int target = Math.max(0, Math.min(nodeCount, MAX_NODE_COUNT));
        NpcHullMods mods = hullMods == null ? NpcHullMods.NONE : hullMods;
        NpcFreedOp budget = freedOp == null ? NpcFreedOp.NONE : freedOp;
        Set<String> installed = mods.installed();
        List<NpcBuildStep> steps = new ArrayList<>();
        List<String> stripped = new ArrayList<>();
        SkillNode root = SkillTree.get(layout.rootNodeId());
        ShipSkillData data = rootedTree(root, target);
        if (data != null) {
            BuildContext context = new BuildContext(data, root.getId(), profile, installed, layoutOptions(layout),
                    budget.opCostPerNode());
            int allocated = convertHullMods(context, mods.removable(), target, steps, stripped);
            int limit = target + budget.extraNodes(stripped, target);
            for (NpcLayoutEntry entry : layout.entries()) {
                String outcome = allocated >= limit
                        ? NpcBuildStep.COUNT_REACHED
                        : tryAllocate(context, entry);
                if (NpcBuildStep.ALLOCATED.equals(outcome)) {
                    if (allocated >= target) {
                        outcome = NpcBuildStep.ALLOCATED_WITH_FREED_OP;
                    }
                    allocated++;
                }
                steps.add(new NpcBuildStep(entry.nodeId(), outcome));
            }
        } else {
            data = emptyTree();
            for (NpcLayoutEntry entry : layout.entries()) {
                steps.add(new NpcBuildStep(entry.nodeId(), NpcBuildStep.INVALID_ROOT + layout.rootNodeId()));
            }
        }
        return new NpcTreeBuild(data, steps, stripped);
    }

    public static ShipSkillData emptyTree() {
        ShipSkillData data = new ShipSkillData();
        data.markNpcBuild();
        return data;
    }

    public static ShipSkillData rootedTree(SkillNode root, int level) {
        ShipSkillData data = emptyTree();
        if (root == null || !data.chooseStartingRoot(root)) {
            return null;
        }
        for (int i = 0; i < level; i++) {
            data.addFreeAllocationCredit();
            data.incrementLevel();
        }
        return data;
    }

    private record BuildContext(ShipSkillData data, String rootId, ShipProfile profile, Set<String> installed,
                                Map<String, String> layoutOptions, int opCostPerNode) {
    }

    private record AllocationState(List<AllocatedNode> allocated, ShieldType shieldType) {

        static AllocationState of(BuildContext context) {
            ShipSkillData data = context.data();
            ShipProfile profile = context.profile();
            return new AllocationState(AllocatedNode.of(data), ShieldSkillEffect.resolveDisplayShieldType(profile.shieldType(),
                    AllocatedSkillEffects.forData(data, profile.hullSize())));
        }
    }

    private record PathStep(SkillNode node, SkillType option) {
    }

    private record Conversion(String hullModId, List<PathStep> path) {
    }

    private static Map<String, String> layoutOptions(NpcLayout layout) {
        Map<String, String> options = new HashMap<>();
        for (NpcLayoutEntry entry : layout.entries()) {
            if (entry.optionTypeId() != null) {
                options.putIfAbsent(entry.nodeId(), entry.optionTypeId());
            }
        }
        return options;
    }

    private static int convertHullMods(BuildContext context, Set<String> removable, int target,
                                       List<NpcBuildStep> steps, List<String> stripped) {
        List<SkillNode> sorted = sortedNodes(SkillTree.getAllNodes().values());
        Map<String, List<SkillNode>> equivalents = equivalentNodesByHullMod(sorted, removable);
        if (equivalents.isEmpty()) {
            return 0;
        }
        Set<String> pending = new TreeSet<>(equivalents.keySet());
        Map<String, List<SkillNode>> dependents = dependentsByNodeId(sorted);
        int allocated = 0;
        while (!pending.isEmpty()) {
            AllocationState state = AllocationState.of(context);
            Conversion best = null;
            for (String hullModId : pending) {
                List<PathStep> path = shortestPath(context, state, equivalents.get(hullModId), hullModId, dependents);
                if (path != null && path.size() <= target - allocated
                        && (best == null || path.size() < best.path().size())) {
                    best = new Conversion(hullModId, path);
                }
            }
            if (best == null) {
                break;
            }
            context.installed().remove(best.hullModId());
            for (int i = 0; i < best.path().size(); i++) {
                PathStep step = best.path().get(i);
                allocate(context, step.node(), step.option());
                boolean isEquivalent = i == best.path().size() - 1;
                steps.add(new NpcBuildStep(step.node().getId(),
                        (isEquivalent ? NpcBuildStep.CONVERTED_HULLMOD : NpcBuildStep.PATH_TO_CONVERTED_HULLMOD)
                                + best.hullModId()));
            }
            allocated += best.path().size();
            stripped.add(best.hullModId());
            pending.remove(best.hullModId());
        }
        for (String hullModId : pending) {
            steps.add(new NpcBuildStep(equivalents.get(hullModId).get(0).getId(), NpcBuildStep.HULLMOD_KEPT + hullModId));
        }
        return allocated;
    }

    private static Map<String, List<SkillNode>> equivalentNodesByHullMod(List<SkillNode> sorted, Set<String> removable) {
        Map<String, List<SkillNode>> equivalents = new TreeMap<>();
        if (removable.isEmpty()) {
            return equivalents;
        }
        for (SkillNode node : sorted) {
            String hullModId = node.getType().getEquivalentHullModId();
            SkillTier tier = node.getType().getTier();
            if (hullModId != null && removable.contains(hullModId) && tier != SkillTier.WORMHOLE && tier != SkillTier.ROOT) {
                equivalents.computeIfAbsent(hullModId, key -> new ArrayList<>()).add(node);
            }
        }
        return equivalents;
    }

    private static Map<String, List<SkillNode>> dependentsByNodeId(List<SkillNode> sorted) {
        Map<String, List<SkillNode>> dependents = new HashMap<>();
        for (SkillNode node : sorted) {
            for (String connectedId : node.getConnectedNodeIds()) {
                dependents.computeIfAbsent(connectedId, key -> new ArrayList<>()).add(node);
            }
        }
        return dependents;
    }

    private static List<SkillNode> sortedNodes(Collection<SkillNode> nodes) {
        List<SkillNode> sorted = new ArrayList<>(nodes);
        sorted.sort((a, b) -> a.getId().compareTo(b.getId()));
        return sorted;
    }

    private static List<PathStep> shortestPath(BuildContext context, AllocationState state, List<SkillNode> targets,
                                               String hullModId, Map<String, List<SkillNode>> dependents) {
        Set<String> targetIds = new HashSet<>();
        for (SkillNode targetNode : targets) {
            targetIds.add(targetNode.getId());
        }
        Set<String> installedAfterStrip = new TreeSet<>(context.installed());
        installedAfterStrip.remove(hullModId);
        Map<String, String> parents = new HashMap<>();
        Map<String, SkillType> options = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>(new TreeSet<>(context.data().getAllocatedNodeIds()));
        Set<String> visited = new HashSet<>(queue);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (SkillNode candidate : dependents.getOrDefault(current, List.of())) {
                if (!visited.add(candidate.getId())) {
                    continue;
                }
                SkillType option = pathOption(context, candidate);
                if (option == null && candidate.getType().isOptional()
                        || structuralSkipReason(context.data(), candidate, option == null ? null : option.getId()) != null
                        || fitSkipReason(context, state, candidate, option, installedAfterStrip) != null) {
                    continue;
                }
                parents.put(candidate.getId(), current);
                options.put(candidate.getId(), option);
                if (targetIds.contains(candidate.getId())) {
                    List<PathStep> path = reconstruct(candidate.getId(), parents, options, context.data());
                    return hasInternalConflict(path) ? null : path;
                }
                queue.add(candidate.getId());
            }
        }
        return null;
    }

    private static SkillType pathOption(BuildContext context, SkillNode node) {
        if (!node.getType().isOptional()) {
            return null;
        }
        String layoutOption = context.layoutOptions().get(node.getId());
        if (layoutOption != null && optionSkipReason(node.getType(), layoutOption) == null) {
            return SkillTree.getType(layoutOption);
        }
        for (String optionId : node.getType().getOptionalOptionIds()) {
            SkillType option = SkillTree.getType(optionId);
            if (option != null && option.allowsHullSize(context.profile().hullSize())
                    && NodeRequirements.isSatisfiedBy(node.effectiveTags(option), context.profile())) {
                return option;
            }
        }
        return null;
    }

    private static List<PathStep> reconstruct(String targetId, Map<String, String> parents, Map<String, SkillType> options,
                                              ShipSkillData data) {
        List<PathStep> path = new ArrayList<>();
        String current = targetId;
        while (current != null && !data.isAllocated(current)) {
            path.add(new PathStep(SkillTree.get(current), options.get(current)));
            current = parents.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private static boolean hasInternalConflict(List<PathStep> path) {
        for (int i = 0; i < path.size(); i++) {
            for (int j = i + 1; j < path.size(); j++) {
                PathStep a = path.get(i);
                PathStep b = path.get(j);
                if (AllocatedNode.planned(a.node(), a.option()).isExclusiveWith(AllocatedNode.planned(b.node(), b.option()))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String tryAllocate(BuildContext context, NpcLayoutEntry entry) {
        SkillNode node = SkillTree.get(entry.nodeId());
        if (node == null) {
            return NpcBuildStep.UNKNOWN_NODE;
        }
        String structuralReason = structuralSkipReason(context.data(), node, entry.optionTypeId());
        if (structuralReason != null) {
            return structuralReason;
        }
        SkillType option = node.getType().isOptional() ? SkillTree.getType(entry.optionTypeId()) : null;
        String fitReason = fitSkipReason(context, AllocationState.of(context), node, option, context.installed());
        if (fitReason != null) {
            return fitReason;
        }
        if (!context.data().canAllocate(node, context.rootId(), Integer.MAX_VALUE, 0, Integer.MAX_VALUE)) {
            return NpcBuildStep.NOT_CONNECTED;
        }
        allocate(context, node, option);
        return NpcBuildStep.ALLOCATED;
    }

    private static void allocate(BuildContext context, SkillNode node, SkillType option) {
        if (option != null) {
            context.data().selectOption(node, option, context.opCostPerNode());
        } else {
            context.data().allocate(node, context.opCostPerNode());
        }
    }

    private static String structuralSkipReason(ShipSkillData data, SkillNode node, String optionTypeId) {
        if (data.isAllocated(node.getId())) {
            return NpcBuildStep.ALREADY_ALLOCATED;
        }
        SkillTier tier = node.getType().getTier();
        if (tier == SkillTier.WORMHOLE) {
            return NpcBuildStep.WORMHOLE;
        }
        if (tier == SkillTier.ROOT) {
            return NpcBuildStep.ROOT_NODE;
        }
        return optionSkipReason(node.getType(), optionTypeId);
    }

    static String optionSkipReason(SkillType type, String optionTypeId) {
        if (!type.isOptional()) {
            return optionTypeId == null ? null : NpcBuildStep.UNEXPECTED_OPTION + optionTypeId;
        }
        if (optionTypeId == null) {
            return NpcBuildStep.MISSING_OPTION;
        }
        if (!type.getOptionalOptionIds().contains(optionTypeId) || SkillTree.getType(optionTypeId) == null) {
            return NpcBuildStep.INVALID_OPTION + optionTypeId;
        }
        return null;
    }

    private static String fitSkipReason(BuildContext context, AllocationState state, SkillNode node, SkillType option,
                                        Set<String> installed) {
        HullSize hullSize = context.profile().hullSize();
        if (!node.getType().allowsHullSize(hullSize) || option != null && !option.allowsHullSize(hullSize)) {
            return NpcBuildStep.WRONG_HULL_SIZE;
        }
        String unmet = NodeRequirements.firstUnmet(node.effectiveTags(option), context.profile());
        if (unmet != null) {
            return NpcBuildStep.UNMET_REQUIREMENT + unmet;
        }
        for (String hullModId : exclusiveHullModIds(node.getType(), option)) {
            if (installed.contains(hullModId)) {
                return NpcBuildStep.INSTALLED_HULLMOD_CONFLICT + hullModId;
            }
        }
        String conflictingType = conflictingAllocatedTypeId(state.allocated(), AllocatedNode.planned(node, option));
        if (conflictingType != null) {
            return NpcBuildStep.EXCLUSIVE_TYPE_CONFLICT + conflictingType;
        }
        String shipStateReason = shipStateBlockReason(state.shieldType(), option != null ? option : node.getType(), hullSize);
        if (shipStateReason != null) {
            return NpcBuildStep.BLOCKED_BY_SHIP_STATE + shipStateReason;
        }
        return null;
    }

    private static String shipStateBlockReason(ShieldType shieldType, SkillType effectiveType, HullSize hullSize) {
        for (SkillTypeEffect effect : effectiveType.effectsFor(hullSize)) {
            String reason = effect.effect().shieldTypeBlockReason(shieldType);
            if (reason != null) {
                return reason;
            }
        }
        return null;
    }

    private static Set<String> exclusiveHullModIds(SkillType type, SkillType option) {
        Set<String> ids = new LinkedHashSet<>(type.getExclusiveHullModIds());
        if (option != null) {
            ids.addAll(option.getExclusiveHullModIds());
        }
        return ids;
    }

    private static String conflictingAllocatedTypeId(List<AllocatedNode> allocatedNodes, AllocatedNode candidate) {
        for (AllocatedNode allocated : allocatedNodes) {
            if (allocated.isExclusiveWith(candidate)) {
                return allocated.effectiveType().getId();
            }
        }
        return null;
    }
}
