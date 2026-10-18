package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;

import java.util.List;
import java.util.Random;
import java.util.Set;

interface ChartContentEngine {

    List<ChartSpot> spots(StarSystemAPI system, Random random, Set<SectorEntityToken> excludedFoci, float gap);

    SectorEntityToken addSalvageEntity(StarSystemAPI system, String entityType, String salvageSpec, Random random);

    SectorEntityToken addDerelict(StarSystemAPI system, String factionId, String derelictType, boolean recoverable, Random random);

    void place(SectorEntityToken entity, ChartSpot spot);

    CampaignFleetAPI createFleet(FleetParamsV3 params);
}
