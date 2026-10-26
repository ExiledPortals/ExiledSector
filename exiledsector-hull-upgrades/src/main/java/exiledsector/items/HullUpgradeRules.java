package exiledsector.items;

import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.tags.ShipProfile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class HullUpgradeRules {

    public enum UnlockBlock {NO_POINTS, ALREADY_UNLOCKED, UNMET_REQUIREMENT}

    public enum UpgradeBlock {STAR_CHART_PROGRESS, AT_MAX_POINTS, WRONG_TIER, NONE_IN_STORAGE}

    private HullUpgradeRules() {
    }

    public static UpgradeBlock upgradeBlock(ShipSkillData shipData, RefitSchematic schematic, int schematicsInStorage) {
        if (!HullUpgradePoints.schematicsInUse()) {
            return UpgradeBlock.STAR_CHART_PROGRESS;
        }
        int shipPoints = HullUpgradePoints.of(shipData);
        if (shipPoints >= HullUpgradePoints.MOST_POINTS) {
            return UpgradeBlock.AT_MAX_POINTS;
        }
        if (shipPoints != schematic.requiredPoints()) {
            return UpgradeBlock.WRONG_TIER;
        }
        return schematicsInStorage <= 0 ? UpgradeBlock.NONE_IN_STORAGE : null;
    }

    public static boolean applySchematic(ShipSkillData shipData, RefitSchematic schematic, ItemStore store) {
        if (upgradeBlock(shipData, schematic, store.schematicCount(schematic)) != null || !store.takeSchematic(schematic)) {
            return false;
        }
        shipData.setStoredHullUpgradePoints(schematic.points());
        return true;
    }

    public static UnlockBlock unlockBlock(ShipSkillData shipData, ShipSection section, ShipProfile currentFit) {
        if (shipData.isSocketTypeUnlocked(section.id())) {
            return UnlockBlock.ALREADY_UNLOCKED;
        }
        if (ShipSections.unmetRequirement(section, currentFit) != null) {
            return UnlockBlock.UNMET_REQUIREMENT;
        }
        return HullUpgradePoints.unspent(shipData) <= 0 ? UnlockBlock.NO_POINTS : null;
    }

    public static boolean unlock(ShipSkillData shipData, ShipSection section, ShipProfile currentFit) {
        return unlockBlock(shipData, section, currentFit) == null && shipData.unlockSocketType(section.id());
    }

    public static boolean lock(ShipSkillData shipData, ShipSection section) {
        if (!shipData.isSocketTypeUnlocked(section.id())) {
            return false;
        }
        shipData.lockSocketType(section.id());
        return true;
    }

    public static boolean canSocket(ShipSkillData shipData, ShipSection section, Item item, Map<String, ShipSkillData> shipDataById) {
        return shipData.isSocketTypeUnlocked(section.id()) && HullItem.fits(item, section)
                && !SocketCustody.isInstalled(shipDataById, item);
    }

    public static boolean socket(ShipSkillData shipData, ShipSection section, Item item, Map<String, ShipSkillData> shipDataById) {
        return socket(shipData, section, item, shipDataById, hullModId -> false);
    }

    public static boolean socket(ShipSkillData shipData, ShipSection section, Item item, Map<String, ShipSkillData> shipDataById,
                                 Predicate<String> hasHullMod) {
        return canSocket(shipData, section, item, shipDataById) && ItemHullModExclusions.fittedConflict(item, hasHullMod) == null
                && shipData.installInSection(section.id(), item.id());
    }

    public static Map<String, Item> excludedHullMods(ShipSkillData shipData) {
        Map<String, Item> excluded = new LinkedHashMap<>();
        for (Map.Entry<String, String> sectionEntry : shipData.getSectionItems().entrySet()) {
            Item item = ItemStore.lookup(sectionEntry.getValue());
            if (item != null && HullItem.fits(item, ShipSection.byIdOrNull(sectionEntry.getKey()))) {
                ItemHullModExclusions.exclusiveHullModIds(item).forEach(hullModId -> excluded.putIfAbsent(hullModId, item));
            }
        }
        return excluded;
    }
}
