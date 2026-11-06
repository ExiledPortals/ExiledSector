package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;

import java.util.function.Function;

final class ChartFleetUpscaler {

    static final int UPSCALE_HULL_COUNT = 50;
    static final int LARGEST_DOCTRINE_SHIP_SIZE = 5;

    private ChartFleetUpscaler() {
    }

    static CampaignFleetAPI build(FleetParamsV3 params, FactionDoctrineAPI doctrine, Function<FleetParamsV3, CampaignFleetAPI> factory) {
        CampaignFleetAPI fleet = factory.apply(params);
        if (doctrine == null) {
            return fleet;
        }
        int shipSize = doctrine.getShipSize();
        while (tooManyHulls(fleet) && shipSize < LARGEST_DOCTRINE_SHIP_SIZE) {
            shipSize++;
            FactionDoctrineAPI larger = doctrine.clone();
            larger.setShipSize(shipSize);
            params.doctrineOverride = larger;
            CampaignFleetAPI rebuilt = factory.apply(params);
            if (rebuilt == null) {
                return fleet;
            }
            fleet = rebuilt;
        }
        return fleet;
    }

    static boolean tooManyHulls(CampaignFleetAPI fleet) {
        return fleet != null && fleet.getFleetData() != null && fleet.getFleetData().getNumMembers() >= UPSCALE_HULL_COUNT;
    }
}
