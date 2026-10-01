package exiledsector.skills;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShipSkillData {

    private final Set<String> allocatedNodeIds = new LinkedHashSet<>();
    private Map<String, String> optionalSelections = new LinkedHashMap<>();
    private int level = 0;
    private float xp = 0f;
    private int bankedFreeAllocations = 0;
    private Set<String> freeNodeIds = new LinkedHashSet<>();
    private Set<String> pairedFreeNodeIds = new LinkedHashSet<>();
    private String startingRootId;
    private boolean npcBuild;

    private Set<String> freeNodeIds() {
        if (freeNodeIds == null) freeNodeIds = new LinkedHashSet<>();
        return freeNodeIds;
    }

    private Set<String> pairedFreeNodeIds() {
        if (pairedFreeNodeIds == null) pairedFreeNodeIds = new LinkedHashSet<>();
        return pairedFreeNodeIds;
    }

    public boolean isAllocated(String nodeId) {
        return allocatedNodeIds.contains(nodeId);
    }

    public String getOptionalSelection(String nodeId) {
        if (optionalSelections == null) return null;
        return optionalSelections.get(nodeId);
    }

    public void selectOption(SkillNode node, SkillType chosenOption, int opCost) {
        if (!isAllocated(node.getId())) {
            charge(node.getId(), opCost);
        }
        allocatedNodeIds.add(node.getId());
        if (optionalSelections == null) optionalSelections = new LinkedHashMap<>();
        optionalSelections.put(node.getId(), chosenOption.getId());
    }

    public String resolveStartingRootId(Collection<SkillNode> allNodes) {
        if (startingRootId == null) {
            startingRootId = firstAllocatedRootId(allNodes);
        }
        return startingRootId;
    }

    private String firstAllocatedRootId(Collection<SkillNode> allNodes) {
        Set<String> rootIds = new HashSet<>();
        for (SkillNode candidate : allNodes) {
            if (candidate.getType().getTier() == SkillTier.ROOT) {
                rootIds.add(candidate.getId());
            }
        }
        for (String allocatedId : allocatedNodeIds) {
            if (rootIds.contains(allocatedId)) {
                return allocatedId;
            }
        }
        return null;
    }

    public boolean chooseStartingRoot(SkillNode root) {
        if (startingRootId != null || root.getType().getTier() != SkillTier.ROOT) {
            return false;
        }
        startingRootId = root.getId();
        allocate(root, 0);
        return true;
    }

    public boolean canUnchooseStartingRoot() {
        return startingRootId != null && allocatedNodeIds.size() == 1 && allocatedNodeIds.contains(startingRootId);
    }

    public boolean unchooseStartingRoot() {
        if (!canUnchooseStartingRoot()) {
            return false;
        }
        allocatedNodeIds.remove(startingRootId);
        release(startingRootId);
        startingRootId = null;
        return true;
    }

    public boolean isSatisfied(String nodeId, String satisfiedRootId) {
        return isAllocated(nodeId) || nodeId.equals(satisfiedRootId);
    }

    public Set<String> getAllocatedNodeIds() {
        return allocatedNodeIds;
    }

    public int getSpentOp(int opCostPerNode) {
        return paidNodeCount() * opCostPerNode;
    }

    private int paidNodeCount() {
        String startingRoot = resolveStartingRootId(SkillTree.getAllNodes().values());
        int paid = 0;
        for (String nodeId : allocatedNodeIds) {
            if (!isFreeNode(nodeId) && !nodeId.equals(startingRoot)) {
                paid++;
            }
        }
        return paid;
    }

    public int getLevel() {
        return level;
    }

    public float getXp() {
        return xp;
    }

    public int getBankedFreeAllocations() {
        return bankedFreeAllocations;
    }

    public boolean isFreeNode(String nodeId) {
        return freeNodeIds().contains(nodeId) || pairedFreeNodeIds().contains(nodeId);
    }

    public boolean isNpcBuild() {
        return npcBuild;
    }

    public void markNpcBuild() {
        npcBuild = true;
    }

    public void clearNpcBuild() {
        npcBuild = false;
    }

    public boolean isBlank() {
        return allocatedNodeIds.isEmpty() && level == 0 && xp == 0f && bankedFreeAllocations == 0;
    }

    public void addXp(float amount) {
        xp += amount;
    }

    public void subtractXp(float amount) {
        xp -= amount;
    }

    public void incrementLevel() {
        level++;
    }

    public void addFreeAllocationCredit() {
        bankedFreeAllocations++;
    }

    public boolean convertMostRecentAllocationToFree(Collection<SkillNode> allNodes) {
        String startingRoot = resolveStartingRootId(allNodes);
        List<String> order = new ArrayList<>(allocatedNodeIds);
        for (int i = order.size() - 1; i >= 0; i--) {
            String nodeId = order.get(i);
            boolean alreadyFree = freeNodeIds().contains(nodeId) || pairedFreeNodeIds().contains(nodeId);
            if (alreadyFree || nodeId.equals(startingRoot)) {
                continue;
            }
            freeNodeIds().add(nodeId);
            return true;
        }
        return false;
    }

    public void allocate(SkillNode node, int opCost) {
        allocatedNodeIds.add(node.getId());
        charge(node.getId(), opCost);

        String pairedId = node.getPairedNodeId();
        if (pairedId != null && !isAllocated(pairedId)) {
            allocatedNodeIds.add(pairedId);
            pairedFreeNodeIds().add(pairedId);
        }
    }

    public void deallocate(SkillNode node) {
        allocatedNodeIds.remove(node.getId());
        release(node.getId());

        String pairedId = node.getPairedNodeId();
        if (pairedId != null && allocatedNodeIds.remove(pairedId)) {
            release(pairedId);
        }
    }

    public List<String> forgetUnknownNodes(Map<String, SkillNode> tree) {
        List<String> forgotten = new ArrayList<>();
        for (String nodeId : List.copyOf(allocatedNodeIds)) {
            if (!tree.containsKey(nodeId)) {
                allocatedNodeIds.remove(nodeId);
                release(nodeId);
                forgotten.add(nodeId);
            }
        }
        return forgotten;
    }

    public boolean hasLostStartingRoot(Map<String, SkillNode> tree) {
        if (startingRootId == null) {
            return !allocatedNodeIds.isEmpty() && firstAllocatedRootId(tree.values()) == null;
        }
        SkillNode root = tree.get(startingRootId);
        return root == null || root.getType().getTier() != SkillTier.ROOT;
    }

    public List<String> resetAllocations() {
        List<String> released = List.copyOf(allocatedNodeIds);
        released.forEach(this::release);
        allocatedNodeIds.clear();
        freeNodeIds().clear();
        pairedFreeNodeIds().clear();
        if (optionalSelections != null) {
            optionalSelections.clear();
        }
        startingRootId = null;
        return released;
    }

    private void charge(String nodeId, int opCost) {
        if (opCost > 0 && bankedFreeAllocations > 0) {
            bankedFreeAllocations--;
            freeNodeIds().add(nodeId);
        }
    }

    private void release(String nodeId) {
        if (optionalSelections != null) {
            optionalSelections.remove(nodeId);
        }
        if (freeNodeIds().remove(nodeId)) {
            bankedFreeAllocations++;
        } else {
            pairedFreeNodeIds().remove(nodeId);
        }
    }

    public boolean canAllocate(SkillNode node, String satisfiedRootId, int totalOp, int opCost, int maxAllocatedNodes) {
        int slotsNeeded = 1;
        String pairedId = node.getPairedNodeId();
        if (pairedId != null && !isAllocated(pairedId)) {
            slotsNeeded = 2;
        }
        if (allocatedNodeIds.size() + slotsNeeded > maxAllocatedNodes) {
            return false;
        }
        if (opCost > 0 && bankedFreeAllocations <= 0 && getSpentOp(opCost) + opCost > totalOp) {
            return false;
        }
        if (node.getConnectedNodeIds().isEmpty()) {
            return true;
        }
        for (String connectedId : node.getConnectedNodeIds()) {
            if (isSatisfied(connectedId, satisfiedRootId)) {
                return true;
            }
        }
        return false;
    }

    public boolean canDeallocate(SkillNode node, Collection<SkillNode> allNodes, String satisfiedRootId) {
        Map<String, SkillNode> byId = new HashMap<>();
        Map<String, List<String>> childrenOf = new HashMap<>();
        for (SkillNode candidate : allNodes) {
            byId.put(candidate.getId(), candidate);
            for (String prerequisiteId : candidate.getConnectedNodeIds()) {
                childrenOf.computeIfAbsent(prerequisiteId, key -> new ArrayList<>()).add(candidate.getId());
            }
        }

        Set<String> excluded = new HashSet<>();
        excluded.add(node.getId());
        String pairedId = node.getPairedNodeId();
        if (pairedId != null) excluded.add(pairedId);

        Set<String> reachableBefore = reachableAllocatedNodeIds(byId, childrenOf, satisfiedRootId, Set.of());
        Set<String> reachableAfter = reachableAllocatedNodeIds(byId, childrenOf, satisfiedRootId, excluded);

        for (String allocatedId : reachableBefore) {
            if (excluded.contains(allocatedId)) continue;
            if (!reachableAfter.contains(allocatedId)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> reachableAllocatedNodeIds(Map<String, SkillNode> byId, Map<String, List<String>> childrenOf,
                                                    String satisfiedRootId, Set<String> excludedNodeIds) {
        Set<String> reachable = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        seedReachableFrontier(byId, satisfiedRootId, excludedNodeIds, reachable, queue);
        expandReachableFrontier(childrenOf, excludedNodeIds, reachable, queue);
        return reachable;
    }

    private void seedReachableFrontier(Map<String, SkillNode> byId, String satisfiedRootId, Set<String> excludedNodeIds,
                                        Set<String> reachable, Deque<String> queue) {
        if (satisfiedRootId != null && !excludedNodeIds.contains(satisfiedRootId) && reachable.add(satisfiedRootId)) {
            queue.add(satisfiedRootId);
        }
        for (String allocatedId : allocatedNodeIds) {
            if (excludedNodeIds.contains(allocatedId)) {
                continue;
            }
            SkillNode allocatedNode = byId.get(allocatedId);
            if (allocatedNode != null && allocatedNode.getConnectedNodeIds().isEmpty() && reachable.add(allocatedId)) {
                queue.add(allocatedId);
            }
        }
    }

    private void expandReachableFrontier(Map<String, List<String>> childrenOf, Set<String> excludedNodeIds,
                                          Set<String> reachable, Deque<String> queue) {
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            for (String childId : childrenOf.getOrDefault(currentId, List.of())) {
                if (excludedNodeIds.contains(childId) || !isAllocated(childId)) {
                    continue;
                }
                if (reachable.add(childId)) {
                    queue.add(childId);
                }
            }
        }
    }

    public void toggle(SkillNode node, Collection<SkillNode> allNodes, String satisfiedRootId, int totalOp, int opCost, int maxAllocatedNodes) {
        if (isAllocated(node.getId())) {
            if (canDeallocate(node, allNodes, satisfiedRootId)) {
                deallocate(node);
            }
        } else if (canAllocate(node, satisfiedRootId, totalOp, opCost, maxAllocatedNodes)) {
            allocate(node, opCost);
        }
    }
}
