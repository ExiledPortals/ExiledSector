package exiledsector.items;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.tags.ShipProfile;

import java.util.Map;

public final class HullUpgradeRules {

    public static final int MAX_POINTS = 4;

    public enum UnlockBlock {NO_POINTS, ALREADY_UNLOCKED, UNMET_REQUIREMENT}

    public enum UpgradeBlock {AT_MAX_POINTS, NONE_IN_STORAGE}

    private HullUpgradeRules() {
    }

    public static UpgradeBlock upgradeBlock(ShipSkillData shipData, int upgradesInStorage) {
        if (shipData.getHullUpgradePoints() >= MAX_POINTS) {
            return UpgradeBlock.AT_MAX_POINTS;
        }
        return upgradesInStorage <= 0 ? UpgradeBlock.NONE_IN_STORAGE : null;
    }

    public static boolean installUpgrade(ShipSkillData shipData, HullSize shipHullSize, ItemStore store) {
        if (upgradeBlock(shipData, store.upgradeCount(shipHullSize)) != null || !store.takeUpgrade(shipHullSize)) {
            return false;
        }
        return shipData.addHullUpgradePoint(MAX_POINTS);
    }

    public static UnlockBlock unlockBlock(ShipSkillData shipData, ShipSection section, ShipProfile currentFit) {
        if (shipData.isSocketTypeUnlocked(section.id())) {
            return UnlockBlock.ALREADY_UNLOCKED;
        }
        if (ShipSections.unmetRequirement(section, currentFit) != null) {
            return UnlockBlock.UNMET_REQUIREMENT;
        }
        return shipData.getUnspentHullUpgradePoints() <= 0 ? UnlockBlock.NO_POINTS : null;
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
        return canSocket(shipData, section, item, shipDataById) && shipData.installInSection(section.id(), item.id());
    }
}
