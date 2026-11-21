package exiledsector;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.List;

public final class FleetMembers {

    private FleetMembers() {
    }

    public static List<FleetMemberAPI> of(CampaignFleetAPI fleet) {
        FleetDataAPI fleetData = fleet == null ? null : fleet.getFleetData();
        return fleetData == null ? List.of() : fleetData.getMembersListCopy();
    }

    public static FleetMemberAPI withId(CampaignFleetAPI fleet, String memberId) {
        if (memberId == null) {
            return null;
        }
        for (FleetMemberAPI member : of(fleet)) {
            if (memberId.equals(member.getId())) {
                return member;
            }
        }
        return null;
    }
}
