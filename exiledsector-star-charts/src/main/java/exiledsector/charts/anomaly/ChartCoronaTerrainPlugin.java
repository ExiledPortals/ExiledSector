package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;

public class ChartCoronaTerrainPlugin extends StarCoronaTerrainPlugin {

    public static final String TERRAIN_ID = "exiledSector_chart_corona";

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        if (entity instanceof CampaignFleetAPI fleet && fleet.isPlayerFleet()) {
            super.applyEffect(entity, days);
        }
    }
}
