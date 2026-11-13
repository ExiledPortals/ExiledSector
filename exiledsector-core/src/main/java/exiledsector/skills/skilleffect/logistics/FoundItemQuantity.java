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

    public static float hullItemMult() {
        return mult(PlayerFleet.orNull(), HULL_ITEM_PERCENT_KEY);
    }

    public static float craftingItemMult() {
        return mult(PlayerFleet.orNull(), CRAFTING_ITEM_PERCENT_KEY);
    }

    static float mult(CampaignFleetAPI fleet, String percentKey) {
        return fleet == null || fleet.getFleetData() == null ? 1f : mult(fleet.getFleetData().getMembersListCopy(), percentKey);
    }

    static float mult(List<FleetMemberAPI> members, String percentKey) {
        return Math.max(0f, 1f + FleetContributions.sum(members, percentKey) / 100f);
    }
}
