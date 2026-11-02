package exiledsector.skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShipSkillData {

    public static final Map<String, String> SAVED_FIELD_NAMES = Map.of("unlockedSections", "unlockedSocketTypes",
            "hullItemsBySection", "frameworkItemsBySocketType", "paidItemCostIds", "chargedItemIds", "paidItemCostQuantities", "chargedItemQuantities",
            "pairGrantedNodeIds", "pairedFreeNodeIds", "subroutineGrantedNodeIds", "grantFreeNodeIds");

    final Set<String> allocatedNodeIds = new LinkedHashSet<>();
    Map<String, String> optionalSelections = new LinkedHashMap<>();
    int level = 0;
    float xp = 0f;
    int bankedFreeAllocations = 0;
    Set<String> freeNodeIds = new LinkedHashSet<>();
    Set<String> pairGrantedNodeIds = new LinkedHashSet<>();
    Set<String> subroutineGrantedNodeIds;
    transient Set<String> fleetGrantNodeIds;
    Set<String> replacementAllocatedNodeIds;
    String startingRootId;
    boolean npcBuild;
    transient int revision;
    Map<String, String> socketedItems;
    Map<String, String> paidItemCostIds;
    Map<String, Float> paidItemCostQuantities;
    Set<String> dormantNodeIds;
    int frameworkPoints;
    boolean skillTreeUnlocked;
    List<String> unlockedSections;
    Map<String, String> hullItemsBySection;
    boolean autoAllocateTemplate;
    transient int pendingTemplateFreeAllocations;
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

    Set<String> pairGrantedNodeIds() {
        if (pairGrantedNodeIds == null) pairGrantedNodeIds = new LinkedHashSet<>();
        return pairGrantedNodeIds;
    }

    Set<String> replacementAllocatedNodeIds() {
        if (replacementAllocatedNodeIds == null) replacementAllocatedNodeIds = new LinkedHashSet<>();
        return replacementAllocatedNodeIds;
    }

    Set<String> subroutineGrantedNodeIds() {
        if (subroutineGrantedNodeIds == null) subroutineGrantedNodeIds = new LinkedHashSet<>();
        return subroutineGrantedNodeIds;
    }

    Set<String> fleetGrantNodeIds() {
        if (fleetGrantNodeIds == null) fleetGrantNodeIds = new LinkedHashSet<>();
        return fleetGrantNodeIds;
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

    public boolean isMasterSchematicd() {
        return skillTreeUnlocked;
    }

    public boolean unlockSkillTree() {
        if (skillTreeUnlocked) {
            return false;
        }
        revision++;
        skillTreeUnlocked = true;
        return true;
    }

    public List<String> getUnlockedSectionIds() {
        return unlockedSections == null ? List.of() : Collections.unmodifiableList(unlockedSections);
    }

    public boolean isSectionUnlocked(String sectionKindId) {
        return unlockedSections != null && unlockedSections.contains(sectionKindId);
    }

    public boolean unlockSection(String sectionKindId) {
        revision++;
        if (sectionKindId == null || isSectionUnlocked(sectionKindId)) {
            return false;
        }
        if (unlockedSections == null) unlockedSections = new ArrayList<>();
        unlockedSections.add(sectionKindId);
        return true;
    }

    public void grantUnlockedSection(String sectionKindId) {
        revision++;
        if (sectionKindId == null || isSectionUnlocked(sectionKindId)) {
            return;
        }
        frameworkPoints++;
        if (unlockedSections == null) unlockedSections = new ArrayList<>();
        unlockedSections.add(sectionKindId);
    }

    public List<String> clearHullUpgrades() {
        revision++;
        List<String> removedItemIds = new ArrayList<>(getSectionItems().values());
        unlockedSections = null;
        hullItemsBySection = null;
        frameworkPoints = 0;
        return removedItemIds;
    }

    public String lockSection(String sectionKindId) {
        revision++;
        if (unlockedSections == null || !unlockedSections.remove(sectionKindId)) {
            return null;
        }
        return hullItemsBySection == null ? null : hullItemsBySection.remove(sectionKindId);
    }

    public Map<String, String> getSectionItems() {
        return hullItemsBySection == null ? Map.of() : Collections.unmodifiableMap(hullItemsBySection);
    }

    public List<String> getInstalledItemIds() {
        List<String> installedIds = new ArrayList<>(getSocketedItems().values());
        installedIds.addAll(getSectionItems().values());
        return installedIds;
    }

    public boolean holdsItem(String itemId) {
        return getSocketedItems().containsValue(itemId) || getSectionItems().containsValue(itemId);
    }

    public String getSectionItem(String sectionKindId) {
        return hullItemsBySection == null ? null : hullItemsBySection.get(sectionKindId);
    }

    public boolean installInSection(String sectionKindId, String itemId) {
        revision++;
        if (!isSectionUnlocked(sectionKindId)) {
            return false;
        }
        if (hullItemsBySection == null) hullItemsBySection = new LinkedHashMap<>();
        hullItemsBySection.put(sectionKindId, itemId);
        return true;
    }

    public String removeFromSection(String sectionKindId) {
        revision++;
        return hullItemsBySection == null ? null : hullItemsBySection.remove(sectionKindId);
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

    public boolean isFleetGranted(String nodeId) {
        return fleetGrantNodeIds != null && fleetGrantNodeIds.contains(nodeId);
    }

    public Set<String> getFleetGrantNodeIds() {
        return fleetGrantNodeIds == null ? Set.of() : Collections.unmodifiableSet(fleetGrantNodeIds);
    }

    public boolean isGrantFree(String nodeId) {
        return subroutineGrantedNodeIds != null && subroutineGrantedNodeIds.contains(nodeId);
    }

    public int usedNodeSlots() {
        if (subroutineGrantedNodeIds == null || subroutineGrantedNodeIds.isEmpty()) {
            return allocatedNodeIds.size();
        }
        int used = 0;
        for (String nodeId : allocatedNodeIds) {
            if (!subroutineGrantedNodeIds.contains(nodeId)) {
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
        return freeNodeIds().contains(nodeId) || pairGrantedNodeIds().contains(nodeId) || isGrantFree(nodeId);
    }

    public boolean isAutoAllocatingTemplate() {
        return autoAllocateTemplate;
    }

    public void setAutoAllocateTemplate(boolean autoAllocate) {
        autoAllocateTemplate = autoAllocate;
    }

    public void bankLevelUpForTemplate() {
        bankedFreeAllocations++;
        pendingTemplateFreeAllocations++;
    }

    public int takePendingTemplateFreeAllocations() {
        int pending = pendingTemplateFreeAllocations;
        pendingTemplateFreeAllocations = 0;
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
                && !skillTreeUnlocked && (unlockedSections == null || unlockedSections.isEmpty())
                && (hullItemsBySection == null || hullItemsBySection.isEmpty());
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

    public void addFreeAllocation() {
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
            pairGrantedNodeIds().remove(nodeId);
        }
        if (subroutineGrantedNodeIds != null) {
            subroutineGrantedNodeIds.remove(nodeId);
        }
        if (fleetGrantNodeIds != null) {
            fleetGrantNodeIds.remove(nodeId);
        }
        if (replacementAllocatedNodeIds != null) {
            replacementAllocatedNodeIds.remove(nodeId);
        }
    }
}
