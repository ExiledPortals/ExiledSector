package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import exiledlib.PlayerFleet;

public final class PlayerFleetStats {

    private PlayerFleetStats() {
    }

    public static void markForStatUpdate() {
        markForStatUpdate(PlayerFleet.orNull());
    }

    public static void markForStatUpdate(CampaignFleetAPI playerFleet) {
        FleetDataAPI fleetData = playerFleet == null ? null : playerFleet.getFleetData();
        if (fleetData == null) return;

        fleetData.getMembersListCopy().forEach(member -> member.setStatUpdateNeeded(true));
        fleetData.setSyncNeeded();
    }
}
