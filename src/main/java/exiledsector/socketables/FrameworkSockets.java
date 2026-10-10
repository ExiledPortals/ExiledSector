package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.FrameworkSlots;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.tags.ShipProfile;

import java.util.Map;

public final class FrameworkSockets {

    public static final int MAX_POINTS = 4;

    public enum UnlockBlock {NO_POINTS, ALREADY_UNLOCKED, UNMET_REQUIREMENT}

    public enum UpgradeBlock {AT_MAX_POINTS, NONE_IN_STORAGE}

    private FrameworkSockets() {
    }

    public static UpgradeBlock upgradeBlock(ShipSkillData shipData, int upgradesInStorage) {
        if (shipData.getFrameworkPoints() >= MAX_POINTS) {
            return UpgradeBlock.AT_MAX_POINTS;
        }
        return upgradesInStorage <= 0 ? UpgradeBlock.NONE_IN_STORAGE : null;
    }

    public static boolean installUpgrade(ShipSkillData shipData, HullSize shipHullSize, SocketableStore store) {
        if (upgradeBlock(shipData, store.upgradeCount(shipHullSize)) != null || !store.takeUpgrade(shipHullSize)) {
            return false;
        }
        return shipData.addFrameworkPoint(MAX_POINTS);
    }

    public static UnlockBlock unlockBlock(ShipSkillData shipData, SocketType socketType, ShipProfile currentFit) {
        if (shipData.isSocketTypeUnlocked(socketType.id())) {
            return UnlockBlock.ALREADY_UNLOCKED;
        }
        if (FrameworkSlots.unmetRequirement(socketType, currentFit) != null) {
            return UnlockBlock.UNMET_REQUIREMENT;
        }
        return shipData.getUnspentFrameworkPoints() <= 0 ? UnlockBlock.NO_POINTS : null;
    }

    public static boolean unlock(ShipSkillData shipData, SocketType socketType, ShipProfile currentFit) {
        return socketType.isFramework() && unlockBlock(shipData, socketType, currentFit) == null && shipData.unlockSocketType(socketType.id());
    }

    public static boolean lock(ShipSkillData shipData, SocketType socketType) {
        if (!shipData.isSocketTypeUnlocked(socketType.id())) {
            return false;
        }
        shipData.lockSocketType(socketType.id());
        return true;
    }

    public static boolean canSocket(ShipSkillData shipData, SocketType socketType, Socketable socketable, Map<String, ShipSkillData> shipDataById) {
        return shipData.isSocketTypeUnlocked(socketType.id()) && socketable.canSocketInto(socketType)
                && !SocketCustody.installations(shipDataById).containsKey(socketable.id());
    }

    public static boolean socket(ShipSkillData shipData, SocketType socketType, Socketable socketable, Map<String, ShipSkillData> shipDataById) {
        return canSocket(shipData, socketType, socketable, shipDataById) && shipData.socketFrameworkItem(socketType.id(), socketable.id());
    }
}
