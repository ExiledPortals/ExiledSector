package exiledsector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

public final class PlayerFleet {

    private PlayerFleet() {
    }

    public static CampaignFleetAPI orNull() {
        SectorAPI sector = Global.getSector();
        return sector == null ? null : sector.getPlayerFleet();
    }

    public static CargoAPI cargoOrNull() {
        CampaignFleetAPI playerFleet = orNull();
        return playerFleet == null ? null : playerFleet.getCargo();
    }
}
