package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;

import java.util.List;

public final class FoundItemQuantity {

    static final String HULL_ITEM_PERCENT_KEY = "exiledSector_hullItemQuantityPercent";
    static final String CRAFTING_ITEM_PERCENT_KEY = "exiledSector_craftingItemQuantityPercent";

    private FoundItemQuantity() {
    }

    public static float hullItemPercent() {
        return percent(PlayerFleet.orNull(), HULL_ITEM_PERCENT_KEY);
    }

    public static float craftingItemPercent() {
        return percent(PlayerFleet.orNull(), CRAFTING_ITEM_PERCENT_KEY);
    }

    static float percent(CampaignFleetAPI fleet, String percentKey) {
        return fleet == null || fleet.getFleetData() == null ? 0f : percent(fleet.getFleetData().getMembersListCopy(), percentKey);
    }

    static float percent(List<FleetMemberAPI> members, String percentKey) {
        return FleetContributions.sum(members, percentKey);
    }
}
