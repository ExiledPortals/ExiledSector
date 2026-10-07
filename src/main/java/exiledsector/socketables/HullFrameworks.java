package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.FrameworkSlots;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.tags.ShipProfile;

import java.util.Map;

public final class HullFrameworks {

    public enum BlockReason {WRONG_HULL_SIZE, UNMET_REQUIREMENT, INSTALLED_ELSEWHERE}

    public record InstallBlock(BlockReason reason, SocketType socketType, String requirementTag) {
    }

    private HullFrameworks() {
    }

    public static InstallBlock installBlock(HullFramework framework, HullSize shipHullSize, ShipProfile currentFit, String installedOnShipId,
                                            String shipId) {
        if (installedOnShipId != null && !installedOnShipId.equals(shipId)) {
            return new InstallBlock(BlockReason.INSTALLED_ELSEWHERE, null, null);
        }
        if (framework.hullSize() != shipHullSize) {
            return new InstallBlock(BlockReason.WRONG_HULL_SIZE, null, null);
        }
        for (SocketType socketType : framework.socketTypes()) {
            String unmetRequirement = FrameworkSlots.unmetRequirement(socketType, currentFit);
            if (unmetRequirement != null) {
                return new InstallBlock(BlockReason.UNMET_REQUIREMENT, socketType, unmetRequirement);
            }
        }
        return null;
    }

    public static boolean install(ShipSkillData shipData, HullFramework framework, Map<String, ShipSkillData> shipDataById) {
        if (framework.id().equals(shipData.getInstalledFrameworkId())) {
            return true;
        }
        String installedOnShipId = SocketCustody.frameworkShipId(shipDataById, framework);
        if (installedOnShipId != null) {
            return false;
        }
        shipData.removeFramework();
        shipData.installFramework(framework.id());
        return true;
    }

    public static String remove(ShipSkillData shipData) {
        return shipData.removeFramework();
    }

    public static boolean canSocket(ShipSkillData shipData, int slotIndex, Socketable socketable, Map<String, ShipSkillData> shipDataById) {
        HullFramework framework = FrameworkSlots.installedFramework(shipData);
        SocketType slotType = framework == null ? null : framework.socketType(slotIndex);
        return slotType != null && socketable.canSocketInto(slotType) && !SocketCustody.installations(shipDataById).containsKey(socketable.id());
    }

    public static boolean socket(ShipSkillData shipData, int slotIndex, Socketable socketable, Map<String, ShipSkillData> shipDataById) {
        return canSocket(shipData, slotIndex, socketable, shipDataById) && shipData.socketFrameworkItem(slotIndex, socketable.id());
    }
}
