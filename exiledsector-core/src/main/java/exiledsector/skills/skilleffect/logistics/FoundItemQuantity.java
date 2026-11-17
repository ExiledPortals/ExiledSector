package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.loot.DropType;

import java.util.List;

public final class FoundItemQuantity {

    static final String ITEM_PERCENT_KEY = "exiledSector_itemQuantityPercent";
    static final String HULL_ITEM_PERCENT_KEY = "exiledSector_hullItemQuantityPercent";
    static final String CRAFTING_ITEM_PERCENT_KEY = "exiledSector_craftingItemQuantityPercent";
    static final String FEEDSTOCK_PERCENT_KEY = "exiledSector_feedstockQuantityPercent";

    private FoundItemQuantity() {
    }

    public static float nodePercent(DropType node) {
        String percentKey = percentKey(node);
        return percentKey == null ? 0f : percent(PlayerFleet.orNull(), percentKey);
    }

    static String percentKey(DropType node) {
        if (node == null) {
            return null;
        }
        return switch (node) {
            case ITEM -> ITEM_PERCENT_KEY;
            case HULL_ITEM -> HULL_ITEM_PERCENT_KEY;
            case CRAFTING_ITEM -> CRAFTING_ITEM_PERCENT_KEY;
            case FEEDSTOCK -> FEEDSTOCK_PERCENT_KEY;
            default -> null;
        };
    }

    static float percent(CampaignFleetAPI fleet, String percentKey) {
        return fleet == null || fleet.getFleetData() == null ? 0f : percent(fleet.getFleetData().getMembersListCopy(), percentKey);
    }

    static float percent(List<FleetMemberAPI> members, String percentKey) {
        return FleetContributions.sum(members, percentKey);
    }
}
