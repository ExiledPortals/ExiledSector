package exiledsector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

public final class PlayerFleet {

    private PlayerFleet() {
    }

    public static CampaignFleetAPI orNull() {
        SectorAPI sector = Global.getSector();
        return sector == null ? null : sector.getPlayerFleet();
    }

    public static boolean isIn(LocationAPI location) {
        CampaignFleetAPI playerFleet = orNull();
        return location != null && playerFleet != null && playerFleet.getContainingLocation() == location;
    }

    public static CargoAPI cargoOrNull() {
        CampaignFleetAPI playerFleet = orNull();
        return playerFleet == null ? null : playerFleet.getCargo();
    }
}
