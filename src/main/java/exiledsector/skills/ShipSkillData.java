package exiledsector.skills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private transient int revision;
    private Map<String, String> socketedItems;

    private Set<String> freeNodeIds() {
        if (freeNodeIds == null) freeNodeIds = new LinkedHashSet<>();
        return freeNodeIds;
    }

    private Set<String> pairedFreeNodeIds() {
        if (pairedFreeNodeIds == null) pairedFreeNodeIds = new LinkedHashSet<>();
        return pairedFreeNodeIds;
    }

    public String getSocketedItem(String nodeId) {
        return socketedItems == null ? null : socketedItems.get(nodeId);
    }

    public Map<String, String> getSocketedItems() {
        return socketedItems == null ? Map.of() : Collections.unmodifiableMap(socketedItems);
    }

    public boolean socketItem(String nodeId, String socketableId) {
        revision++;
        if (!isAllocated(nodeId)) {
            return false;
        }
        if (socketedItems == null) socketedItems = new LinkedHashMap<>();
        socketedItems.put(nodeId, socketableId);
        return true;
    }

    public String unsocketItem(String nodeId) {
        revision++;
        return socketedItems == null ? null : socketedItems.remove(nodeId);
    }

    public boolean isAllocated(String nodeId) {
        return allocatedNodeIds.contains(nodeId);
    }

    public String getOptionalSelection(String nodeId) {
        if (optionalSelections == null) return null;
        return optionalSelections.get(nodeId);
    }

    public void selectOption(SkillNode node, SkillType chosenOption, int opCost) {
        revision++;
        if (!isAllocated(node.getId())) {
            charge(node.getId(), opCost);
        }
        allocatedNodeIds.add(node.getId());
        if (optionalSelections == null) optionalSelections = new LinkedHashMap<>();
        optionalSelections.put(node.getId(), chosenOption.getId());
    }

    public String resolveStartingRootId() {
        if (startingRootId == null) {
            startingRootId = firstAllocatedRootId(SkillTree.topology().rootIds());
        }
        return startingRootId;
    }

    public String resolveStartingRootId(Collection<SkillNode> allNodes) {
        if (startingRootId == null) {
            startingRootId = firstAllocatedRootId(SkillTreeTopology.rootIdsOf(allNodes));
        }
        return startingRootId;
    }

    private String firstAllocatedRootId(Set<String> rootIds) {
        for (String allocatedId : allocatedNodeIds) {
            if (rootIds.contains(allocatedId)) {
                return allocatedId;
            }
        }
        return null;
    }

    public boolean chooseStartingRoot(SkillNode root) {
        revision++;
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
        revision++;
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

    public int revision() {
        return revision;
    }

    public Set<String> getAllocatedNodeIds() {
        return allocatedNodeIds;
    }

    public int getSpentOp(int opCostPerNode) {
        return paidNodeCount() * opCostPerNode;
    }

    private int paidNodeCount() {
        String startingRoot = resolveStartingRootId();
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
        revision++;
        npcBuild = true;
    }

    public void clearNpcBuild() {
        revision++;
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
        revision++;
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
        revision++;
        allocatedNodeIds.add(node.getId());
        charge(node.getId(), opCost);

        String pairedId = node.getPairedNodeId();
        if (pairedId != null && !isAllocated(pairedId)) {
            allocatedNodeIds.add(pairedId);
            pairedFreeNodeIds().add(pairedId);
        }
    }

    public void deallocate(SkillNode node) {
        revision++;
        allocatedNodeIds.remove(node.getId());
        release(node.getId());

        String pairedId = node.getPairedNodeId();
        if (pairedId != null && allocatedNodeIds.remove(pairedId)) {
            release(pairedId);
        }
    }

    public boolean replaceNode(String oldId, String newId) {
        revision++;
        if (!allocatedNodeIds.contains(oldId) || allocatedNodeIds.contains(newId)) {
            return false;
        }
        List<String> order = new ArrayList<>(allocatedNodeIds);
        allocatedNodeIds.clear();
        order.forEach(id -> allocatedNodeIds.add(id.equals(oldId) ? newId : id));
        if (freeNodeIds().remove(oldId)) {
            freeNodeIds().add(newId);
        }
        if (pairedFreeNodeIds().remove(oldId)) {
            pairedFreeNodeIds().add(newId);
        }
        if (optionalSelections != null) {
            optionalSelections.remove(oldId);
        }
        unsocketItem(oldId);
        return true;
    }

    public List<String> forgetUnknownNodes(Map<String, SkillNode> tree, Map<String, SkillType> types) {
        revision++;
        List<String> forgotten = new ArrayList<>();
        for (String nodeId : List.copyOf(allocatedNodeIds)) {
            SkillNode node = tree.get(nodeId);
            if (node == null || hasInvalidOption(node, types)) {
                allocatedNodeIds.remove(nodeId);
                release(nodeId);
                forgotten.add(nodeId);
            } else {
                if (!node.getType().isOptional() && optionalSelections != null) {
                    optionalSelections.remove(nodeId);
                }
                if (node.getType().getTier() != SkillTier.SOCKET) {
                    unsocketItem(nodeId);
                }
            }
        }
        return forgotten;
    }

    private boolean hasInvalidOption(SkillNode node, Map<String, SkillType> types) {
        if (!node.getType().isOptional()) {
            return false;
        }
        String selectedId = getOptionalSelection(node.getId());
        return selectedId == null || !node.getType().getOptionalOptionIds().contains(selectedId) || !types.containsKey(selectedId);
    }

    public boolean hasLostStartingRoot(Map<String, SkillNode> tree) {
        if (startingRootId == null) {
            return !allocatedNodeIds.isEmpty() && firstAllocatedRootId(SkillTreeTopology.rootIdsOf(tree.values())) == null;
        }
        SkillNode root = tree.get(startingRootId);
        return root == null || root.getType().getTier() != SkillTier.ROOT;
    }

    public List<String> resetAllocations() {
        revision++;
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
        unsocketItem(nodeId);
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
        return canDeallocate(node, SkillTreeTopology.of(allNodes), satisfiedRootId);
    }

    public boolean canDeallocate(SkillNode node, SkillTreeTopology topology, String satisfiedRootId) {
        Set<String> excluded = new HashSet<>();
        excluded.add(node.getId());
        String pairedId = node.getPairedNodeId();
        if (pairedId != null) excluded.add(pairedId);

        Set<String> reachableBefore = reachableAllocatedNodeIds(topology, satisfiedRootId, Set.of());
        Set<String> reachableAfter = reachableAllocatedNodeIds(topology, satisfiedRootId, excluded);

        for (String allocatedId : reachableBefore) {
            if (excluded.contains(allocatedId)) continue;
            if (!reachableAfter.contains(allocatedId)) {
                return false;
            }
        }
        return true;
    }

    Set<String> reachableAllocatedNodeIds(SkillTreeTopology topology, String satisfiedRootId, Set<String> excludedNodeIds) {
        List<String> seeds = new ArrayList<>();
        if (satisfiedRootId != null && !excludedNodeIds.contains(satisfiedRootId)) {
            seeds.add(satisfiedRootId);
        }
        for (String allocatedId : allocatedNodeIds) {
            SkillNode allocatedNode = excludedNodeIds.contains(allocatedId) ? null : topology.node(allocatedId);
            if (allocatedNode != null && allocatedNode.getConnectedNodeIds().isEmpty()) {
                seeds.add(allocatedId);
            }
        }
        return new HashSet<>(TreeSearch.from(topology, seeds,
                child -> !excludedNodeIds.contains(child.getId()) && isAllocated(child.getId()),
                child -> allocatedWormholePartner(child, excludedNodeIds)).reached());
    }

    private String allocatedWormholePartner(SkillNode node, Set<String> excludedNodeIds) {
        String partnerId = node.getType().getTier() == SkillTier.WORMHOLE ? node.getPairedNodeId() : null;
        return partnerId != null && !excludedNodeIds.contains(partnerId) && isAllocated(partnerId) ? partnerId : null;
    }

    public void toggle(SkillNode node, Collection<SkillNode> allNodes, String satisfiedRootId, int totalOp, int opCost, int maxAllocatedNodes) {
        toggle(node, SkillTreeTopology.of(allNodes), satisfiedRootId, totalOp, opCost, maxAllocatedNodes);
    }

    public void toggle(SkillNode node, SkillTreeTopology topology, String satisfiedRootId, int totalOp, int opCost, int maxAllocatedNodes) {
        if (isAllocated(node.getId())) {
            if (canDeallocate(node, topology, satisfiedRootId)) {
                deallocate(node);
            }
        } else if (canAllocate(node, satisfiedRootId, totalOp, opCost, maxAllocatedNodes)) {
            allocate(node, opCost);
        }
    }
}
