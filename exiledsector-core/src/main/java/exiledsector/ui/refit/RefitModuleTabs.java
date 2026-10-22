package exiledsector.ui.refit;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public final class RefitModuleTabs {

    private RefitModuleTabs() {
    }

    public static boolean isModuleTab(FleetMemberAPI member) {
        if (member == null) return false;
        FleetDataAPI fleetData = member.getFleetData();
        return fleetData != null && !fleetData.getMembersListCopy().contains(member);
    }
}
