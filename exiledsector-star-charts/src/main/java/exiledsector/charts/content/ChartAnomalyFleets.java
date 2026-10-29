package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.ChartVanillaLoot;

import java.util.Random;

public final class ChartAnomalyFleets {

    public record Spec(String factionId, String fleetType, float combatPoints, float freighterPoints, float tankerPoints, float quality,
                       int officerLevelBonus, int officerCountBonus, int averageSMods) {
    }

    private ChartAnomalyFleets() {
    }

    public static ChartTier tier(int tier, ChartGenContext context) {
        ChartTier base = ChartTiers.forTier(ChartTiers.clamp(tier));
        return context == null ? base : context.adjust(base);
    }

    public static Spec combat(String factionId, String fleetType, ChartTier tier, float sizePercent) {
        float quality = ChartContentPlanner.fleetQuality(tier);
        return new Spec(factionId, fleetType, ChartTier.fleetPoints(Math.min(ChartContentPlanner.FLEET_SIZE_CAP, sizePercent)), 0f, 0f,
                quality / ChartTier.PERCENT, tier.officerLevelBonus(), tier.officerCountBonus(), ChartContentPlanner.averageSMods(quality));
    }

    public static Spec freighters(String factionId, String fleetType, ChartTier tier, float freighterPoints, float tankerPoints, float combatPoints) {
        float quality = ChartContentPlanner.fleetQuality(tier);
        return new Spec(factionId, fleetType, combatPoints, freighterPoints, tankerPoints, quality / ChartTier.PERCENT, 0, 0, 0);
    }

    static FleetParamsV3 params(Spec spec, Random random) {
        FleetParamsV3 params = new FleetParamsV3(null, spec.factionId(), spec.quality(), spec.fleetType(), spec.combatPoints(),
                spec.freighterPoints(), spec.tankerPoints(), 0f, 0f, 0f, 0f);
        params.random = random;
        params.ignoreMarketFleetSizeMult = true;
        params.withOfficers = spec.combatPoints() > 0f;
        params.officerLevelBonus = spec.officerLevelBonus();
        params.officerNumberBonus = spec.officerCountBonus();
        params.averageSMods = spec.averageSMods() > 0 ? spec.averageSMods() : null;
        return params;
    }

    public static CampaignFleetAPI create(Spec spec, Random random) {
        return create(params(spec, random));
    }

    public static CampaignFleetAPI create(Spec spec, Random random, int maxShipSize, boolean withOfficers) {
        FleetParamsV3 params = params(spec, random);
        params.maxShipSize = maxShipSize;
        params.withOfficers = params.withOfficers && withOfficers;
        return create(params);
    }

    private static CampaignFleetAPI create(FleetParamsV3 params) {
        CampaignFleetAPI fleet = ChartContentGenerator.engine().createFleet(params);
        return fleet == null || fleet.isEmpty() ? null : fleet;
    }

    public static CampaignFleetAPI station(String factionId, String variantId, ChartTier tier, Random random) {
        CampaignFleetAPI station = ChartContentGenerator.engine().createStation(factionId, variantId,
                ChartContentPlanner.STATION_OFFICER_LEVEL + tier.officerLevelBonus(), random);
        return station == null || station.isEmpty() ? null : station;
    }

    public static CampaignFleetAPI emptyFleet(String factionId, String fleetType) {
        return ChartContentGenerator.engine().createEmptyFleet(factionId, fleetType);
    }

    public static boolean addCoreFlagship(CampaignFleetAPI fleet, String variantId, String aiCoreId, Random random) {
        return fleet != null && ChartContentGenerator.engine().addCoreFlagship(fleet, variantId, aiCoreId, random);
    }

    public static void prepare(CampaignFleetAPI fleet, String runId, int tier, long fleetSeed, boolean lootable, ChartGenContext context) {
        ChartContentGenerator.prepareFleet(fleet, runId, tier, FleetRole.PATROL, 0, fleetSeed);
        if (!lootable) {
            fleet.getMemoryWithoutUpdate().unset(ChartIds.START_DP_MEMORY_KEY);
            return;
        }
        ChartGenContext liveContext = context == null ? ChartGenContext.none() : context;
        ChartContentGenerator.applyEnemySensorRange(fleet, liveContext.enemySensorRangeBonus());
        ChartVanillaLoot.addToFleet(fleet, false, liveContext, ChartTiers.clamp(tier));
    }

    public static String dressConvoy(String factionId) {
        return ChartContentGenerator.engine().dressConvoy(factionId);
    }

    public static void tag(SectorEntityToken entity, String runId) {
        ChartContentGenerator.tagContent(entity, runId);
    }
}
