package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.List;

public final class BlackHoleSalvage {

    static final String HULL_ITEM_PERCENT_KEY = "exiledSector_blackHoleHullItemPercent";

    private BlackHoleSalvage() {
    }

    public static float hullItemPercent(CampaignFleetAPI fleet) {
        return fleet == null || fleet.getFleetData() == null ? 0f : hullItemPercent(fleet.getFleetData().getMembersListCopy());
    }

    static float hullItemPercent(List<FleetMemberAPI> members) {
        return FleetContributions.sum(members, HULL_ITEM_PERCENT_KEY);
    }
}
