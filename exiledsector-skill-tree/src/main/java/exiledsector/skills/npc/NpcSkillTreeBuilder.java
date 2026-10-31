package exiledsector.skills.npc;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import exiledsector.skills.AffinitySteps;
import exiledsector.skills.ShipFacts;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.shield.ShieldSkillEffect;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.AllocationGate;
import exiledsector.skills.AnchorLinks;
import exiledsector.skills.NodeEligibility;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import exiledsector.skills.TreeSearch;
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
        int targetNodeCount = Math.max(0, Math.min(request.nodeCount(), MAX_NODE_COUNT));
        SkillNode rootNode = chooseRoot(request.designType(), random);
        ShipSkillData shipData = rootedTree(rootNode, targetNodeCount);
        if (shipData == null) {
            String rootId = rootNode == null ? "none" : rootNode.getId();
            return new NpcTreeBuild(emptyTree(), List.of(new NpcBuildStep(rootId, NpcBuildStep.INVALID_ROOT + rootId)));
        }
        Generation generation = new Generation(request, shipData, targetNodeCount, random);
        generation.run();
        return new NpcTreeBuild(shipData, generation.steps);
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
        for (SkillNode rootNode : roots) {
            if (rootNode.getType().getId().equals(rootTypeId)) {
                return rootNode;
            }
        }
        return roots.isEmpty() ? null : roots.get(random.nextInt(roots.size()));
    }

    public static ShipSkillData emptyTree() {
        ShipSkillData shipData = new ShipSkillData();
        shipData.markNpcBuild();
        return shipData;
    }

    public static ShipSkillData rootedTree(SkillNode rootNode, int startingLevel) {
        ShipSkillData shipData = emptyTree();
        if (rootNode == null || !ShipTrees.chooseStartingRoot(shipData, rootNode)) {
            return null;
        }
        for (int i = 0; i < startingLevel; i++) {
            shipData.addFreeAllocationCredit();
            shipData.incrementLevel();
        }
        return shipData;
    }

    static String optionSkipReason(SkillType skillType, String optionTypeId) {
        NodeEligibility.OptionProblem problem = NodeEligibility.optionProblem(skillType, optionTypeId);
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
        double totalWeight = 0;
        for (T item : items) {
            totalWeight += Math.max(0, weight.applyAsDouble(item));
        }
        if (items.isEmpty()) {
            return null;
        }
        if (totalWeight <= 0) {
            return items.get(random.nextInt(items.size()));
        }
        double roll = random.nextDouble() * totalWeight;
        for (T item : items) {
            roll -= Math.max(0, weight.applyAsDouble(item));
            if (roll < 0) {
                return item;
            }
        }
        return items.get(items.size() - 1);
    }

    private record State(AllocationGate gate, boolean shieldInvested) {
    }

    private record PathStep(SkillNode node, SkillType option, boolean wormholeExit) {
    }

    private record Search(TreeSearch treeSearch, List<String> reached, Set<String> fitInstalled, Map<String, SkillType> options) {

        static Search of(TreeSearch treeSearch, Set<String> fitInstalled, Map<String, SkillType> options) {
            List<String> reached = new ArrayList<>();
            for (String nodeId : treeSearch.reached()) {
                if (!treeSearch.isSeed(nodeId) && !treeSearch.isJump(nodeId)) {
                    reached.add(nodeId);
                }
            }
            return new Search(treeSearch, List.copyOf(reached), fitInstalled, options);
        }

        boolean reaches(String nodeId) {
            return treeSearch.reaches(nodeId) && !treeSearch.isSeed(nodeId) && !treeSearch.isJump(nodeId);
        }

        int cost(String nodeId) {
            return reaches(nodeId) ? treeSearch.distance(nodeId) : Integer.MAX_VALUE;
        }

        String parent(String nodeId) {
            return treeSearch.parent(nodeId);
        }

        boolean isExit(String nodeId) {
            return treeSearch.isJump(nodeId);
        }
    }

    private static final class Generation {

        private final ShipSkillData shipData;
        private final ShipProfile profile;
        private final String factionRegion;
        private final NpcHullMods hullMods;
        private final NpcFreedOp freedOp;
        private final int freeNodeCount;
        private final int maxNodes;
        private final Random random;
        private final NpcRelevance relevance;
        private final SkillTreeTopology topology = SkillTree.topology();
        private final Set<String> installedHullModIds;
        private final Map<String, SkillType> chosenOptions = new HashMap<>();
        private final Set<String> rejectedNodeIds = new HashSet<>();
        private final List<NpcBuildStep> steps = new ArrayList<>();
        private final List<String> strippedHullModIds = new ArrayList<>();
        private final Predicate<SkillType> lockedWormhole;
        private final String factionAnchorItemId;
        private String factionExitId;
        private String anchorSocketId;
        private int nodeBudget;
        private int nodesSpent;
        private final String hullId;

        Generation(NpcBuildRequest request, ShipSkillData shipData, int targetNodeCount, Random random) {
            this.shipData = shipData;
            this.hullId = request.hullId();
            this.profile = request.profile();
            this.factionRegion = request.factionRegion() != null && SkillTags.FACTION_VOLUMES.contains(request.factionRegion())
                    ? request.factionRegion() : null;
            this.hullMods = request.hullMods();
            this.freedOp = request.freedOp();
            this.freeNodeCount = targetNodeCount;
            this.maxNodes = Math.min(freedOp.maxNodeCount(), MAX_NODE_COUNT);
            this.random = random;
            this.relevance = NpcRelevance.of(profile, hullMods.permanent(), request.focus());
            this.installedHullModIds = new TreeSet<>(hullMods.installed());
            this.nodeBudget = Math.min(targetNodeCount, maxNodes);
            Predicate<SkillType> lockedTypes = request.lockedTypes();
            this.lockedWormhole = type -> type.getTier().isWormhole() && lockedTypes.test(type);
            this.factionAnchorItemId = request.factionAnchorItemId();
            this.factionExitId = openableExitOf(topology, factionRegion, factionAnchorItemId, lockedWormhole);
        }

        private static String openableExitOf(SkillTreeTopology topology, String region, String anchorItemId,
                                             Predicate<SkillType> lockedWormhole) {
            String exitId = region == null || anchorItemId == null ? null : AnchorLinks.lookup().exitNodeId(anchorItemId);
            SkillNode exit = exitId == null ? null : topology.node(exitId);
            boolean openable = exit != null && exit.getType().getTier() == SkillTier.WORMHOLE_EXIT && region.equals(exit.getRegion())
                    && !lockedWormhole.test(exit.getType());
            return openable ? exitId : null;
        }

        void run() {
            convertHullMods();
            tasteFactionVolume();
            closeUnusedFactionVolume();
            pursueGoals();
        }

        private String emptyAllocatedSocket() {
            for (String nodeId : shipData.getAllocatedNodeIds()) {
                SkillNode node = topology.node(nodeId);
                if (node != null && node.getType().getTier() == SkillTier.SOCKET && shipData.getSocketedItem(nodeId) == null) {
                    return nodeId;
                }
            }
            return null;
        }

        private int remaining() {
            return nodeBudget - nodesSpent;
        }

        private int slotsLeft() {
            return maxNodes - (shipData.getAllocatedNodeIds().size() - 1);
        }

        private boolean affordable(Search search, String nodeId, int allowance) {
            return search.cost(nodeId) <= allowance && pathSize(search, nodeId) <= slotsLeft();
        }

        private void convertHullMods() {
            Map<String, List<SkillNode>> equivalentsByHullMod = equivalentNodesByHullMod(hullMods.removable());
            Set<String> pendingHullModIds = new TreeSet<>(equivalentsByHullMod.keySet());
            int convertedOp = 0;
            while (!pendingHullModIds.isEmpty()) {
                String bestHullModId = null;
                String bestNodeId = null;
                Search bestSearch = null;
                for (String hullModId : pendingHullModIds) {
                    Set<String> fitInstalled = new TreeSet<>(installedHullModIds);
                    fitInstalled.removeAll(strippedHullModIds);
                    fitInstalled.remove(hullModId);
                    Search search = search(fitInstalled);
                    int allowance = Math.min(freeNodeCount + (convertedOp + opCost(hullModId)) / freedOp.opCostPerNode(), maxNodes) - nodesSpent;
                    for (SkillNode node : equivalentsByHullMod.get(hullModId)) {
                        String nodeId = node.getId();
                        if (search.reaches(nodeId) && !rejectedNodeIds.contains(nodeId) && affordable(search, nodeId, allowance)
                                && (bestNodeId == null || search.cost(nodeId) < bestSearch.cost(bestNodeId))) {
                            bestHullModId = hullModId;
                            bestNodeId = nodeId;
                            bestSearch = search;
                        }
                    }
                }
                if (bestHullModId == null) {
                    break;
                }
                if (!allocatePath(bestSearch, bestNodeId, NpcBuildStep.CONVERTED_HULLMOD + bestHullModId,
                        NpcBuildStep.PATH_TO_CONVERTED_HULLMOD + bestHullModId)) {
                    rejectedNodeIds.add(bestNodeId);
                    continue;
                }
                convertedOp += opCost(bestHullModId);
                strippedHullModIds.add(bestHullModId);
                pendingHullModIds.remove(bestHullModId);
            }
            for (String hullModId : pendingHullModIds) {
                steps.add(new NpcBuildStep(equivalentsByHullMod.get(hullModId).get(0).getId(), NpcBuildStep.HULLMOD_KEPT + hullModId));
            }
            nodeBudget = Math.min(freeNodeCount + convertedOp / freedOp.opCostPerNode(), maxNodes);
        }

        private int opCost(String hullModId) {
            return Math.max(0, freedOp.hullModOpCosts().getOrDefault(hullModId, 0));
        }

        private Map<String, List<SkillNode>> equivalentNodesByHullMod(Set<String> removableHullModIds) {
            Map<String, List<SkillNode>> equivalentsByHullMod = new TreeMap<>();
            for (SkillNode node : topology.sortedById()) {
                String hullModId = node.getType().getEquivalentHullModId();
                SkillTier tier = node.getType().getTier();
                if (hullModId != null && removableHullModIds.contains(hullModId) && !tier.isWormhole() && tier != SkillTier.ROOT) {
                    equivalentsByHullMod.computeIfAbsent(hullModId, key -> new ArrayList<>()).add(node);
                }
            }
            return equivalentsByHullMod;
        }

        private Set<String> currentInstalled() {
            Set<String> remainingInstalled = new TreeSet<>(installedHullModIds);
            remainingInstalled.removeAll(strippedHullModIds);
            return remainingInstalled;
        }

        private void tasteFactionVolume() {
            if (factionRegion == null || remaining() <= 0) {
                return;
            }
            anchorAllocatedSocket();
            Search search = search(currentInstalled());
            List<String> candidates = new ArrayList<>();
            for (String nodeId : search.reached()) {
                SkillNode node = topology.node(nodeId);
                SkillTier tier = node.getType().getTier();
                if ((tier == SkillTier.NOTABLE || tier == SkillTier.KEYSTONE) && factionRegion.equals(node.getRegion())
                        && !rejectedNodeIds.contains(nodeId) && !wastedOnStation(search, nodeId) && affordable(search, nodeId, remaining())) {
                    candidates.add(nodeId);
                }
            }
            String goalNodeId = weightedPick(candidates, id -> nodeRelevance(search, id) * tierWeight(id), random);
            if (goalNodeId != null && !allocatePath(search, goalNodeId, NpcBuildStep.FACTION_GOAL, NpcBuildStep.PATH_TO_GOAL)) {
                rejectedNodeIds.add(goalNodeId);
            }
        }

        private void pursueGoals() {
            List<String> chosenGoals = new ArrayList<>();
            while (remaining() > 0 && slotsLeft() > 0) {
                Search search = search(currentInstalled());
                chosenGoals.removeIf(id -> !search.reaches(id));
                List<String> affordableGoals = affordableGoals(search, chosenGoals);
                if (affordableGoals.isEmpty()) {
                    drawGoals(search, chosenGoals);
                    affordableGoals = affordableGoals(search, chosenGoals);
                }
                if (!affordableGoals.isEmpty()) {
                    String goalNodeId = affordableGoals.get(0);
                    for (String nodeId : affordableGoals) {
                        if (search.cost(nodeId) < search.cost(goalNodeId)) {
                            goalNodeId = nodeId;
                        }
                    }
                    chosenGoals.remove(goalNodeId);
                    if (!allocatePath(search, goalNodeId, NpcBuildStep.GOAL, NpcBuildStep.PATH_TO_GOAL)) {
                        rejectedNodeIds.add(goalNodeId);
                    }
                } else if (!fillOne(search)) {
                    return;
                }
            }
        }

        private List<String> affordableGoals(Search search, List<String> chosenGoals) {
            List<String> affordableNodeIds = new ArrayList<>();
            for (String nodeId : chosenGoals) {
                if (affordable(search, nodeId, remaining())) {
                    affordableNodeIds.add(nodeId);
                }
            }
            return affordableNodeIds;
        }

        private void drawGoals(Search search, List<String> chosenGoals) {
            List<String> goalPool = new ArrayList<>();
            for (String nodeId : search.reached()) {
                SkillTier tier = topology.node(nodeId).getType().getTier();
                if ((tier == SkillTier.NOTABLE || tier == SkillTier.KEYSTONE) && !chosenGoals.contains(nodeId) && !rejectedNodeIds.contains(nodeId)
                        && !wastedOnStation(search, nodeId) && affordable(search, nodeId, remaining())) {
                    goalPool.add(nodeId);
                }
            }
            int coveredCost = 0;
            while (coveredCost < remaining() && !goalPool.isEmpty()) {
                String goalNodeId = weightedPick(goalPool, id -> nodeRelevance(search, id) * tierWeight(id), random);
                goalPool.remove(goalNodeId);
                chosenGoals.add(goalNodeId);
                coveredCost += search.cost(goalNodeId);
            }
        }

        private boolean fillOne(Search search) {
            int nearestCost = Integer.MAX_VALUE;
            List<String> candidates = new ArrayList<>();
            for (String nodeId : search.reached()) {
                if (!isFillTier(topology.node(nodeId).getType().getTier()) || rejectedNodeIds.contains(nodeId) || wastedOnStation(search, nodeId)
                        || !affordable(search, nodeId, remaining())) {
                    continue;
                }
                int nodeCost = search.cost(nodeId);
                if (nodeCost < nearestCost) {
                    nearestCost = nodeCost;
                    candidates.clear();
                }
                if (nodeCost == nearestCost) {
                    candidates.add(nodeId);
                }
            }
            String pickedNodeId = weightedPick(candidates, id -> nodeRelevance(search, id), random);
            if (pickedNodeId == null) {
                return false;
            }
            if (!allocatePath(search, pickedNodeId, NpcBuildStep.ALLOCATED, NpcBuildStep.PATH_TO_GOAL)) {
                rejectedNodeIds.add(pickedNodeId);
            }
            return true;
        }

        private static boolean isFillTier(SkillTier tier) {
            return tier == SkillTier.SMALL || tier == SkillTier.ROOT;
        }

        private boolean wastedOnStation(Search search, String nodeId) {
            return NpcStationNodes.isWasted(AllocatedNode.planned(topology.node(nodeId), search.options().get(nodeId)).effectiveType(), profile);
        }

        private float tierWeight(String nodeId) {
            return topology.node(nodeId).getType().getTier() == SkillTier.KEYSTONE ? KEYSTONE_WEIGHT : NOTABLE_WEIGHT;
        }

        private double nodeRelevance(Search search, String nodeId) {
            SkillNode node = topology.node(nodeId);
            return relevance.of(node.effectiveTags(search.options().get(nodeId)));
        }

        private Search search(Set<String> fitInstalled) {
            State fitState = state(fitInstalled);
            Map<String, SkillType> searchOptions = new HashMap<>();
            return Search.of(TreeSearch.from(topology, new TreeSet<>(shipData.getAllocatedNodeIds()),
                    candidate -> traversable(candidate, fitState, searchOptions),
                    this::jumpTarget), fitInstalled, searchOptions);
        }

        private State state(Set<String> fitInstalled) {
            List<AllocatedNode> allocatedNodes = AllocatedNode.of(shipData);
            ShieldType shieldType = NodeEligibility.currentShieldType(shipData, profile.hullSize(), profile.shieldType());
            int fighterBays = profile.fighterBays();
            for (String hullModId : hullMods.installed()) {
                if (!fitInstalled.contains(hullModId)) {
                    fighterBays -= freedOp.hullModFighterBays().getOrDefault(hullModId, 0);
                }
            }
            ShipProfile fittedProfile = new ShipProfile(profile.hullSize(), shieldType, Math.max(0, fighterBays), profile.weaponKinds(),
                    profile.flagship(), profile.baseArmor(), profile.phaseHull(), profile.limitedSystemCharges(), profile.onlyBuiltInWings(),
                    profile.station());
            boolean shieldInvested = false;
            for (AllocatedNode node : allocatedNodes) {
                List<String> tags = node.effectiveType().getTags();
                shieldInvested |= tags.contains(SHIELD_THEME) || tags.contains(SHIELD_REQUIREMENT);
            }
            ShipFacts fittedFacts = fittedProfile.facts(fitInstalled::contains, hullId,
                    hullModId -> fitInstalled.contains(hullModId) && hullMods.permanent().contains(hullModId));
            NodeEligibility.Context eligibility = new NodeEligibility.Context(allocatedNodes, shieldType, fittedFacts,
                    lockedWormhole, fittedProfile);
            AllocationGate.Budget budget = new AllocationGate.Budget(Integer.MAX_VALUE, freedOp.opCostPerNode(), maxNodes + 1);
            return new State(new AllocationGate(shipData, topology, ShipTrees.resolveStartingRootId(shipData), budget, eligibility), shieldInvested);
        }

        private boolean traversable(SkillNode node, State fitState, Map<String, SkillType> searchOptions) {
            if (shipData.isAllocated(node.getId()) || !regionAllowed(node.getRegion())) {
                return false;
            }
            if (node.getType().getTier().isWormhole()) {
                return false;
            }
            SkillType chosenOption = option(node, fitState, searchOptions);
            if (node.getType().isOptional() && chosenOption == null) {
                return false;
            }
            return stillLegal(node, chosenOption, fitState);
        }

        private boolean stillLegal(SkillNode node, SkillType chosenOption, State fitState) {
            if (fitState.shieldInvested() && removesShield(AllocatedNode.planned(node, chosenOption).effectiveType())) {
                return false;
            }
            return usable(node, chosenOption, fitState);
        }

        private boolean usable(SkillNode node, SkillType chosenOption, State fitState) {
            return fitState.gate().eligibility(node, chosenOption).allowed();
        }

        private boolean placeable(PathStep step, State fitState) {
            return stillLegal(step.node(), step.option(), fitState) && fitState.gate().allocation(step.node(), step.option()).allowed();
        }

        private boolean removesShield(SkillType skillType) {
            for (SkillTypeEffect effect : skillType.effectsFor(profile.hullSize())) {
                if (effect.effect() == ShieldSkillEffect.REMOVE_SHIELD) {
                    return true;
                }
            }
            return false;
        }

        private boolean regionAllowed(String region) {
            return region == null || SkillTags.CORE_REGION.equals(region) || region.equals(factionRegion);
        }

        private SkillType option(SkillNode node, State fitState, Map<String, SkillType> searchOptions) {
            if (!node.getType().isOptional()) {
                return null;
            }
            SkillType searchOption = searchOptions.get(node.getId());
            if (searchOption != null) {
                return searchOption;
            }
            SkillType preferredOption = chosenOptions.get(node.getId());
            if (preferredOption != null && usable(node, preferredOption, fitState)) {
                searchOptions.put(node.getId(), preferredOption);
                return preferredOption;
            }
            List<SkillType> options = new ArrayList<>();
            for (String optionId : node.getType().getOptionalOptionIds()) {
                SkillType optionType = SkillTree.getType(optionId);
                if (optionType != null && optionSkipReason(node.getType(), optionId) == null
                        && optionType.allowsHullSize(profile.hullSize()) && usable(node, optionType, fitState)) {
                    options.add(optionType);
                }
            }
            if (options.stream().anyMatch(optionType -> !NpcStationNodes.isWasted(optionType, profile))) {
                options.removeIf(optionType -> NpcStationNodes.isWasted(optionType, profile));
            }
            SkillType rootAffinityOption = rootAffinityOption(options);
            SkillType pickedOption = rootAffinityOption != null ? rootAffinityOption
                    : weightedPick(options, optionType -> relevance.of(optionType.getTags()), random);
            if (pickedOption != null) {
                searchOptions.put(node.getId(), pickedOption);
                chosenOptions.put(node.getId(), pickedOption);
            }
            return pickedOption;
        }

        private SkillType rootAffinityOption(List<SkillType> options) {
            SkillNode root = topology.node(ShipTrees.resolveStartingRootId(shipData));
            Set<SkillEffect> rootAffinities = root == null ? Set.of() : affinitiesOf(root.getType());
            for (SkillType option : options) {
                if (!Collections.disjoint(affinitiesOf(option), rootAffinities)) {
                    return option;
                }
            }
            return null;
        }

        private static Set<SkillEffect> affinitiesOf(SkillType type) {
            Set<SkillEffect> affinities = new HashSet<>();
            for (SkillTypeEffect effect : type.getEffects()) {
                if (AffinitySteps.isAffinity(effect.effect())) {
                    affinities.add(effect.effect());
                }
            }
            return affinities;
        }

        private void anchorAllocatedSocket() {
            if (factionExitId == null || anchorSocketId != null || slotsLeft() <= 0) {
                return;
            }
            String socketId = emptyAllocatedSocket();
            if (socketId != null && socketAnchor(socketId)) {
                steps.add(new NpcBuildStep(factionExitId, NpcBuildStep.WORMHOLE_EXIT));
            }
        }

        private boolean socketAnchor(String socketId) {
            shipData.socketItem(socketId, factionAnchorItemId);
            if (factionExitId.equals(ShipTreeLinks.anchorExit(shipData, socketId))) {
                anchorSocketId = socketId;
                ShipTrees.openAnchorWormholes(shipData, maxNodes + 1);
                if (shipData.isAllocated(factionExitId)) {
                    return true;
                }
            }
            releaseAnchor(socketId);
            return false;
        }

        private void releaseAnchor(String socketId) {
            shipData.unsocketItem(socketId);
            SkillNode exit = topology.node(factionExitId);
            if (exit != null && shipData.isAllocated(factionExitId)) {
                ShipTrees.deallocate(shipData, exit);
            }
            anchorSocketId = null;
        }

        private void closeUnusedFactionVolume() {
            if (factionExitId == null || allocatesFactionVolumeNode()) {
                return;
            }
            if (anchorSocketId != null) {
                steps.removeIf(step -> step.nodeId().equals(factionExitId) && NpcBuildStep.WORMHOLE_EXIT.equals(step.outcome()));
                steps.add(new NpcBuildStep(factionExitId, NpcBuildStep.ANCHOR_REMOVED));
                releaseAnchor(anchorSocketId);
            }
            factionExitId = null;
        }

        private boolean allocatesFactionVolumeNode() {
            for (String nodeId : shipData.getAllocatedNodeIds()) {
                SkillNode node = topology.node(nodeId);
                if (node != null && factionRegion.equals(node.getRegion()) && node.getType().getTier() != SkillTier.WORMHOLE_EXIT) {
                    return true;
                }
            }
            return false;
        }

        private String jumpTarget(SkillNode node) {
            String partnerId = ShipTreeLinks.wormholePartner(shipData, node);
            if (partnerId != null) {
                return partnerId;
            }
            return canTakeFactionAnchor(node) ? factionExitId : null;
        }

        private boolean canTakeFactionAnchor(SkillNode socket) {
            return factionExitId != null && socket.getType().getTier() == SkillTier.SOCKET && anchorSocketId == null
                    && shipData.getSocketedItem(socket.getId()) == null && !shipData.isAllocated(factionExitId);
        }

        private int pathSize(Search search, String targetId) {
            int pathLength = 0;
            String pathNodeId = targetId;
            while (pathNodeId != null && !shipData.isAllocated(pathNodeId)) {
                pathLength++;
                pathNodeId = search.parent(pathNodeId);
            }
            return pathLength;
        }

        private List<PathStep> path(Search search, String targetId) {
            List<PathStep> pathSteps = new ArrayList<>();
            String pathNodeId = targetId;
            while (pathNodeId != null && !shipData.isAllocated(pathNodeId)) {
                SkillNode node = topology.node(pathNodeId);
                pathSteps.add(new PathStep(node, search.options().get(pathNodeId), search.isExit(pathNodeId)));
                pathNodeId = search.parent(pathNodeId);
            }
            Collections.reverse(pathSteps);
            return pathSteps;
        }

        private boolean allocatePath(Search search, String targetId, String goalOutcome, String pathOutcome) {
            List<PathStep> pathSteps = path(search, targetId);
            if (hasInternalConflict(pathSteps)) {
                return false;
            }
            int stepsBefore = steps.size();
            List<SkillNode> placedNodes = new ArrayList<>();
            boolean anchoredInPath = false;
            for (int i = 0; i < pathSteps.size(); i++) {
                PathStep step = pathSteps.get(i);
                String outcome = i == pathSteps.size() - 1 ? goalOutcome : pathOutcome;
                if (step.wormholeExit()) {
                    if (!shipData.isAllocated(step.node().getId())) {
                        if (!socketAnchor(search.parent(step.node().getId()))) {
                            undoPath(placedNodes, stepsBefore, anchoredInPath);
                            return false;
                        }
                        anchoredInPath = true;
                    }
                    steps.add(new NpcBuildStep(step.node().getId(), NpcBuildStep.WORMHOLE_EXIT));
                    continue;
                }
                if (!placedNodes.isEmpty() && !placeable(step, state(search.fitInstalled()))) {
                    undoPath(placedNodes, stepsBefore, anchoredInPath);
                    return false;
                }
                placedNodes.add(step.node());
                if (step.option() != null) {
                    ShipTrees.selectOption(shipData, step.node(), step.option(), freedOp.opCostPerNode());
                } else {
                    ShipTrees.allocate(shipData, step.node(), freedOp.opCostPerNode());
                }
                nodesSpent++;
                steps.add(new NpcBuildStep(step.node().getId(), nodesSpent > freeNodeCount ? outcome + NpcBuildStep.FREED_OP_SUFFIX : outcome));
            }
            return true;
        }

        private void undoPath(List<SkillNode> placedNodes, int stepsBefore, boolean anchoredInPath) {
            for (int i = placedNodes.size() - 1; i >= 0; i--) {
                ShipTrees.deallocate(shipData, placedNodes.get(i));
                nodesSpent--;
            }
            if (anchoredInPath && anchorSocketId != null) {
                releaseAnchor(anchorSocketId);
            }
            steps.subList(stepsBefore, steps.size()).clear();
        }

        private static boolean hasInternalConflict(List<PathStep> pathSteps) {
            for (int i = 0; i < pathSteps.size(); i++) {
                for (int j = i + 1; j < pathSteps.size(); j++) {
                    PathStep earlierStep = pathSteps.get(i);
                    PathStep laterStep = pathSteps.get(j);
                    if (AllocatedNode.planned(earlierStep.node(), earlierStep.option()).isExclusiveWith(AllocatedNode.planned(laterStep.node(), laterStep.option()))) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
