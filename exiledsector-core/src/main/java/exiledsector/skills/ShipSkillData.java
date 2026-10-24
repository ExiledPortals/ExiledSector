package exiledsector.skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShipSkillData {

    final Set<String> allocatedNodeIds = new LinkedHashSet<>();
    Map<String, String> optionalSelections = new LinkedHashMap<>();
    int level = 0;
    float xp = 0f;
    int bankedFreeAllocations = 0;
    Set<String> freeNodeIds = new LinkedHashSet<>();
    Set<String> pairedFreeNodeIds = new LinkedHashSet<>();
    Set<String> grantFreeNodeIds;
    Set<String> replacementAllocatedNodeIds;
    String startingRootId;
    boolean npcBuild;
    transient int revision;
    Map<String, String> socketedItems;
    Map<String, String> chargedItemIds;
    Map<String, Float> chargedItemQuantities;
    Set<String> dormantNodeIds;
    int frameworkPoints;
    List<String> unlockedSocketTypes;
    Map<String, String> frameworkItemsBySocketType;
    boolean autoAllocateTemplate;
    transient int pendingTemplateCredits;
    transient LinkCache<AnchorLinkMaps> anchorLinks;
    transient LinkCache<Map<String, List<String>>> grantLinks;

    record AnchorLinkMaps(Map<String, String> exitBySocketId, Map<String, String> socketByExitId) {
    }

    LinkCache<AnchorLinkMaps> anchorLinkCache() {
        if (anchorLinks == null) anchorLinks = new LinkCache<>();
        return anchorLinks;
    }

    LinkCache<Map<String, List<String>>> grantLinkCache() {
        if (grantLinks == null) grantLinks = new LinkCache<>();
        return grantLinks;
    }

    Set<String> freeNodeIds() {
        if (freeNodeIds == null) freeNodeIds = new LinkedHashSet<>();
        return freeNodeIds;
    }

    Set<String> dormantNodeIds() {
        if (dormantNodeIds == null) dormantNodeIds = new LinkedHashSet<>();
        return dormantNodeIds;
    }

    Set<String> pairedFreeNodeIds() {
        if (pairedFreeNodeIds == null) pairedFreeNodeIds = new LinkedHashSet<>();
        return pairedFreeNodeIds;
    }

    Set<String> replacementAllocatedNodeIds() {
        if (replacementAllocatedNodeIds == null) replacementAllocatedNodeIds = new LinkedHashSet<>();
        return replacementAllocatedNodeIds;
    }

    Set<String> grantFreeNodeIds() {
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

    public int getStoredHullUpgradePoints() {
        return frameworkPoints;
    }

    public void setStoredHullUpgradePoints(int storedPoints) {
        revision++;
        frameworkPoints = Math.max(0, storedPoints);
    }

    public List<String> getUnlockedSocketTypeIds() {
        return unlockedSocketTypes == null ? List.of() : Collections.unmodifiableList(unlockedSocketTypes);
    }

    public boolean isSocketTypeUnlocked(String socketTypeId) {
        return unlockedSocketTypes != null && unlockedSocketTypes.contains(socketTypeId);
    }

    public boolean unlockSocketType(String socketTypeId) {
        revision++;
        if (socketTypeId == null || isSocketTypeUnlocked(socketTypeId)) {
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

    public List<String> clearHullUpgrades() {
        revision++;
        List<String> removedItemIds = new ArrayList<>(getSectionItems().values());
        unlockedSocketTypes = null;
        frameworkItemsBySocketType = null;
        frameworkPoints = 0;
        return removedItemIds;
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

    public boolean canUnchooseStartingRoot() {
        return startingRootId != null && allocatedNodeIds.size() == 1 && allocatedNodeIds.contains(startingRootId);
    }

    public boolean isSatisfied(String nodeId, String satisfiedRootId) {
        return isAllocated(nodeId) || nodeId.equals(satisfiedRootId);
    }

    public int revision() {
        return revision;
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

    public Set<String> getAllocatedNodeIds() {
        return allocatedNodeIds;
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

    public void mirrorSocketedItems(ShipSkillData source) {
        revision++;
        socketedItems = source.socketedItems == null ? null : new LinkedHashMap<>(source.socketedItems);
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
        return allocatedNodeIds.isEmpty() && getDormantNodeIds().isEmpty() && level == 0 && xp == 0f && bankedFreeAllocations == 0 && frameworkPoints == 0
                && (unlockedSocketTypes == null || unlockedSocketTypes.isEmpty())
                && (frameworkItemsBySocketType == null || frameworkItemsBySocketType.isEmpty());
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

    public Set<String> getReplacementAllocatedNodeIds() {
        return replacementAllocatedNodeIds == null ? Set.of() : Collections.unmodifiableSet(replacementAllocatedNodeIds);
    }

    public void keepAsOriginal(String nodeId) {
        if (replacementAllocatedNodeIds != null) {
            replacementAllocatedNodeIds.remove(nodeId);
        }
    }

    void charge(String nodeId, int opCost) {
        if (opCost > 0 && bankedFreeAllocations > 0) {
            bankedFreeAllocations--;
            freeNodeIds().add(nodeId);
        }
    }

    void release(String nodeId) {
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
        if (replacementAllocatedNodeIds != null) {
            replacementAllocatedNodeIds.remove(nodeId);
        }
    }
}
