package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.TreeSearch;
import exiledsector.skills.skilleffect.ShieldSkillEffect;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.skills.tags.SkillTags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class NpcSkillTreeBuilder {

    public static final int MAX_NODE_COUNT = 60;
    static final float NOTABLE_WEIGHT = 3f;
    static final float KEYSTONE_WEIGHT = 4f;
    private static final String SHIELD_THEME = "shield";
    private static final String SHIELD_REQUIREMENT = "req_shields";
    static final Map<String, String> ROOT_TYPE_BY_DESIGN_TYPE = Map.of(
            "Low Tech", "root_low_tech", "Midline", "root_midline", "High Tech", "root_high_tech");

    private NpcSkillTreeBuilder() {
    }

    public static NpcTreeBuild generate(NpcBuildRequest request, Random random) {
        int target = Math.max(0, Math.min(request.nodeCount(), MAX_NODE_COUNT));
        SkillNode root = chooseRoot(request.designType(), random);
        ShipSkillData data = rootedTree(root, target);
        if (data == null) {
            String rootId = root == null ? "none" : root.getId();
            return new NpcTreeBuild(emptyTree(), List.of(new NpcBuildStep(rootId, NpcBuildStep.INVALID_ROOT + rootId)), List.of());
        }
        Generation generation = new Generation(request, data, target, random);
        generation.run();
        return new NpcTreeBuild(data, generation.steps, generation.stripped, generation.claimedSockets);
    }

    public static boolean hasRootFor(String designType) {
        return designType != null && ROOT_TYPE_BY_DESIGN_TYPE.containsKey(designType.trim());
    }

    static SkillNode chooseRoot(String designType, Random random) {
        List<SkillNode> roots = new ArrayList<>();
        for (SkillNode node : SkillTree.topology().sortedById()) {
            if (node.getType().getTier() == SkillTier.ROOT) {
                roots.add(node);
            }
        }
        String rootTypeId = designType == null ? null : ROOT_TYPE_BY_DESIGN_TYPE.get(designType.trim());
        for (SkillNode root : roots) {
            if (root.getType().getId().equals(rootTypeId)) {
                return root;
            }
        }
        return roots.isEmpty() ? null : roots.get(random.nextInt(roots.size()));
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

    static String optionSkipReason(SkillType type, String optionTypeId) {
        NodeEligibility.OptionProblem problem = NodeEligibility.optionProblem(type, optionTypeId);
        if (problem == null) {
            return null;
        }
        return switch (problem) {
            case UNEXPECTED -> NpcBuildStep.UNEXPECTED_OPTION + optionTypeId;
            case MISSING -> NpcBuildStep.MISSING_OPTION;
            case INVALID -> NpcBuildStep.INVALID_OPTION + optionTypeId;
        };
    }

    static <T> T weightedPick(List<T> items, ToDoubleFunction<T> weight, Random random) {
        double total = 0;
        for (T item : items) {
            total += Math.max(0, weight.applyAsDouble(item));
        }
        if (items.isEmpty()) {
            return null;
        }
        if (total <= 0) {
            return items.get(random.nextInt(items.size()));
        }
        double roll = random.nextDouble() * total;
        for (T item : items) {
            roll -= Math.max(0, weight.applyAsDouble(item));
            if (roll < 0) {
                return item;
            }
        }
        return items.get(items.size() - 1);
    }

    private record State(List<AllocatedNode> allocated, ShieldType shieldType, ShipFacts facts, ShipProfile profile,
                         boolean shieldInvested) {
    }

    private record PathStep(SkillNode node, SkillType option, boolean wormholeExit) {
    }

    private record Search(TreeSearch tree, List<String> reached) {

        static Search of(TreeSearch tree) {
            List<String> reached = new ArrayList<>();
            for (String id : tree.reached()) {
                if (!tree.isSeed(id) && !tree.isJump(id)) {
                    reached.add(id);
                }
            }
            return new Search(tree, List.copyOf(reached));
        }

        boolean reaches(String nodeId) {
            return tree.reaches(nodeId) && !tree.isSeed(nodeId) && !tree.isJump(nodeId);
        }

        int cost(String nodeId) {
            return reaches(nodeId) ? tree.distance(nodeId) : Integer.MAX_VALUE;
        }

        String parent(String nodeId) {
            return tree.parent(nodeId);
        }

        boolean isExit(String nodeId) {
            return tree.isJump(nodeId);
        }
    }

    private static final class Generation {

        private final ShipSkillData data;
        private final ShipProfile profile;
        private final String factionRegion;
        private final NpcHullMods hullMods;
        private final NpcFreedOp freedOp;
        private final int free;
        private final int maxNodes;
        private final Random random;
        private final NpcRelevance relevance;
        private final SkillTreeTopology topology = SkillTree.topology();
        private final Set<String> installed;
        private final Map<String, SkillType> chosenOptions = new HashMap<>();
        private final Set<String> rejected = new HashSet<>();
        private final List<NpcBuildStep> steps = new ArrayList<>();
        private final List<String> stripped = new ArrayList<>();
        private final List<String> claimedSockets = new ArrayList<>();
        private final int socketables;
        private final Predicate<SkillType> lockedWormhole;
        private int budget;
        private int spent;

        Generation(NpcBuildRequest request, ShipSkillData data, int target, Random random) {
            this.data = data;
            this.profile = request.profile();
            this.factionRegion = request.factionRegion() != null && SkillTags.FACTION_VOLUMES.contains(request.factionRegion())
                    ? request.factionRegion() : null;
            this.hullMods = request.hullMods();
            this.freedOp = request.freedOp();
            this.free = target;
            this.maxNodes = Math.min(freedOp.maxNodeCount(), MAX_NODE_COUNT);
            this.random = random;
            this.relevance = NpcRelevance.of(profile, hullMods.permanent());
            this.installed = new TreeSet<>(hullMods.installed());
            this.budget = Math.min(target, maxNodes);
            this.socketables = request.socketables();
            Predicate<SkillType> locked = request.locked();
            this.lockedWormhole = type -> type.getTier() == SkillTier.WORMHOLE && locked.test(type);
        }

        void run() {
            convertHullMods();
            claimSockets();
            tasteFactionVolume();
            pursueGoals();
        }

        private void claimSockets() {
            for (int i = 0; i < socketables; i++) {
                String socket = unclaimedAllocatedSocket();
                if (socket == null) {
                    socket = reachSocket();
                }
                if (socket == null) {
                    steps.add(new NpcBuildStep("", NpcBuildStep.SOCKETABLE_DISCARDED));
                } else {
                    claimedSockets.add(socket);
                }
            }
        }

        private String unclaimedAllocatedSocket() {
            for (String id : data.getAllocatedNodeIds()) {
                SkillNode node = topology.node(id);
                if (node != null && node.getType().getTier() == SkillTier.SOCKET && !claimedSockets.contains(id)) {
                    return id;
                }
            }
            return null;
        }

        private String reachSocket() {
            while (true) {
                Search search = search(currentInstalled());
                String nearest = null;
                for (String id : search.reached()) {
                    if (topology.node(id).getType().getTier() == SkillTier.SOCKET && !rejected.contains(id)
                            && affordable(search, id, remaining()) && (nearest == null || search.cost(id) < search.cost(nearest))) {
                        nearest = id;
                    }
                }
                if (nearest == null || allocatePath(search, nearest, NpcBuildStep.SOCKET_GOAL, NpcBuildStep.PATH_TO_GOAL)) {
                    return nearest;
                }
                rejected.add(nearest);
            }
        }

        private int remaining() {
            return budget - spent;
        }

        private int slotsLeft() {
            return maxNodes - (data.getAllocatedNodeIds().size() - 1);
        }

        private boolean affordable(Search search, String nodeId, int allowance) {
            return search.cost(nodeId) <= allowance && pathSize(search, nodeId) <= slotsLeft();
        }

        private void convertHullMods() {
            Map<String, List<SkillNode>> equivalents = equivalentNodesByHullMod(hullMods.removable());
            Set<String> pending = new TreeSet<>(equivalents.keySet());
            int convertedOp = 0;
            while (!pending.isEmpty()) {
                String bestMod = null;
                String bestNode = null;
                Search bestSearch = null;
                for (String hullModId : pending) {
                    Set<String> fitInstalled = new TreeSet<>(installed);
                    fitInstalled.removeAll(stripped);
                    fitInstalled.remove(hullModId);
                    Search search = search(fitInstalled);
                    int allowance = Math.min(free + (convertedOp + opCost(hullModId)) / freedOp.opCostPerNode(), maxNodes) - spent;
                    for (SkillNode node : equivalents.get(hullModId)) {
                        String id = node.getId();
                        if (search.reaches(id) && !rejected.contains(id) && affordable(search, id, allowance)
                                && (bestNode == null || search.cost(id) < bestSearch.cost(bestNode))) {
                            bestMod = hullModId;
                            bestNode = id;
                            bestSearch = search;
                        }
                    }
                }
                if (bestMod == null) {
                    break;
                }
                if (!allocatePath(bestSearch, bestNode, NpcBuildStep.CONVERTED_HULLMOD + bestMod,
                        NpcBuildStep.PATH_TO_CONVERTED_HULLMOD + bestMod)) {
                    rejected.add(bestNode);
                    continue;
                }
                convertedOp += opCost(bestMod);
                stripped.add(bestMod);
                pending.remove(bestMod);
            }
            for (String hullModId : pending) {
                steps.add(new NpcBuildStep(equivalents.get(hullModId).get(0).getId(), NpcBuildStep.HULLMOD_KEPT + hullModId));
            }
            budget = Math.min(free + convertedOp / freedOp.opCostPerNode(), maxNodes);
        }

        private int opCost(String hullModId) {
            return Math.max(0, freedOp.hullModOpCosts().getOrDefault(hullModId, 0));
        }

        private Map<String, List<SkillNode>> equivalentNodesByHullMod(Set<String> removable) {
            Map<String, List<SkillNode>> equivalents = new TreeMap<>();
            for (SkillNode node : topology.sortedById()) {
                String hullModId = node.getType().getEquivalentHullModId();
                SkillTier tier = node.getType().getTier();
                if (hullModId != null && removable.contains(hullModId) && tier != SkillTier.WORMHOLE && tier != SkillTier.ROOT) {
                    equivalents.computeIfAbsent(hullModId, key -> new ArrayList<>()).add(node);
                }
            }
            return equivalents;
        }

        private Set<String> currentInstalled() {
            Set<String> current = new TreeSet<>(installed);
            current.removeAll(stripped);
            return current;
        }

        private void tasteFactionVolume() {
            if (factionRegion == null || remaining() <= 0) {
                return;
            }
            Search search = search(currentInstalled());
            List<String> candidates = new ArrayList<>();
            for (String id : search.reached()) {
                SkillNode node = topology.node(id);
                SkillTier tier = node.getType().getTier();
                if ((tier == SkillTier.NOTABLE || tier == SkillTier.KEYSTONE) && factionRegion.equals(node.getRegion())
                        && !rejected.contains(id) && affordable(search, id, remaining())) {
                    candidates.add(id);
                }
            }
            String goal = weightedPick(candidates, id -> nodeRelevance(id) * tierWeight(id), random);
            if (goal != null && !allocatePath(search, goal, NpcBuildStep.FACTION_GOAL, NpcBuildStep.PATH_TO_GOAL)) {
                rejected.add(goal);
            }
        }

        private void pursueGoals() {
            List<String> chosen = new ArrayList<>();
            while (remaining() > 0 && slotsLeft() > 0) {
                Search search = search(currentInstalled());
                chosen.removeIf(id -> !search.reaches(id));
                List<String> affordableGoals = affordableGoals(search, chosen);
                if (affordableGoals.isEmpty()) {
                    drawGoals(search, chosen);
                    affordableGoals = affordableGoals(search, chosen);
                }
                if (!affordableGoals.isEmpty()) {
                    String goal = affordableGoals.get(0);
                    for (String id : affordableGoals) {
                        if (search.cost(id) < search.cost(goal)) {
                            goal = id;
                        }
                    }
                    chosen.remove(goal);
                    if (!allocatePath(search, goal, NpcBuildStep.GOAL, NpcBuildStep.PATH_TO_GOAL)) {
                        rejected.add(goal);
                    }
                } else if (!fillOne(search)) {
                    return;
                }
            }
        }

        private List<String> affordableGoals(Search search, List<String> chosen) {
            List<String> affordable = new ArrayList<>();
            for (String id : chosen) {
                if (affordable(search, id, remaining())) {
                    affordable.add(id);
                }
            }
            return affordable;
        }

        private void drawGoals(Search search, List<String> chosen) {
            List<String> pool = new ArrayList<>();
            for (String id : search.reached()) {
                SkillTier tier = topology.node(id).getType().getTier();
                if ((tier == SkillTier.NOTABLE || tier == SkillTier.KEYSTONE) && !chosen.contains(id) && !rejected.contains(id)
                        && affordable(search, id, remaining())) {
                    pool.add(id);
                }
            }
            int covered = 0;
            while (covered < remaining() && !pool.isEmpty()) {
                String goal = weightedPick(pool, id -> nodeRelevance(id) * tierWeight(id), random);
                pool.remove(goal);
                chosen.add(goal);
                covered += search.cost(goal);
            }
        }

        private boolean fillOne(Search search) {
            int nearest = Integer.MAX_VALUE;
            List<String> candidates = new ArrayList<>();
            for (String id : search.reached()) {
                if (!isFillTier(topology.node(id).getType().getTier()) || rejected.contains(id) || !affordable(search, id, remaining())) {
                    continue;
                }
                int cost = search.cost(id);
                if (cost < nearest) {
                    nearest = cost;
                    candidates.clear();
                }
                if (cost == nearest) {
                    candidates.add(id);
                }
            }
            String pick = weightedPick(candidates, this::nodeRelevance, random);
            if (pick == null) {
                return false;
            }
            if (!allocatePath(search, pick, NpcBuildStep.ALLOCATED, NpcBuildStep.PATH_TO_GOAL)) {
                rejected.add(pick);
            }
            return true;
        }

        private static boolean isFillTier(SkillTier tier) {
            return tier == SkillTier.SMALL || tier == SkillTier.ROOT;
        }

        private float tierWeight(String nodeId) {
            return topology.node(nodeId).getType().getTier() == SkillTier.KEYSTONE ? KEYSTONE_WEIGHT : NOTABLE_WEIGHT;
        }

        private double nodeRelevance(String nodeId) {
            SkillNode node = topology.node(nodeId);
            return relevance.of(node.effectiveTags(chosenOptions.get(nodeId)));
        }

        private Search search(Set<String> fitInstalled) {
            State state = state(fitInstalled);
            return Search.of(TreeSearch.from(topology, new TreeSet<>(data.getAllocatedNodeIds()), candidate -> traversable(candidate, state),
                    candidate -> candidate.getType().getTier() == SkillTier.WORMHOLE ? candidate.getPairedNodeId() : null));
        }

        private State state(Set<String> fitInstalled) {
            List<AllocatedNode> allocated = AllocatedNode.of(data);
            ShieldType shieldType = NodeEligibility.currentShieldType(data, profile.hullSize(), profile.shieldType());
            int fighterBays = profile.fighterBays();
            for (String hullModId : hullMods.installed()) {
                if (!fitInstalled.contains(hullModId)) {
                    fighterBays -= freedOp.hullModFighterBays().getOrDefault(hullModId, 0);
                }
            }
            ShipProfile current = new ShipProfile(profile.hullSize(), shieldType, Math.max(0, fighterBays), profile.weaponKinds(),
                    profile.flagship(), profile.baseArmor(), profile.phaseHull(), profile.limitedSystemCharges(), profile.onlyBuiltInWings());
            boolean shieldInvested = false;
            for (AllocatedNode node : allocated) {
                List<String> tags = node.effectiveType().getTags();
                shieldInvested |= tags.contains(SHIELD_THEME) || tags.contains(SHIELD_REQUIREMENT);
            }
            return new State(allocated, shieldType, ShipFacts.of(current, fitInstalled::contains), current, shieldInvested);
        }

        private boolean traversable(SkillNode node, State state) {
            if (data.isAllocated(node.getId()) || !regionAllowed(node.getRegion())) {
                return false;
            }
            if (node.getType().getTier() == SkillTier.WORMHOLE && !wormholeAllowed(node)) {
                return false;
            }
            SkillType option = option(node, state);
            if (node.getType().isOptional() && option == null) {
                return false;
            }
            if (state.shieldInvested() && removesShield(AllocatedNode.planned(node, option).effectiveType())) {
                return false;
            }
            return usable(node, option, state);
        }

        private boolean usable(SkillNode node, SkillType option, State state) {
            return NodeEligibility.check(node, option, new NodeEligibility.Context(state.allocated(), state.shieldType(), state.facts(),
                    lockedWormhole, state.profile())) == null;
        }

        private boolean removesShield(SkillType type) {
            for (SkillTypeEffect effect : type.effectsFor(profile.hullSize())) {
                if (effect.effect() == ShieldSkillEffect.REMOVE_SHIELD) {
                    return true;
                }
            }
            return false;
        }

        private boolean regionAllowed(String region) {
            return region == null || SkillTags.CORE_REGION.equals(region) || region.equals(factionRegion);
        }

        private boolean wormholeAllowed(SkillNode wormhole) {
            SkillNode exit = topology.node(wormhole.getPairedNodeId());
            if (factionRegion == null || exit == null) {
                return false;
            }
            String here = wormhole.getRegion();
            String there = exit.getRegion();
            return SkillTags.CORE_REGION.equals(here) && factionRegion.equals(there)
                    || factionRegion.equals(here) && SkillTags.CORE_REGION.equals(there);
        }

        private SkillType option(SkillNode node, State state) {
            if (!node.getType().isOptional()) {
                return null;
            }
            SkillType cached = chosenOptions.get(node.getId());
            if (cached != null && usable(node, cached, state)) {
                return cached;
            }
            List<SkillType> options = new ArrayList<>();
            for (String optionId : node.getType().getOptionalOptionIds()) {
                SkillType option = SkillTree.getType(optionId);
                if (option != null && optionSkipReason(node.getType(), optionId) == null
                        && option.allowsHullSize(profile.hullSize()) && usable(node, option, state)) {
                    options.add(option);
                }
            }
            SkillType picked = weightedPick(options, option -> relevance.of(option.getTags()), random);
            if (picked != null) {
                chosenOptions.put(node.getId(), picked);
            }
            return picked;
        }

        private int pathSize(Search search, String targetId) {
            int size = 0;
            String current = targetId;
            while (current != null && !data.isAllocated(current)) {
                size++;
                current = search.parent(current);
            }
            return size;
        }

        private List<PathStep> path(Search search, String targetId) {
            List<PathStep> path = new ArrayList<>();
            String current = targetId;
            while (current != null && !data.isAllocated(current)) {
                SkillNode node = topology.node(current);
                path.add(new PathStep(node, chosenOptions.get(current), search.isExit(current)));
                current = search.parent(current);
            }
            Collections.reverse(path);
            return path;
        }

        private boolean allocatePath(Search search, String targetId, String goalOutcome, String pathOutcome) {
            List<PathStep> path = path(search, targetId);
            if (hasInternalConflict(path)) {
                return false;
            }
            for (int i = 0; i < path.size(); i++) {
                PathStep step = path.get(i);
                String outcome = i == path.size() - 1 ? goalOutcome : pathOutcome;
                if (step.wormholeExit()) {
                    steps.add(new NpcBuildStep(step.node().getId(), NpcBuildStep.WORMHOLE_EXIT));
                    continue;
                }
                if (step.option() != null) {
                    data.selectOption(step.node(), step.option(), freedOp.opCostPerNode());
                } else {
                    data.allocate(step.node(), freedOp.opCostPerNode());
                }
                spent++;
                steps.add(new NpcBuildStep(step.node().getId(), spent > free ? outcome + NpcBuildStep.FREED_OP_SUFFIX : outcome));
            }
            return true;
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
    }
}
