package exiledsector.ui.refit;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.persistence.ShipRecords;
import exiledsector.persistence.ShipSkillDataManager;

public final class RefitModuleTabs {

    private RefitModuleTabs() {
    }

    public static boolean isModuleTab(FleetMemberAPI member) {
        if (member == null) return false;
        FleetDataAPI fleetData = member.getFleetData();
        return fleetData != null && !fleetData.getMembersListCopy().contains(member);
    }

    public static boolean showsDockedShip(FleetMemberAPI member, ShipVariantAPI variant) {
        return dockedShipId(member, variant) != null;
    }

    public static void adoptDockedShip(FleetMemberAPI member, ShipVariantAPI variant) {
        String dockedShipId = dockedShipId(member, variant);
        if (dockedShipId != null && !dockedShipId.equals(member.getId())) {
            member.setId(dockedShipId);
        }
    }

    private static String dockedShipId(FleetMemberAPI member, ShipVariantAPI variant) {
        String dockedShipId = ShipRecords.taggedShipId(variant);
        return dockedShipId != null && ShipSkillDataManager.find(dockedShipId) != null && isModuleTab(member) ? dockedShipId : null;
    }
}
