package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.terrain.PulsarBeamTerrainPlugin;

public class ChartPulsarBeamTerrainPlugin extends PulsarBeamTerrainPlugin {

    public static final String TERRAIN_ID = "exiledSector_chart_pulsar_beam";

    @Override
    public void applyEffect(SectorEntityToken entity, float days) {
        if (entity instanceof CampaignFleetAPI fleet && fleet.isPlayerFleet()) {
            super.applyEffect(entity, days);
        }
    }
}
