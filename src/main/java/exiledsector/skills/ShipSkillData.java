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
    private Map<String, String> chargedItemIds;
    private Map<String, Float> chargedItemQuantities;
    private Set<String> dormantNodeIds;
    private String installedFrameworkId;
    private Map<String, String> frameworkSocketedItems;
    private int frameworkPoints;
    private List<String> unlockedSocketTypes;
    private Map<String, String> frameworkItemsBySocketType;

    private Set<String> freeNodeIds() {
        if (freeNodeIds == null) freeNodeIds = new LinkedHashSet<>();
        return freeNodeIds;
    }

    private Set<String> dormantNodeIds() {
        if (dormantNodeIds == null) dormantNodeIds = new LinkedHashSet<>();
        return dormantNodeIds;
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

    public int getFrameworkPoints() {
        return frameworkPoints;
    }

    public int getUnspentFrameworkPoints() {
        return Math.max(0, frameworkPoints - getUnlockedSocketTypeIds().size());
    }

    public boolean addFrameworkPoint(int maxPoints) {
        revision++;
        if (frameworkPoints >= maxPoints) {
            return false;
        }
        frameworkPoints++;
        return true;
    }

    public List<String> getUnlockedSocketTypeIds() {
        return unlockedSocketTypes == null ? List.of() : Collections.unmodifiableList(unlockedSocketTypes);
    }

    public boolean isSocketTypeUnlocked(String socketTypeId) {
        return unlockedSocketTypes != null && unlockedSocketTypes.contains(socketTypeId);
    }

    public boolean unlockSocketType(String socketTypeId) {
        revision++;
        if (socketTypeId == null || isSocketTypeUnlocked(socketTypeId) || getUnspentFrameworkPoints() <= 0) {
            return false;
        }
        if (unlockedSocketTypes == null) unlockedSocketTypes = new ArrayList<>();
        unlockedSocketTypes.add(socketTypeId);
        return true;
    }

    public void grantUnlockedSocketType(String socketTypeId) {
        revision++;
        if (socketTypeId == null || isSocketTypeUnlocked(socketTypeId)) {
            return;
        }
        frameworkPoints++;
        if (unlockedSocketTypes == null) unlockedSocketTypes = new ArrayList<>();
        unlockedSocketTypes.add(socketTypeId);
    }

    public String lockSocketType(String socketTypeId) {
        revision++;
        if (unlockedSocketTypes == null || !unlockedSocketTypes.remove(socketTypeId)) {
            return null;
        }
        return frameworkItemsBySocketType == null ? null : frameworkItemsBySocketType.remove(socketTypeId);
    }

    public Map<String, String> getFrameworkSocketedItems() {
        return frameworkItemsBySocketType == null ? Map.of() : Collections.unmodifiableMap(frameworkItemsBySocketType);
    }

    public String getFrameworkSocketedItem(String socketTypeId) {
        return frameworkItemsBySocketType == null ? null : frameworkItemsBySocketType.get(socketTypeId);
    }

    public boolean socketFrameworkItem(String socketTypeId, String socketableId) {
        revision++;
        if (!isSocketTypeUnlocked(socketTypeId)) {
            return false;
        }
        if (frameworkItemsBySocketType == null) frameworkItemsBySocketType = new LinkedHashMap<>();
        frameworkItemsBySocketType.put(socketTypeId, socketableId);
        return true;
    }

    public String unsocketFrameworkItem(String socketTypeId) {
        revision++;
        return frameworkItemsBySocketType == null ? null : frameworkItemsBySocketType.remove(socketTypeId);
    }

    public String takeLegacyFrameworkId() {
        String legacyFrameworkId = installedFrameworkId;
        installedFrameworkId = null;
        return legacyFrameworkId;
    }

    public Map<String, String> takeLegacyFrameworkItems() {
        Map<String, String> legacyItems = frameworkSocketedItems == null ? Map.of() : Map.copyOf(frameworkSocketedItems);
        frameworkSocketedItems = null;
        return legacyItems;
    }

    public void markChanged() {
        revision++;
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
            discardItemCharge(node.getId());
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
        ensureItemChargeLedger();
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
        return allocatedNodeIds.isEmpty() && getDormantNodeIds().isEmpty() && level == 0 && xp == 0f && bankedFreeAllocations == 0
                && installedFrameworkId == null && frameworkPoints == 0;
    }

    public Set<String> getDormantNodeIds() {
        return dormantNodeIds == null ? Set.of() : Collections.unmodifiableSet(dormantNodeIds);
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
        discardItemCharge(node.getId());
        allocatedNodeIds.add(node.getId());
        charge(node.getId(), opCost);

        String pairedId = node.getPairedNodeId();
        if (pairedId != null && !isAllocated(pairedId)) {
            discardItemCharge(pairedId);
            allocatedNodeIds.add(pairedId);
            pairedFreeNodeIds().add(pairedId);
        }
    }

    public void deallocate(SkillNode node) {
        revision++;
        ensureItemChargeLedger();
        allocatedNodeIds.remove(node.getId());
        release(node.getId());

        String pairedId = node.getPairedNodeId();
        if (pairedId != null && allocatedNodeIds.remove(pairedId)) {
            release(pairedId);
        }
    }

    public boolean replaceNode(String oldId, String newId) {
        revision++;
        boolean dormant = getDormantNodeIds().contains(oldId);
        if ((!dormant && !allocatedNodeIds.contains(oldId)) || allocatedNodeIds.contains(newId) || getDormantNodeIds().contains(newId)) {
            return false;
        }
        ensureItemChargeLedger();
        Set<String> holdingNodeIds = dormant ? dormantNodeIds : allocatedNodeIds;
        List<String> order = new ArrayList<>(holdingNodeIds);
        holdingNodeIds.clear();
        order.forEach(id -> holdingNodeIds.add(id.equals(oldId) ? newId : id));
        if (freeNodeIds().remove(oldId)) {
            freeNodeIds().add(newId);
        }
        if (pairedFreeNodeIds().remove(oldId)) {
            pairedFreeNodeIds().add(newId);
        }
        SkillItemCost movedCharge = takeItemCharge(oldId);
        if (movedCharge != null) {
            recordItemCharge(newId, movedCharge);
        }
        if (optionalSelections != null) {
            optionalSelections.remove(oldId);
        }
        unsocketItem(oldId);
        return true;
    }

    public List<String> forgetUnknownNodes(Map<String, SkillNode> tree, Map<String, SkillType> types) {
        return forgetUnknownNodes(tree, tree, types);
    }

    public List<String> forgetUnknownNodes(Map<String, SkillNode> tree, Map<String, SkillNode> declaredTree, Map<String, SkillType> types) {
        revision++;
        ensureItemChargeLedger();
        List<String> forgotten = new ArrayList<>();
        for (String nodeId : List.copyOf(getDormantNodeIds())) {
            SkillNode declaredNode = declaredTree.get(nodeId);
            if (declaredNode == null || hasInvalidOption(declaredNode, types)) {
                dormantNodeIds.remove(nodeId);
                release(nodeId);
                forgotten.add(nodeId);
            }
        }
        for (String nodeId : List.copyOf(allocatedNodeIds)) {
            SkillNode node = tree.get(nodeId);
            SkillNode declaredNode = declaredTree.get(nodeId);
            if (node == null && declaredNode != null && !hasInvalidOption(declaredNode, types)) {
                allocatedNodeIds.remove(nodeId);
                dormantNodeIds().add(nodeId);
            } else if (node == null || hasInvalidOption(node, types)) {
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

    public List<String> wakeDormantNodes(Map<String, SkillNode> tree, int maxAllocatedNodes) {
        if (getDormantNodeIds().isEmpty()) {
            return List.of();
        }
        revision++;
        ensureItemChargeLedger();
        String satisfiedRootId = resolveStartingRootId(tree.values());
        boolean wokeAny = true;
        while (wokeAny) {
            wokeAny = false;
            for (String nodeId : List.copyOf(dormantNodeIds)) {
                SkillNode node = tree.get(nodeId);
                if (node != null && isReconnected(node, satisfiedRootId) && fitsAfterWaking(node, maxAllocatedNodes)) {
                    dormantNodeIds.remove(nodeId);
                    allocatedNodeIds.add(nodeId);
                    wokeAny = true;
                }
            }
        }
        List<String> released = new ArrayList<>();
        for (String nodeId : List.copyOf(dormantNodeIds)) {
            if (tree.containsKey(nodeId)) {
                dormantNodeIds.remove(nodeId);
                release(nodeId);
                released.add(nodeId);
            }
        }
        return released;
    }

    private boolean isReconnected(SkillNode node, String satisfiedRootId) {
        for (String connectedId : node.getConnectedNodeIds()) {
            if (isSatisfied(connectedId, satisfiedRootId)) {
                return true;
            }
        }
        return false;
    }

    private boolean fitsAfterWaking(SkillNode node, int maxAllocatedNodes) {
        String pairedId = node.getPairedNodeId();
        boolean partnerOfAllocatedWormhole = pairedId != null && pairedFreeNodeIds().contains(node.getId()) && allocatedNodeIds.contains(pairedId);
        return partnerOfAllocatedWormhole || allocatedNodeIds.size() < maxAllocatedNodes;
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
        ensureItemChargeLedger();
        List<String> released = new ArrayList<>(allocatedNodeIds);
        released.addAll(getDormantNodeIds());
        released.forEach(this::release);
        allocatedNodeIds.clear();
        if (dormantNodeIds != null) {
            dormantNodeIds.clear();
        }
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

    public SkillItemCost itemCharge(String nodeId) {
        String itemId = chargedItemIds().get(nodeId);
        return itemId == null ? null : new SkillItemCost(itemId, chargedItemQuantities.getOrDefault(nodeId, 0f));
    }

    public void recordItemCharge(String nodeId, SkillItemCost itemCost) {
        chargedItemIds().put(nodeId, itemCost.itemId());
        chargedItemQuantities.put(nodeId, itemCost.quantity());
    }

    public SkillItemCost takeItemCharge(String nodeId) {
        SkillItemCost charged = itemCharge(nodeId);
        if (charged != null) {
            discardItemCharge(nodeId);
        }
        return charged;
    }

    private void discardItemCharge(String nodeId) {
        chargedItemIds().remove(nodeId);
        chargedItemQuantities.remove(nodeId);
    }

    private Map<String, String> chargedItemIds() {
        ensureItemChargeLedger();
        return chargedItemIds;
    }

    private void ensureItemChargeLedger() {
        if (chargedItemIds != null && chargedItemQuantities != null) {
            return;
        }
        chargedItemIds = new LinkedHashMap<>();
        chargedItemQuantities = new LinkedHashMap<>();
        if (npcBuild) {
            return;
        }
        for (String nodeId : allocatedNodeIds) {
            SkillNode node = SkillTree.getDeclared(nodeId);
            SkillItemCost legacyCharge = node == null ? null : node.getType().getItemCost();
            if (legacyCharge != null) {
                chargedItemIds.put(nodeId, legacyCharge.itemId());
                chargedItemQuantities.put(nodeId, legacyCharge.quantity());
            }
        }
    }
}
