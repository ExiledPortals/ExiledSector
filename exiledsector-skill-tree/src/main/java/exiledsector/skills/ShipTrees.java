package exiledsector.skills;

import exiledsector.items.DoctrineOverrides;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ShipTrees {

    private ShipTrees() {
    }

    public static void selectOption(ShipSkillData data, SkillNode node, SkillType chosenOption, int opCost) {
        data.revision++;
        if (!data.isAllocated(node.getId())) {
            ItemCostLedger.discard(data, node.getId());
            data.charge(node.getId(), opCost);
        }
        data.allocatedNodeIds.add(node.getId());
        if (data.choiceSelections == null) data.choiceSelections = new LinkedHashMap<>();
        data.choiceSelections.put(node.getId(), chosenOption.getId());
    }

    public static String resolveStartingRootId(ShipSkillData data) {
        if (data.startingRootId == null) {
            data.startingRootId = firstAllocatedRootId(data, SkillTree.topology().rootIds());
        }
        return data.startingRootId;
    }

    public static String resolveStartingRootId(ShipSkillData data, Collection<SkillNode> allNodes) {
        if (data.startingRootId == null) {
            data.startingRootId = firstAllocatedRootId(data, SkillTreeTopology.rootIdsOf(allNodes));
        }
        return data.startingRootId;
    }

    private static String firstAllocatedRootId(ShipSkillData data, Set<String> rootIds) {
        for (String allocatedId : data.allocatedNodeIds) {
            if (rootIds.contains(allocatedId)) {
                return allocatedId;
            }
        }
        return null;
    }

    public static boolean chooseStartingRoot(ShipSkillData data, SkillNode root) {
        data.revision++;
        if (data.startingRootId != null || root.getType().getTier() != SkillTier.ROOT) {
            return false;
        }
        data.startingRootId = root.getId();
        allocate(data, root, 0);
        return true;
    }

    public static boolean unchooseStartingRoot(ShipSkillData data) {
        data.revision++;
        if (!data.canUnchooseStartingRoot()) {
            return false;
        }
        ItemCostLedger.ensure(data);
        data.allocatedNodeIds.remove(data.startingRootId);
        data.release(data.startingRootId);
        data.startingRootId = null;
        return true;
    }

    public static void allocateGranted(ShipSkillData data, SkillNode node) {
        boolean rewritten = DoctrineOverrides.replacementType(data, node) != null;
        data.revision++;
        String nodeId = node.getId();
        if (rewritten) {
            data.replacementAllocatedNodeIds().add(nodeId);
        }
        ItemCostLedger.discard(data, nodeId);
        data.dormantNodeIds().remove(nodeId);
        data.allocatedNodeIds.add(nodeId);
        data.subroutineGrantedNodeIds().add(nodeId);
    }

    public static void allocateFleetGrant(ShipSkillData data, SkillNode node) {
        allocateGranted(data, node);
        data.fleetGrantNodeIds().add(node.getId());
    }

    public static boolean isFleetGrantable(SkillNode node) {
        SkillTier tier = node.getType().getTier();
        return !tier.isWormhole() && tier != SkillTier.ROOT && tier != SkillTier.SOCKET;
    }

    public static List<String> dropFleetGrants(ShipSkillData data) {
        if (data.fleetGrantNodeIds == null || data.fleetGrantNodeIds.isEmpty()) {
            return List.of();
        }
        data.fleetGrantNodeIds.clear();
        return releaseUnreachable(data, SkillTree.topology(), resolveStartingRootId(data)).nodeIds();
    }

    private static List<String> ungrantedFreeNodeIds(ShipSkillData data) {
        if (data.subroutineGrantedNodeIds == null || data.subroutineGrantedNodeIds.isEmpty()) {
            return List.of();
        }
        Set<String> granted = ShipTreeLinks.grantedNodeIds(data);
        if (data.fleetGrantNodeIds != null) {
            granted.addAll(data.fleetGrantNodeIds);
        }
        if (data.socketedItems != null && data.dormantNodeIds != null) {
            NodeGrants.GrantLookup lookup = NodeGrants.lookup();
            data.socketedItems.forEach((socketId, itemId) -> {
                if (data.dormantNodeIds.contains(socketId)) {
                    granted.addAll(lookup.grantedNodeIds(itemId));
                }
            });
        }
        List<String> ungranted = new ArrayList<>();
        for (String nodeId : data.subroutineGrantedNodeIds) {
            if (data.allocatedNodeIds.contains(nodeId) && !granted.contains(nodeId)) {
                ungranted.add(nodeId);
            }
        }
        return ungranted;
    }

    public static ReleasedNodes releaseUngrantedFreeNodes(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId) {
        return releaseUngrantedFreeNodes(data, topology, satisfiedRootId, Set.of());
    }

    public static ReleasedNodes releaseUngrantedFreeNodes(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId, Set<String> alsoReleasable) {
        List<String> ungranted = ungrantedFreeNodeIds(data);
        if (ungranted.isEmpty() && alsoReleasable.isEmpty()) {
            return ReleasedNodes.NONE;
        }
        Set<String> reachableBefore = reachableAllocatedNodeIds(data, topology, satisfiedRootId, Set.of());
        reachableBefore.addAll(ungranted);
        reachableBefore.addAll(alsoReleasable);
        return releaseNewlyStranded(data, topology, satisfiedRootId, reachableBefore);
    }

    public static Set<String> pathReachableAllocatedNodeIds(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId) {
        return pathReachableAllocatedNodeIds(data, topology, satisfiedRootId, Set.of());
    }

    public static Set<String> grantOnlyNodeIds(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId) {
        if (ShipTreeLinks.grantedNodesBySocket(data).isEmpty() && (data.fleetGrantNodeIds == null || data.fleetGrantNodeIds.isEmpty())) {
            return Set.of();
        }
        Set<String> pathReached = pathReachableAllocatedNodeIds(data, topology, satisfiedRootId, Set.of());
        Set<String> grantOnly = new LinkedHashSet<>(grantedReachableNodeIds(data, topology, pathReached, Set.of()));
        grantOnly.removeAll(pathReached);
        return grantOnly;
    }

    public static List<String> openAnchorWormholes(ShipSkillData data, int maxAllocatedNodes) {
        List<String> openedExitIds = new ArrayList<>();
        AnchorLinks.ExitLookup lookup = AnchorLinks.lookup();
        for (Map.Entry<String, String> link : List.copyOf(ShipTreeLinks.anchorExitsBySocket(data).entrySet())) {
            String exitId = link.getValue();
            if (!data.isAllocated(exitId) && data.usedNodeSlots() < maxAllocatedNodes && canOpen(data, link.getKey(), lookup)) {
                data.revision++;
                ItemCostLedger.discard(data, exitId);
                data.dormantNodeIds().remove(exitId);
                data.allocatedNodeIds.add(exitId);
                data.pairGrantedNodeIds().add(exitId);
                openedExitIds.add(exitId);
            }
        }
        return openedExitIds;
    }

    private static boolean canOpen(ShipSkillData data, String socketId, AnchorLinks.ExitLookup lookup) {
        String itemId = data.getSocketedItem(socketId);
        return itemId != null && lookup.canOpen(itemId);
    }

    public static int getSpentOp(ShipSkillData data, int opCostPerNode) {
        return paidNodeCount(data) * opCostPerNode;
    }

    public static int paidNodeCount(ShipSkillData data) {
        String startingRoot = resolveStartingRootId(data);
        int paid = 0;
        for (String nodeId : data.allocatedNodeIds) {
            if (!data.isFreeNode(nodeId) && !nodeId.equals(startingRoot)) {
                paid++;
            }
        }
        return paid;
    }

    public static void refundBankedFreeAllocationAsOp(ShipSkillData data, Collection<SkillNode> allNodes) {
        if (data.bankedFreeAllocations <= 0) {
            return;
        }
        data.bankedFreeAllocations--;
        if (!convertMostRecentAllocationToFree(data, allNodes)) {
            data.bankedFreeAllocations++;
        }
    }

    public static ShipSkillData planningCopy(ShipSkillData data) {
        ShipSkillData copy = new ShipSkillData();
        copy.startingRootId = resolveStartingRootId(data);
        copy.allocatedNodeIds.addAll(data.allocatedNodeIds);
        if (data.choiceSelections != null) {
            copy.choiceSelections.putAll(data.choiceSelections);
        }
        copy.subroutineGrantedNodeIds = data.subroutineGrantedNodeIds == null ? null : new LinkedHashSet<>(data.subroutineGrantedNodeIds);
        copy.fleetGrantNodeIds = data.fleetGrantNodeIds == null ? null : new LinkedHashSet<>(data.fleetGrantNodeIds);
        copy.replacementAllocatedNodeIds = data.replacementAllocatedNodeIds == null ? null : new LinkedHashSet<>(data.replacementAllocatedNodeIds);
        copy.mirrorSocketedItems(data);
        return copy;
    }

    private static ShipSkillData releaseCopy(ShipSkillData data) {
        ShipSkillData copy = planningCopy(data);
        copy.freeNodeIds = new LinkedHashSet<>(data.freeNodeIds());
        copy.pairGrantedNodeIds = new LinkedHashSet<>(data.pairGrantedNodeIds());
        copy.bankedFreeAllocations = data.bankedFreeAllocations;
        copy.paidItemCostIds = new LinkedHashMap<>();
        copy.paidItemCostQuantities = new LinkedHashMap<>();
        copy.npcBuild = data.npcBuild;
        return copy;
    }

    public static Set<String> previewRelease(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId, Consumer<ShipSkillData> change) {
        ShipSkillData copy = releaseCopy(data);
        Set<String> reachableBefore = reachableAllocatedNodeIds(copy, topology, satisfiedRootId);
        change.accept(copy);
        releaseNewlyStranded(copy, topology, satisfiedRootId, reachableBefore);
        Set<String> released = new LinkedHashSet<>(data.allocatedNodeIds);
        released.removeAll(copy.allocatedNodeIds);
        return released;
    }

    public static int paidNodeCount(ShipSkillData data, Collection<String> nodeIds) {
        String startingRoot = resolveStartingRootId(data);
        int paid = 0;
        for (String nodeId : nodeIds) {
            if (data.isAllocated(nodeId) && !data.isFreeNode(nodeId) && !nodeId.equals(startingRoot)) {
                paid++;
            }
        }
        return paid;
    }

    public static Set<String> reachableAllocatedNodeIds(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId) {
        return reachableAllocatedNodeIds(data, topology, satisfiedRootId, Set.of());
    }

    public static ReleasedNodes release(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId, Collection<SkillNode> nodes) {
        return releaseWithStranded(data, topology, satisfiedRootId, () -> nodes.forEach(node -> releaseNode(data, node)));
    }

    public static ReleasedNodes releaseWithStranded(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId, Runnable deallocation) {
        Set<String> allocatedBefore = new LinkedHashSet<>(data.allocatedNodeIds);
        Set<String> reachableBefore = reachableAllocatedNodeIds(data, topology, satisfiedRootId);
        deallocation.run();
        if (data.allocatedNodeIds.containsAll(allocatedBefore)) {
            return ReleasedNodes.NONE;
        }
        deallocateUnreachable(data, topology, satisfiedRootId, node -> reachableBefore.contains(node.getId()));
        return takeReleased(data, allocatedBefore);
    }

    private static void releaseNode(ShipSkillData data, SkillNode node) {
        if (ShipTreeLinks.anchorSocket(data, node.getId()) != null) {
            deallocateAnchorExit(data, node.getId());
        } else if (data.isAllocated(node.getId())) {
            deallocate(data, node);
        }
    }

    public static ReleasedNodes releaseNewlyStranded(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId, Set<String> reachableBefore) {
        return releaseUnreachable(data, topology, satisfiedRootId, node -> reachableBefore.contains(node.getId()));
    }

    public static ReleasedNodes releaseUnreachable(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId) {
        return releaseUnreachable(data, topology, satisfiedRootId, node -> true);
    }

    public static ReleasedNodes releaseUnreachable(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId, Predicate<SkillNode> releasable) {
        Set<String> allocatedBefore = new LinkedHashSet<>(data.allocatedNodeIds);
        deallocateUnreachable(data, topology, satisfiedRootId, releasable);
        return takeReleased(data, allocatedBefore);
    }

    private static ReleasedNodes takeReleased(ShipSkillData data, Set<String> allocatedBefore) {
        allocatedBefore.removeAll(data.allocatedNodeIds);
        if (allocatedBefore.isEmpty()) {
            return ReleasedNodes.NONE;
        }
        return new ReleasedNodes(List.copyOf(allocatedBefore), ItemCostLedger.takePaidItemCosts(data, allocatedBefore));
    }

    private static void deallocateUnreachable(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId, Predicate<SkillNode> releasable) {
        if (satisfiedRootId == null || !data.isAllocated(satisfiedRootId)) {
            return;
        }
        for (String nodeId : ungrantedFreeNodeIds(data)) {
            SkillNode node = topology.node(nodeId);
            if (node != null && releasable.test(node)) {
                deallocate(data, node);
            }
        }
        Set<String> reachable = reachableAllocatedNodeIds(data, topology, satisfiedRootId, Set.of());
        for (String nodeId : List.copyOf(data.allocatedNodeIds)) {
            SkillNode node = topology.node(nodeId);
            if (node != null && data.isAllocated(nodeId) && !reachable.contains(nodeId) && !nodeId.equals(satisfiedRootId) && releasable.test(node)) {
                deallocate(data, node);
            }
        }
    }

    public static List<String> trimPaidNodes(ShipSkillData data, Map<String, SkillNode> tree, SkillTreeTopology topology) {
        data.revision++;
        String startingRoot = resolveStartingRootId(data, tree.values());
        List<String> removedNodeIds = new ArrayList<>();
        while (paidNodeCount(data) > 0) {
            if (data.bankedFreeAllocations > 0 && convertMostRecentAllocationToFree(data, tree.values())) {
                data.bankedFreeAllocations--;
                continue;
            }
            SkillNode removable = newestRemovableNode(data, tree, topology, startingRoot);
            if (removable == null) {
                break;
            }
            deallocate(data, removable);
            removedNodeIds.add(removable.getId());
        }
        return removedNodeIds;
    }

    public static List<String> trimFreeAllocationsToLevel(ShipSkillData data, Map<String, SkillNode> tree, SkillTreeTopology topology) {
        data.revision++;
        String startingRoot = resolveStartingRootId(data, tree.values());
        List<String> removedNodeIds = new ArrayList<>();
        data.bankedFreeAllocations = Math.max(0, Math.min(data.bankedFreeAllocations, data.level - data.freeNodeIds().size()));
        while (data.freeNodeIds().size() > data.level) {
            SkillNode removable = newestRemovableNode(data, tree, topology, startingRoot, data.freeNodeIds()::contains);
            if (removable == null) {
                break;
            }
            deallocate(data, removable);
            data.bankedFreeAllocations = Math.max(0, data.bankedFreeAllocations - 1);
            removedNodeIds.add(removable.getId());
        }
        return removedNodeIds;
    }

    private static SkillNode newestRemovableNode(ShipSkillData data, Map<String, SkillNode> tree, SkillTreeTopology topology, String startingRoot) {
        return newestRemovableNode(data, tree, topology, startingRoot, nodeId -> true);
    }

    private static SkillNode newestRemovableNode(ShipSkillData data, Map<String, SkillNode> tree, SkillTreeTopology topology, String startingRoot,
                                                 Predicate<String> candidate) {
        List<String> order = new ArrayList<>(data.allocatedNodeIds);
        for (int i = order.size() - 1; i >= 0; i--) {
            String nodeId = order.get(i);
            SkillNode node = tree.get(nodeId);
            if (node != null && candidate.test(nodeId) && !nodeId.equals(startingRoot) && !data.pairGrantedNodeIds().contains(nodeId)
                    && ShipTreeLinks.anchorPartner(data, nodeId) == null && !ShipTreeLinks.isGrantHeld(data, nodeId)
                    && !ShipTreeLinks.grantedNodesBySocket(data).containsKey(nodeId)
                    && canDeallocate(data, node, topology, startingRoot)) {
                return node;
            }
        }
        return null;
    }

    public static boolean convertMostRecentAllocationToFree(ShipSkillData data, Collection<SkillNode> allNodes) {
        data.revision++;
        String startingRoot = resolveStartingRootId(data, allNodes);
        List<String> order = new ArrayList<>(data.allocatedNodeIds);
        for (int i = order.size() - 1; i >= 0; i--) {
            String nodeId = order.get(i);
            boolean alreadyFree = data.isFreeNode(nodeId);
            if (alreadyFree || nodeId.equals(startingRoot)) {
                continue;
            }
            data.freeNodeIds().add(nodeId);
            return true;
        }
        return false;
    }

    public static void allocate(ShipSkillData data, SkillNode node, SkillType option, int opCost) {
        boolean rewritten = DoctrineOverrides.replacementType(data, node) != null;
        SkillType storedOption = option != null || !rewritten ? option : firstOption(node.getType());
        if (storedOption != null) {
            selectOption(data, node, storedOption, opCost);
        } else {
            allocate(data, node, opCost);
        }
        if (rewritten) {
            data.replacementAllocatedNodeIds().add(node.getId());
        }
    }

    private static SkillType firstOption(SkillType type) {
        for (String optionId : type.getChoiceOptionIds()) {
            SkillType option = SkillTree.getType(optionId);
            if (option != null) {
                return option;
            }
        }
        return null;
    }

    public static void allocate(ShipSkillData data, SkillNode node, int opCost) {
        data.revision++;
        ItemCostLedger.discard(data, node.getId());
        data.allocatedNodeIds.add(node.getId());
        data.charge(node.getId(), opCost);

        String pairedId = ShipTreeLinks.wormholePartner(data, node);
        if (pairedId != null && !data.isAllocated(pairedId)) {
            ItemCostLedger.discard(data, pairedId);
            data.allocatedNodeIds.add(pairedId);
            data.pairGrantedNodeIds().add(pairedId);
        }
    }

    public static void deallocate(ShipSkillData data, SkillNode node) {
        String pairedId = ShipTreeLinks.wormholePartner(data, node);
        data.revision++;
        ItemCostLedger.ensure(data);
        data.allocatedNodeIds.remove(node.getId());
        data.release(node.getId());

        if (pairedId != null && data.allocatedNodeIds.remove(pairedId)) {
            data.release(pairedId);
        }
    }

    private static boolean deallocateAnchorExit(ShipSkillData data, String exitNodeId) {
        if (!data.allocatedNodeIds.contains(exitNodeId)) {
            return false;
        }
        data.revision++;
        ItemCostLedger.ensure(data);
        data.allocatedNodeIds.remove(exitNodeId);
        data.release(exitNodeId);
        return true;
    }

    public static boolean replaceNode(ShipSkillData data, String oldId, String newId) {
        data.revision++;
        boolean dormant = data.getDormantNodeIds().contains(oldId);
        if ((!dormant && !data.allocatedNodeIds.contains(oldId)) || data.allocatedNodeIds.contains(newId) || data.getDormantNodeIds().contains(newId)) {
            return false;
        }
        ItemCostLedger.ensure(data);
        Set<String> holdingNodeIds = dormant ? data.dormantNodeIds : data.allocatedNodeIds;
        List<String> order = new ArrayList<>(holdingNodeIds);
        holdingNodeIds.clear();
        order.forEach(id -> holdingNodeIds.add(id.equals(oldId) ? newId : id));
        if (data.freeNodeIds().remove(oldId)) {
            data.freeNodeIds().add(newId);
        }
        if (data.pairGrantedNodeIds().remove(oldId)) {
            data.pairGrantedNodeIds().add(newId);
        }
        if (data.subroutineGrantedNodeIds != null && data.subroutineGrantedNodeIds.remove(oldId)) {
            data.subroutineGrantedNodeIds.add(newId);
        }
        if (data.replacementAllocatedNodeIds != null && data.replacementAllocatedNodeIds.remove(oldId)) {
            data.replacementAllocatedNodeIds.add(newId);
        }
        if (oldId.equals(data.startingRootId)) {
            data.startingRootId = newId;
        }
        SkillItemCost movedCharge = ItemCostLedger.takePaidItemCost(data, oldId);
        if (movedCharge != null) {
            ItemCostLedger.recordPaidItemCost(data, newId, movedCharge);
        }
        carryOptionOver(data, oldId, newId);
        data.unsocketItem(oldId);
        return true;
    }

    public static boolean renameNode(ShipSkillData data, String oldId, String newId) {
        if (!holdsNode(data, oldId) || data.allocatedNodeIds.contains(newId) || data.getDormantNodeIds().contains(newId)) {
            return false;
        }
        data.revision++;
        ItemCostLedger.ensure(data);
        renameIn(data.allocatedNodeIds, oldId, newId);
        renameIn(data.dormantNodeIds, oldId, newId);
        renameIn(data.freeNodeIds, oldId, newId);
        renameIn(data.pairGrantedNodeIds, oldId, newId);
        renameIn(data.subroutineGrantedNodeIds, oldId, newId);
        renameIn(data.fleetGrantNodeIds, oldId, newId);
        renameIn(data.replacementAllocatedNodeIds, oldId, newId);
        if (oldId.equals(data.startingRootId)) {
            data.startingRootId = newId;
        }
        renameKey(data.choiceSelections, oldId, newId);
        if (data.choiceSelections != null) {
            data.choiceSelections.computeIfPresent(newId, (nodeId, optionId) -> NodeReplacements.currentTypeId(optionId));
        }
        renameKey(data.socketedItems, oldId, newId);
        renameKey(data.paidItemCostIds, oldId, newId);
        renameKey(data.paidItemCostQuantities, oldId, newId);
        return true;
    }

    private static boolean holdsNode(ShipSkillData data, String nodeId) {
        return data.allocatedNodeIds.contains(nodeId) || data.getDormantNodeIds().contains(nodeId) || data.isFreeNode(nodeId)
                || data.isFleetGranted(nodeId) || nodeId.equals(data.startingRootId) || data.getChoiceSelection(nodeId) != null
                || data.getSocketedItem(nodeId) != null || (data.paidItemCostIds != null && data.paidItemCostIds.containsKey(nodeId))
                || (data.replacementAllocatedNodeIds != null && data.replacementAllocatedNodeIds.contains(nodeId));
    }

    private static void renameIn(Set<String> nodeIds, String oldId, String newId) {
        if (nodeIds == null || !nodeIds.contains(oldId)) {
            return;
        }
        List<String> order = new ArrayList<>(nodeIds);
        nodeIds.clear();
        order.forEach(id -> nodeIds.add(id.equals(oldId) ? newId : id));
    }

    private static <V> void renameKey(Map<String, V> byNodeId, String oldId, String newId) {
        if (byNodeId == null || !byNodeId.containsKey(oldId)) {
            return;
        }
        Map<String, V> order = new LinkedHashMap<>(byNodeId);
        byNodeId.clear();
        order.forEach((id, value) -> byNodeId.put(id.equals(oldId) ? newId : id, value));
    }

    private static void carryOptionOver(ShipSkillData data, String oldId, String newId) {
        String oldOptionId = data.choiceSelections == null ? null : NodeReplacements.currentTypeId(data.choiceSelections.remove(oldId));
        SkillNode replacement = SkillTree.getDeclared(newId);
        if (replacement == null || !replacement.getType().isChoice()) {
            return;
        }
        SkillType option = oldOptionId != null && replacement.getType().getChoiceOptionIds().contains(oldOptionId) ? SkillTree.getType(oldOptionId)
                : firstOption(replacement.getType());
        if (option != null) {
            if (data.choiceSelections == null) data.choiceSelections = new LinkedHashMap<>();
            data.choiceSelections.put(newId, option.getId());
        }
    }

    public static List<String> forgetUnknownNodes(ShipSkillData data, Map<String, SkillNode> tree, Map<String, SkillType> types) {
        return forgetUnknownNodes(data, tree, tree, types);
    }

    public static List<String> forgetUnknownNodes(ShipSkillData data, Map<String, SkillNode> tree, Map<String, SkillNode> declaredTree, Map<String, SkillType> types) {
        data.revision++;
        ItemCostLedger.ensure(data);
        List<String> forgotten = new ArrayList<>();
        for (String nodeId : List.copyOf(data.getDormantNodeIds())) {
            SkillNode declaredNode = declaredTree.get(nodeId);
            if (declaredNode == null || hasInvalidOption(data, declaredNode, types)) {
                data.dormantNodeIds.remove(nodeId);
                data.release(nodeId);
                forgotten.add(nodeId);
            }
        }
        for (String nodeId : List.copyOf(data.allocatedNodeIds)) {
            SkillNode node = tree.get(nodeId);
            SkillNode declaredNode = declaredTree.get(nodeId);
            if (node == null && declaredNode != null && !hasInvalidOption(data, declaredNode, types)) {
                data.allocatedNodeIds.remove(nodeId);
                data.dormantNodeIds().add(nodeId);
            } else if (node == null || hasInvalidOption(data, node, types)) {
                data.allocatedNodeIds.remove(nodeId);
                data.release(nodeId);
                forgotten.add(nodeId);
            } else {
                if (!node.getType().isChoice() && data.choiceSelections != null) {
                    data.choiceSelections.remove(nodeId);
                }
                if (node.getType().getTier() != SkillTier.SOCKET) {
                    data.unsocketItem(nodeId);
                }
            }
        }
        return forgotten;
    }

    private static boolean hasInvalidOption(ShipSkillData data, SkillNode node, Map<String, SkillType> types) {
        return node.getType().isChoice() && NodeEligibility.optionProblem(node.getType(), NodeReplacements.currentTypeId(data.getChoiceSelection(node.getId())), types::get) != null;
    }

    public static boolean hasLostStartingRoot(ShipSkillData data, Map<String, SkillNode> tree) {
        if (data.startingRootId == null) {
            return !data.allocatedNodeIds.isEmpty() && firstAllocatedRootId(data, SkillTreeTopology.rootIdsOf(tree.values())) == null;
        }
        SkillNode root = tree.get(data.startingRootId);
        return root == null || root.getType().getTier() != SkillTier.ROOT;
    }

    public static List<String> resetAllocations(ShipSkillData data) {
        data.revision++;
        ItemCostLedger.ensure(data);
        List<String> released = new ArrayList<>(data.allocatedNodeIds);
        released.addAll(data.getDormantNodeIds());
        released.forEach(data::release);
        data.allocatedNodeIds.clear();
        if (data.dormantNodeIds != null) {
            data.dormantNodeIds.clear();
        }
        data.freeNodeIds().clear();
        data.pairGrantedNodeIds().clear();
        if (data.subroutineGrantedNodeIds != null) {
            data.subroutineGrantedNodeIds.clear();
        }
        if (data.fleetGrantNodeIds != null) {
            data.fleetGrantNodeIds.clear();
        }
        if (data.choiceSelections != null) {
            data.choiceSelections.clear();
        }
        data.startingRootId = null;
        return released;
    }

    public static List<String> resetProgress(ShipSkillData data) {
        List<String> released = resetAllocations(data);
        data.level = 0;
        data.xp = 0f;
        data.bankedFreeAllocations = 0;
        data.pendingTemplateFreeAllocations = 0;
        return released;
    }

    public static boolean canDeallocate(ShipSkillData data, SkillNode node, Collection<SkillNode> allNodes, String satisfiedRootId) {
        return canDeallocate(data, node, SkillTreeTopology.of(allNodes), satisfiedRootId);
    }

    public static boolean canDeallocate(ShipSkillData data, SkillNode node, SkillTreeTopology topology, String satisfiedRootId) {
        Set<String> excluded = new HashSet<>();
        excluded.add(node.getId());
        String pairedId = ShipTreeLinks.wormholePartner(data, node);
        if (pairedId != null) excluded.add(pairedId);

        Set<String> reachableBefore = reachableAllocatedNodeIds(data, topology, satisfiedRootId, Set.of());
        Set<String> reachableAfter = reachableAllocatedNodeIds(data, topology, satisfiedRootId, excluded);
        Set<String> closingRelease = closingRelease(data, node, topology, satisfiedRootId, reachableBefore);
        List<String> grantedNodeIds = ShipTreeLinks.grantedNodesBySocket(data).getOrDefault(node.getId(), List.of());

        for (String allocatedId : reachableBefore) {
            if (excluded.contains(allocatedId) || closingRelease.contains(allocatedId) || grantedNodeIds.contains(allocatedId)) continue;
            if (!reachableAfter.contains(allocatedId)) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> closingRelease(ShipSkillData data, SkillNode node, SkillTreeTopology topology, String satisfiedRootId, Set<String> reachableBefore) {
        String exitId = ShipTreeLinks.anchorExit(data, node.getId());
        if (exitId == null && ShipTreeLinks.anchorSocket(data, node.getId()) != null) {
            exitId = node.getId();
        }
        if (exitId == null) {
            return Set.of();
        }
        Set<String> released = new HashSet<>(reachableBefore);
        released.removeAll(reachableAllocatedNodeIds(data, topology, satisfiedRootId, Set.of(exitId)));
        return released;
    }

    static Set<String> reachableAllocatedNodeIds(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId, Set<String> excludedNodeIds) {
        Set<String> reached = pathReachableAllocatedNodeIds(data, topology, satisfiedRootId, excludedNodeIds);
        reached.addAll(grantedReachableNodeIds(data, topology, reached, excludedNodeIds));
        return reached;
    }

    private static List<String> grantedReachableNodeIds(ShipSkillData data, SkillTreeTopology topology, Set<String> pathReached, Set<String> excludedNodeIds) {
        List<String> granted = new ArrayList<>();
        for (Map.Entry<String, List<String>> grantLink : ShipTreeLinks.grantedNodesBySocket(data).entrySet()) {
            if (!pathReached.contains(grantLink.getKey())) {
                continue;
            }
            for (String grantedNodeId : grantLink.getValue()) {
                if (data.isAllocated(grantedNodeId) && !excludedNodeIds.contains(grantedNodeId) && topology.node(grantedNodeId) != null) {
                    granted.add(grantedNodeId);
                }
            }
        }
        if (data.fleetGrantNodeIds != null) {
            for (String fleetGrantedId : data.fleetGrantNodeIds) {
                if (data.isAllocated(fleetGrantedId) && !excludedNodeIds.contains(fleetGrantedId) && topology.node(fleetGrantedId) != null) {
                    granted.add(fleetGrantedId);
                }
            }
        }
        return granted;
    }

    private static Set<String> pathReachableAllocatedNodeIds(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId, Set<String> excludedNodeIds) {
        List<String> seeds = new ArrayList<>();
        if (satisfiedRootId != null && !excludedNodeIds.contains(satisfiedRootId)) {
            seeds.add(satisfiedRootId);
        }
        for (String allocatedId : data.allocatedNodeIds) {
            SkillNode allocatedNode = excludedNodeIds.contains(allocatedId) ? null : topology.node(allocatedId);
            if (allocatedNode != null && allocatedNode.getConnectedNodeIds().isEmpty()) {
                seeds.add(allocatedId);
            }
        }
        return new HashSet<>(TreeSearch.from(topology, seeds,
                child -> !excludedNodeIds.contains(child.getId()) && data.isAllocated(child.getId())
                        && child.getType().getTier() != SkillTier.WORMHOLE_EXIT,
                child -> allocatedWormholePartner(data, child, excludedNodeIds)).reached());
    }

    private static String allocatedWormholePartner(ShipSkillData data, SkillNode node, Set<String> excludedNodeIds) {
        String partnerId = ShipTreeLinks.wormholePartner(data, node);
        return partnerId != null && !excludedNodeIds.contains(partnerId) && data.isAllocated(partnerId) ? partnerId : null;
    }
}
