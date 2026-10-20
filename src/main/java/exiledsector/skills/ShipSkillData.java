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
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ShipSkillData implements WormholePartners {

    private final Set<String> allocatedNodeIds = new LinkedHashSet<>();
    private Map<String, String> optionalSelections = new LinkedHashMap<>();
    private int level = 0;
    private float xp = 0f;
    private int bankedFreeAllocations = 0;
    private Set<String> freeNodeIds = new LinkedHashSet<>();
    private Set<String> pairedFreeNodeIds = new LinkedHashSet<>();
    private Set<String> grantFreeNodeIds;
    private String startingRootId;
    private boolean npcBuild;
    private transient int revision;
    private Map<String, String> socketedItems;
    private Map<String, String> chargedItemIds;
    private Map<String, Float> chargedItemQuantities;
    private Set<String> dormantNodeIds;
    private int frameworkPoints;
    private List<String> unlockedSocketTypes;
    private Map<String, String> frameworkItemsBySocketType;
    private boolean autoAllocateTemplate;
    private transient int pendingTemplateCredits;
    private transient Map<String, String> anchorExitBySocketId;
    private transient Map<String, String> anchorSocketByExitId;
    private transient int anchorLinksRevision;
    private transient AnchorLinks.ExitLookup anchorLinksLookup;
    private transient int anchorLinksLookupRevision;
    private transient Map<String, List<String>> grantedNodesBySocketId;
    private transient int grantLinksRevision;
    private transient NodeGrants.GrantLookup grantLinksLookup;
    private transient int grantLinksLookupRevision;

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

    private Set<String> grantFreeNodeIds() {
        if (grantFreeNodeIds == null) grantFreeNodeIds = new LinkedHashSet<>();
        return grantFreeNodeIds;
    }

    public String getSocketedItem(String nodeId) {
        return socketedItems == null ? null : socketedItems.get(nodeId);
    }

    public Map<String, String> getSocketedItems() {
        return socketedItems == null ? Map.of() : Collections.unmodifiableMap(socketedItems);
    }

    public boolean socketItem(String nodeId, String itemId) {
        revision++;
        if (!isAllocated(nodeId)) {
            return false;
        }
        if (socketedItems == null) socketedItems = new LinkedHashMap<>();
        socketedItems.put(nodeId, itemId);
        return true;
    }

    public String unsocketItem(String nodeId) {
        revision++;
        return socketedItems == null ? null : socketedItems.remove(nodeId);
    }

    public int getHullUpgradePoints() {
        return frameworkPoints;
    }

    public int getUnspentHullUpgradePoints() {
        return Math.max(0, frameworkPoints - getUnlockedSocketTypeIds().size());
    }

    public boolean addHullUpgradePoint(int maxPoints) {
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
        if (socketTypeId == null || isSocketTypeUnlocked(socketTypeId) || getUnspentHullUpgradePoints() <= 0) {
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

    public Map<String, String> getSectionItems() {
        return frameworkItemsBySocketType == null ? Map.of() : Collections.unmodifiableMap(frameworkItemsBySocketType);
    }

    public List<String> getInstalledItemIds() {
        List<String> installedIds = new ArrayList<>(getSocketedItems().values());
        installedIds.addAll(getSectionItems().values());
        return installedIds;
    }

    public boolean holdsItem(String itemId) {
        return getSocketedItems().containsValue(itemId) || getSectionItems().containsValue(itemId);
    }

    public String getSectionItem(String socketTypeId) {
        return frameworkItemsBySocketType == null ? null : frameworkItemsBySocketType.get(socketTypeId);
    }

    public boolean installInSection(String socketTypeId, String itemId) {
        revision++;
        if (!isSocketTypeUnlocked(socketTypeId)) {
            return false;
        }
        if (frameworkItemsBySocketType == null) frameworkItemsBySocketType = new LinkedHashMap<>();
        frameworkItemsBySocketType.put(socketTypeId, itemId);
        return true;
    }

    public String removeFromSection(String socketTypeId) {
        revision++;
        return frameworkItemsBySocketType == null ? null : frameworkItemsBySocketType.remove(socketTypeId);
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

    @Override
    public String wormholePartner(SkillNode node) {
        String anchorPartner = anchorPartner(node.getId());
        return anchorPartner != null ? anchorPartner : WormholePartners.pairedWith(node);
    }

    public String anchorPartner(String nodeId) {
        refreshAnchorLinks();
        String exitId = anchorExitBySocketId.get(nodeId);
        return exitId != null ? exitId : anchorSocketByExitId.get(nodeId);
    }

    public String anchorExit(String socketNodeId) {
        refreshAnchorLinks();
        return anchorExitBySocketId.get(socketNodeId);
    }

    public String anchorSocket(String exitNodeId) {
        refreshAnchorLinks();
        return anchorSocketByExitId.get(exitNodeId);
    }

    public Map<String, String> anchorExitsBySocket() {
        refreshAnchorLinks();
        return Collections.unmodifiableMap(anchorExitBySocketId);
    }

    public List<SkillTreeTopology.WormholePair> wormholePairs(SkillTreeTopology topology) {
        refreshAnchorLinks();
        if (anchorExitBySocketId.isEmpty()) {
            return topology.wormholePairs();
        }
        List<SkillTreeTopology.WormholePair> pairs = new ArrayList<>(topology.wormholePairs());
        anchorExitBySocketId.forEach((socketId, exitId) -> {
            SkillNode socket = topology.node(socketId);
            SkillNode exit = topology.node(exitId);
            if (socket != null && exit != null && isAllocated(exitId)) {
                pairs.add(new SkillTreeTopology.WormholePair(socket, exit));
            }
        });
        return pairs;
    }

    private void refreshAnchorLinks() {
        AnchorLinks.ExitLookup lookup = AnchorLinks.lookup();
        int lookupRevision = lookup.revision();
        if (anchorExitBySocketId != null && anchorLinksRevision == revision && anchorLinksLookup == lookup
                && anchorLinksLookupRevision == lookupRevision) {
            return;
        }
        Map<String, String> exitBySocketId = new LinkedHashMap<>();
        Map<String, String> socketByExitId = new LinkedHashMap<>();
        if (socketedItems != null) {
            new TreeMap<>(socketedItems).forEach((socketId, itemId) -> {
                if (isAllocated(socketId)) {
                    link(socketId, lookup.exitNodeId(itemId), exitBySocketId, socketByExitId);
                }
            });
        }
        anchorExitBySocketId = exitBySocketId.isEmpty() ? Map.of() : exitBySocketId;
        anchorSocketByExitId = socketByExitId.isEmpty() ? Map.of() : socketByExitId;
        anchorLinksRevision = revision;
        anchorLinksLookup = lookup;
        anchorLinksLookupRevision = lookupRevision;
    }

    private void link(String socketId, String exitId, Map<String, String> exitBySocketId, Map<String, String> socketByExitId) {
        if (exitId == null || exitId.equals(socketId) || !isAllocated(socketId) || socketByExitId.containsKey(exitId)
                || exitBySocketId.containsKey(exitId) || socketByExitId.containsKey(socketId)) {
            return;
        }
        exitBySocketId.put(socketId, exitId);
        socketByExitId.put(exitId, socketId);
    }

    public Map<String, List<String>> grantedNodesBySocket() {
        refreshGrantLinks();
        return Collections.unmodifiableMap(grantedNodesBySocketId);
    }

    public String grantingSocket(String nodeId) {
        refreshGrantLinks();
        for (Map.Entry<String, List<String>> grantLink : grantedNodesBySocketId.entrySet()) {
            if (grantLink.getValue().contains(nodeId)) {
                return grantLink.getKey();
            }
        }
        return null;
    }

    public boolean isGrantHeld(String nodeId) {
        return isAllocated(nodeId) && grantingSocket(nodeId) != null;
    }

    public boolean isGrantFree(String nodeId) {
        return grantFreeNodeIds != null && grantFreeNodeIds.contains(nodeId);
    }

    public int usedNodeSlots() {
        if (grantFreeNodeIds == null || grantFreeNodeIds.isEmpty()) {
            return allocatedNodeIds.size();
        }
        int used = 0;
        for (String nodeId : allocatedNodeIds) {
            if (!grantFreeNodeIds.contains(nodeId)) {
                used++;
            }
        }
        return used;
    }

    public void allocateGranted(SkillNode node) {
        revision++;
        String nodeId = node.getId();
        discardItemCharge(nodeId);
        dormantNodeIds().remove(nodeId);
        allocatedNodeIds.add(nodeId);
        grantFreeNodeIds().add(nodeId);
    }

    private Set<String> grantedNodeIds() {
        Set<String> granted = new HashSet<>();
        grantedNodesBySocket().values().forEach(granted::addAll);
        return granted;
    }

    private List<String> ungrantedFreeNodeIds() {
        if (grantFreeNodeIds == null || grantFreeNodeIds.isEmpty()) {
            return List.of();
        }
        Set<String> granted = grantedNodeIds();
        if (socketedItems != null && dormantNodeIds != null) {
            NodeGrants.GrantLookup lookup = NodeGrants.lookup();
            socketedItems.forEach((socketId, itemId) -> {
                if (dormantNodeIds.contains(socketId)) {
                    granted.addAll(lookup.grantedNodeIds(itemId));
                }
            });
        }
        List<String> ungranted = new ArrayList<>();
        for (String nodeId : grantFreeNodeIds) {
            if (allocatedNodeIds.contains(nodeId) && !granted.contains(nodeId)) {
                ungranted.add(nodeId);
            }
        }
        return ungranted;
    }

    public ReleasedNodes releaseUngrantedFreeNodes(SkillTreeTopology topology, String satisfiedRootId) {
        return releaseUngrantedFreeNodes(topology, satisfiedRootId, Set.of());
    }

    public ReleasedNodes releaseUngrantedFreeNodes(SkillTreeTopology topology, String satisfiedRootId, Set<String> alsoReleasable) {
        List<String> ungranted = ungrantedFreeNodeIds();
        if (ungranted.isEmpty() && alsoReleasable.isEmpty()) {
            return ReleasedNodes.NONE;
        }
        Set<String> reachableBefore = reachableAllocatedNodeIds(topology, satisfiedRootId, Set.of());
        reachableBefore.addAll(ungranted);
        reachableBefore.addAll(alsoReleasable);
        return releaseNewlyStranded(topology, satisfiedRootId, reachableBefore);
    }

    private void refreshGrantLinks() {
        NodeGrants.GrantLookup lookup = NodeGrants.lookup();
        int lookupRevision = lookup.revision();
        if (grantedNodesBySocketId != null && grantLinksRevision == revision && grantLinksLookup == lookup
                && grantLinksLookupRevision == lookupRevision) {
            return;
        }
        Map<String, List<String>> nodesBySocketId = new LinkedHashMap<>();
        if (socketedItems != null) {
            new TreeMap<>(socketedItems).forEach((socketId, itemId) -> {
                List<String> grantedNodeIds = isAllocated(socketId) ? lookup.grantedNodeIds(itemId) : List.of();
                List<String> linkedNodeIds = grantedNodeIds == null ? List.of()
                        : grantedNodeIds.stream().filter(nodeId -> nodeId != null && !nodeId.equals(socketId)).distinct().toList();
                if (!linkedNodeIds.isEmpty()) {
                    nodesBySocketId.put(socketId, linkedNodeIds);
                }
            });
        }
        grantedNodesBySocketId = nodesBySocketId.isEmpty() ? Map.of() : nodesBySocketId;
        grantLinksRevision = revision;
        grantLinksLookup = lookup;
        grantLinksLookupRevision = lookupRevision;
    }

    public Set<String> pathReachableAllocatedNodeIds(SkillTreeTopology topology, String satisfiedRootId) {
        return pathReachableAllocatedNodeIds(topology, satisfiedRootId, Set.of());
    }

    public Set<String> grantOnlyNodeIds(SkillTreeTopology topology, String satisfiedRootId) {
        if (grantedNodesBySocket().isEmpty()) {
            return Set.of();
        }
        Set<String> pathReached = pathReachableAllocatedNodeIds(topology, satisfiedRootId, Set.of());
        Set<String> grantOnly = new LinkedHashSet<>(grantedReachableNodeIds(topology, pathReached, Set.of()));
        grantOnly.removeAll(pathReached);
        return grantOnly;
    }

    public List<String> openAnchorWormholes(int maxAllocatedNodes) {
        List<String> openedExitIds = new ArrayList<>();
        AnchorLinks.ExitLookup lookup = AnchorLinks.lookup();
        for (Map.Entry<String, String> link : List.copyOf(anchorExitsBySocket().entrySet())) {
            String exitId = link.getValue();
            if (!isAllocated(exitId) && usedNodeSlots() < maxAllocatedNodes && canOpen(link.getKey(), lookup)) {
                revision++;
                discardItemCharge(exitId);
                dormantNodeIds().remove(exitId);
                allocatedNodeIds.add(exitId);
                pairedFreeNodeIds().add(exitId);
                openedExitIds.add(exitId);
            }
        }
        return openedExitIds;
    }

    private boolean canOpen(String socketId, AnchorLinks.ExitLookup lookup) {
        String itemId = getSocketedItem(socketId);
        return itemId != null && lookup.canOpen(itemId);
    }

    public Set<String> getAllocatedNodeIds() {
        return allocatedNodeIds;
    }

    public int getSpentOp(int opCostPerNode) {
        return paidNodeCount() * opCostPerNode;
    }

    public int paidNodeCount() {
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
        return freeNodeIds().contains(nodeId) || pairedFreeNodeIds().contains(nodeId) || isGrantFree(nodeId);
    }

    public boolean isAutoAllocatingTemplate() {
        return autoAllocateTemplate;
    }

    public void setAutoAllocateTemplate(boolean autoAllocate) {
        autoAllocateTemplate = autoAllocate;
    }

    public void bankLevelUpForTemplate() {
        bankedFreeAllocations++;
        pendingTemplateCredits++;
    }

    public int takePendingTemplateCredits() {
        int pending = pendingTemplateCredits;
        pendingTemplateCredits = 0;
        return pending;
    }

    public void refundBankedCreditAsOp(Collection<SkillNode> allNodes) {
        if (bankedFreeAllocations <= 0) {
            return;
        }
        bankedFreeAllocations--;
        if (!convertMostRecentAllocationToFree(allNodes)) {
            bankedFreeAllocations++;
        }
    }

    public ShipSkillData planningCopy() {
        ShipSkillData copy = new ShipSkillData();
        copy.startingRootId = resolveStartingRootId();
        copy.allocatedNodeIds.addAll(allocatedNodeIds);
        if (optionalSelections != null) {
            copy.optionalSelections.putAll(optionalSelections);
        }
        copy.grantFreeNodeIds = grantFreeNodeIds == null ? null : new LinkedHashSet<>(grantFreeNodeIds);
        copy.mirrorSocketedItems(this);
        return copy;
    }

    public void mirrorSocketedItems(ShipSkillData source) {
        revision++;
        socketedItems = source.socketedItems == null ? null : new LinkedHashMap<>(source.socketedItems);
    }

    private ShipSkillData releaseCopy() {
        ShipSkillData copy = planningCopy();
        copy.freeNodeIds = new LinkedHashSet<>(freeNodeIds());
        copy.pairedFreeNodeIds = new LinkedHashSet<>(pairedFreeNodeIds());
        copy.bankedFreeAllocations = bankedFreeAllocations;
        copy.chargedItemIds = new LinkedHashMap<>();
        copy.chargedItemQuantities = new LinkedHashMap<>();
        copy.npcBuild = npcBuild;
        return copy;
    }

    public Set<String> previewRelease(SkillTreeTopology topology, String satisfiedRootId, Consumer<ShipSkillData> change) {
        ShipSkillData copy = releaseCopy();
        Set<String> reachableBefore = copy.reachableAllocatedNodeIds(topology, satisfiedRootId);
        change.accept(copy);
        copy.releaseNewlyStranded(topology, satisfiedRootId, reachableBefore);
        Set<String> released = new LinkedHashSet<>(allocatedNodeIds);
        released.removeAll(copy.allocatedNodeIds);
        return released;
    }

    public int paidNodeCount(Collection<String> nodeIds) {
        String startingRoot = resolveStartingRootId();
        int paid = 0;
        for (String nodeId : nodeIds) {
            if (isAllocated(nodeId) && !isFreeNode(nodeId) && !nodeId.equals(startingRoot)) {
                paid++;
            }
        }
        return paid;
    }

    public Set<String> reachableAllocatedNodeIds(SkillTreeTopology topology, String satisfiedRootId) {
        return reachableAllocatedNodeIds(topology, satisfiedRootId, Set.of());
    }

    public ReleasedNodes releaseNewlyStranded(SkillTreeTopology topology, String satisfiedRootId, Set<String> reachableBefore) {
        return releaseUnreachable(topology, satisfiedRootId, node -> reachableBefore.contains(node.getId()));
    }

    public ReleasedNodes releaseUnreachable(SkillTreeTopology topology, String satisfiedRootId) {
        return releaseUnreachable(topology, satisfiedRootId, node -> true);
    }

    public ReleasedNodes releaseUnreachable(SkillTreeTopology topology, String satisfiedRootId, Predicate<SkillNode> releasable) {
        if (satisfiedRootId == null || !isAllocated(satisfiedRootId)) {
            return ReleasedNodes.NONE;
        }
        Set<String> allocatedBefore = new LinkedHashSet<>(allocatedNodeIds);
        for (String nodeId : ungrantedFreeNodeIds()) {
            SkillNode node = topology.node(nodeId);
            if (node != null && releasable.test(node)) {
                deallocate(node);
            }
        }
        Set<String> reachable = reachableAllocatedNodeIds(topology, satisfiedRootId, Set.of());
        for (String nodeId : List.copyOf(allocatedNodeIds)) {
            SkillNode node = topology.node(nodeId);
            if (node != null && isAllocated(nodeId) && !reachable.contains(nodeId) && !nodeId.equals(satisfiedRootId) && releasable.test(node)) {
                deallocate(node);
            }
        }
        allocatedBefore.removeAll(allocatedNodeIds);
        if (allocatedBefore.isEmpty()) {
            return ReleasedNodes.NONE;
        }
        return new ReleasedNodes(List.copyOf(allocatedBefore), takeItemCharges(allocatedBefore));
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
        return allocatedNodeIds.isEmpty() && getDormantNodeIds().isEmpty() && level == 0 && xp == 0f && bankedFreeAllocations == 0 && frameworkPoints == 0;
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

    public void capLevel(int maxLevel) {
        if (level > maxLevel) {
            level = Math.max(0, maxLevel);
            revision++;
        }
    }

    public void addFreeAllocationCredit() {
        bankedFreeAllocations++;
    }

    public List<String> trimPaidNodes(Map<String, SkillNode> tree, SkillTreeTopology topology) {
        revision++;
        String startingRoot = resolveStartingRootId(tree.values());
        List<String> removedNodeIds = new ArrayList<>();
        while (paidNodeCount() > 0) {
            if (bankedFreeAllocations > 0 && convertMostRecentAllocationToFree(tree.values())) {
                bankedFreeAllocations--;
                continue;
            }
            SkillNode removable = newestRemovableNode(tree, topology, startingRoot);
            if (removable == null) {
                break;
            }
            deallocate(removable);
            removedNodeIds.add(removable.getId());
        }
        return removedNodeIds;
    }

    public List<String> trimFreeAllocationsToLevel(Map<String, SkillNode> tree, SkillTreeTopology topology) {
        revision++;
        String startingRoot = resolveStartingRootId(tree.values());
        List<String> removedNodeIds = new ArrayList<>();
        bankedFreeAllocations = Math.max(0, Math.min(bankedFreeAllocations, level - freeNodeIds().size()));
        while (freeNodeIds().size() > level) {
            SkillNode removable = newestRemovableNode(tree, topology, startingRoot, freeNodeIds()::contains);
            if (removable == null) {
                break;
            }
            deallocate(removable);
            bankedFreeAllocations = Math.max(0, bankedFreeAllocations - 1);
            removedNodeIds.add(removable.getId());
        }
        return removedNodeIds;
    }

    private SkillNode newestRemovableNode(Map<String, SkillNode> tree, SkillTreeTopology topology, String startingRoot) {
        return newestRemovableNode(tree, topology, startingRoot, nodeId -> true);
    }

    private SkillNode newestRemovableNode(Map<String, SkillNode> tree, SkillTreeTopology topology, String startingRoot,
                                          Predicate<String> candidate) {
        List<String> order = new ArrayList<>(allocatedNodeIds);
        for (int i = order.size() - 1; i >= 0; i--) {
            String nodeId = order.get(i);
            SkillNode node = tree.get(nodeId);
            if (node != null && candidate.test(nodeId) && !nodeId.equals(startingRoot) && !pairedFreeNodeIds().contains(nodeId)
                    && anchorPartner(nodeId) == null && !isGrantHeld(nodeId) && !grantedNodesBySocket().containsKey(nodeId)
                    && canDeallocate(node, topology, startingRoot)) {
                return node;
            }
        }
        return null;
    }

    public boolean convertMostRecentAllocationToFree(Collection<SkillNode> allNodes) {
        revision++;
        String startingRoot = resolveStartingRootId(allNodes);
        List<String> order = new ArrayList<>(allocatedNodeIds);
        for (int i = order.size() - 1; i >= 0; i--) {
            String nodeId = order.get(i);
            boolean alreadyFree = isFreeNode(nodeId);
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

        String pairedId = wormholePartner(node);
        if (pairedId != null && !isAllocated(pairedId)) {
            discardItemCharge(pairedId);
            allocatedNodeIds.add(pairedId);
            pairedFreeNodeIds().add(pairedId);
        }
    }

    public void deallocate(SkillNode node) {
        String pairedId = wormholePartner(node);
        revision++;
        ensureItemChargeLedger();
        allocatedNodeIds.remove(node.getId());
        release(node.getId());

        if (pairedId != null && allocatedNodeIds.remove(pairedId)) {
            release(pairedId);
        }
    }

    public List<SkillItemCost> deallocateReleasingCharges(SkillNode node) {
        Set<String> allocatedBefore = new LinkedHashSet<>(allocatedNodeIds);
        deallocate(node);
        allocatedBefore.removeAll(allocatedNodeIds);
        return takeItemCharges(allocatedBefore);
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
        if (grantFreeNodeIds != null && grantFreeNodeIds.remove(oldId)) {
            grantFreeNodeIds.add(newId);
        }
        if (oldId.equals(startingRootId)) {
            startingRootId = newId;
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
        return node.getType().isOptional() && NodeEligibility.optionProblem(node.getType(), getOptionalSelection(node.getId()), types::get) != null;
    }

    public interface WakeCheck {
        boolean allows(SkillNode dormantNode);
    }

    public boolean hasReturningDormantNodes(Map<String, SkillNode> tree) {
        for (String nodeId : getDormantNodeIds()) {
            if (tree.containsKey(nodeId)) {
                return true;
            }
        }
        return false;
    }

    public List<String> wakeDormantNodes(Map<String, SkillNode> tree, int maxAllocatedNodes, WakeCheck wakeCheck) {
        if (getDormantNodeIds().isEmpty()) {
            return List.of();
        }
        revision++;
        ensureItemChargeLedger();
        String satisfiedRootId = resolveStartingRootId(tree.values());
        SkillTreeTopology wakeTopology = grantedNodesBySocket().isEmpty() ? null : SkillTreeTopology.of(tree.values());
        boolean wokeAny = true;
        while (wokeAny) {
            wokeAny = false;
            Set<String> grantOnlyNodeIds = wakeTopology == null ? Set.of() : grantHeldOffPath(wakeTopology, satisfiedRootId);
            for (String nodeId : List.copyOf(dormantNodeIds)) {
                SkillNode node = tree.get(nodeId);
                if (node != null && (!isGrantFree(nodeId) || grantingSocket(nodeId) != null) && isReconnected(node, satisfiedRootId, grantOnlyNodeIds)) {
                    wokeAny |= wake(node, satisfiedRootId, maxAllocatedNodes, wakeCheck);
                }
            }
        }
        for (String nodeId : List.copyOf(dormantNodeIds)) {
            SkillNode node = tree.get(nodeId);
            if (node != null && node.getType().getTier() != SkillTier.WORMHOLE_EXIT && grantingSocket(nodeId) != null) {
                wake(node, satisfiedRootId, maxAllocatedNodes, wakeCheck);
            }
        }
        List<String> released = new ArrayList<>();
        SkillTreeTopology topology = null;
        for (String nodeId : List.copyOf(dormantNodeIds)) {
            SkillNode node = tree.get(nodeId);
            if (node == null) {
                continue;
            }
            String partnerId = wormholePartner(node);
            boolean freePartnerOfAllocatedEnd = pairedFreeNodeIds().contains(nodeId) && allocatedNodeIds.contains(partnerId)
                    && anchorPartner(nodeId) == null;
            dormantNodeIds.remove(nodeId);
            release(nodeId);
            released.add(nodeId);
            SkillNode allocatedEnd = freePartnerOfAllocatedEnd ? tree.get(partnerId) : null;
            if (allocatedEnd != null) {
                topology = topology != null ? topology : SkillTreeTopology.of(tree.values());
                if (canDeallocate(allocatedEnd, topology, satisfiedRootId)) {
                    deallocate(allocatedEnd);
                    released.add(allocatedEnd.getId());
                }
            }
        }
        return released;
    }

    private boolean wake(SkillNode node, String satisfiedRootId, int maxAllocatedNodes, WakeCheck wakeCheck) {
        if (!isGrantFree(node.getId()) && usedNodeSlots() >= maxAllocatedNodes || !wakeCheck.allows(node)) {
            return false;
        }
        String nodeId = node.getId();
        revision++;
        dormantNodeIds.remove(nodeId);
        allocatedNodeIds.add(nodeId);
        if (!isFreeNode(nodeId) && !nodeId.equals(satisfiedRootId)) {
            charge(nodeId, 1);
        }
        return true;
    }

    private Set<String> grantHeldOffPath(SkillTreeTopology topology, String satisfiedRootId) {
        Set<String> pathReached = pathReachableAllocatedNodeIds(topology, satisfiedRootId, Set.of());
        Set<String> grantHeld = new HashSet<>();
        for (String grantedNodeId : grantedNodeIds()) {
            if (isAllocated(grantedNodeId) && !pathReached.contains(grantedNodeId)) {
                grantHeld.add(grantedNodeId);
            }
        }
        return grantHeld;
    }

    private boolean isReconnected(SkillNode node, String satisfiedRootId, Set<String> grantOnlyNodeIds) {
        if (node.getType().getTier() == SkillTier.WORMHOLE_EXIT) {
            String socketId = anchorSocket(node.getId());
            return socketId != null && isAllocated(socketId);
        }
        for (String connectedId : node.getConnectedNodeIds()) {
            if (isSatisfied(connectedId, satisfiedRootId) && !grantOnlyNodeIds.contains(connectedId)) {
                return true;
            }
        }
        return false;
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
        if (grantFreeNodeIds != null) {
            grantFreeNodeIds.clear();
        }
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
        if (grantFreeNodeIds != null) {
            grantFreeNodeIds.remove(nodeId);
        }
    }

    public boolean canDeallocate(SkillNode node, Collection<SkillNode> allNodes, String satisfiedRootId) {
        return canDeallocate(node, SkillTreeTopology.of(allNodes), satisfiedRootId);
    }

    public boolean canDeallocate(SkillNode node, SkillTreeTopology topology, String satisfiedRootId) {
        Set<String> excluded = new HashSet<>();
        excluded.add(node.getId());
        String pairedId = wormholePartner(node);
        if (pairedId != null) excluded.add(pairedId);

        Set<String> reachableBefore = reachableAllocatedNodeIds(topology, satisfiedRootId, Set.of());
        Set<String> reachableAfter = reachableAllocatedNodeIds(topology, satisfiedRootId, excluded);
        Set<String> closingRelease = closingRelease(node, topology, satisfiedRootId, reachableBefore);
        List<String> grantedNodeIds = grantedNodesBySocket().getOrDefault(node.getId(), List.of());

        for (String allocatedId : reachableBefore) {
            if (excluded.contains(allocatedId) || closingRelease.contains(allocatedId) || grantedNodeIds.contains(allocatedId)) continue;
            if (!reachableAfter.contains(allocatedId)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> closingRelease(SkillNode node, SkillTreeTopology topology, String satisfiedRootId, Set<String> reachableBefore) {
        String exitId = anchorExit(node.getId());
        if (exitId == null && anchorSocket(node.getId()) != null) {
            exitId = node.getId();
        }
        if (exitId == null) {
            return Set.of();
        }
        Set<String> released = new HashSet<>(reachableBefore);
        released.removeAll(reachableAllocatedNodeIds(topology, satisfiedRootId, Set.of(exitId)));
        return released;
    }

    Set<String> reachableAllocatedNodeIds(SkillTreeTopology topology, String satisfiedRootId, Set<String> excludedNodeIds) {
        Set<String> reached = pathReachableAllocatedNodeIds(topology, satisfiedRootId, excludedNodeIds);
        reached.addAll(grantedReachableNodeIds(topology, reached, excludedNodeIds));
        return reached;
    }

    private List<String> grantedReachableNodeIds(SkillTreeTopology topology, Set<String> pathReached, Set<String> excludedNodeIds) {
        List<String> granted = new ArrayList<>();
        for (Map.Entry<String, List<String>> grantLink : grantedNodesBySocket().entrySet()) {
            if (!pathReached.contains(grantLink.getKey())) {
                continue;
            }
            for (String grantedNodeId : grantLink.getValue()) {
                if (isAllocated(grantedNodeId) && !excludedNodeIds.contains(grantedNodeId) && topology.node(grantedNodeId) != null) {
                    granted.add(grantedNodeId);
                }
            }
        }
        return granted;
    }

    private Set<String> pathReachableAllocatedNodeIds(SkillTreeTopology topology, String satisfiedRootId, Set<String> excludedNodeIds) {
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
                child -> !excludedNodeIds.contains(child.getId()) && isAllocated(child.getId())
                        && child.getType().getTier() != SkillTier.WORMHOLE_EXIT,
                child -> allocatedWormholePartner(child, excludedNodeIds)).reached());
    }

    private String allocatedWormholePartner(SkillNode node, Set<String> excludedNodeIds) {
        String partnerId = wormholePartner(node);
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

    public List<SkillItemCost> takeItemCharges(Collection<String> nodeIds) {
        List<SkillItemCost> charges = new ArrayList<>();
        for (String nodeId : nodeIds) {
            SkillItemCost charged = takeItemCharge(nodeId);
            if (charged != null) {
                charges.add(charged);
            }
        }
        return charges;
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
