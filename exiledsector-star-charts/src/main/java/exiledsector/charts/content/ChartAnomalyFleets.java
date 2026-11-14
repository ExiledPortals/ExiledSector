package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import exiledsector.charts.effect.ChartEnemyShipEffects;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.RunFactionDress;
import exiledsector.charts.content.ChartContentPlan.FleetRole;
import exiledsector.charts.content.ChartContentPlan.PlannedFleet;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

public final class ChartAnomalyFleets {

    public record Spec(String factionId, String fleetType, FleetStrength strength, float freighterPoints, float tankerPoints) {

        public Spec atLeast(float minCombatPoints) {
            FleetStrength raised = strength.atLeast(minCombatPoints);
            return raised == strength ? this : new Spec(factionId, fleetType, raised, freighterPoints, tankerPoints);
        }

        public Spec withCargoShips(float addedFreighterPoints, float addedTankerPoints) {
            return new Spec(factionId, fleetType, strength, freighterPoints + Math.max(0f, addedFreighterPoints),
                    tankerPoints + Math.max(0f, addedTankerPoints));
        }
    }

    private ChartAnomalyFleets() {
    }

    public static ChartTier tier(int tier, ChartGenContext context) {
        return context.adjust(ChartTiers.forTier(ChartTiers.clamp(tier)));
    }

    public static Spec combat(String factionId, String fleetType, ChartTier tier, float sizePercent) {
        return combat(factionId, fleetType, tier, sizePercent, ChartContentPlanner.FLEET_SIZE_CAP);
    }

    public static Spec combat(String factionId, String fleetType, ChartTier tier, float sizePercent, float sizeCap) {
        return new Spec(factionId, fleetType, FleetStrength.regular(tier, sizePercent, sizeCap), 0f, 0f);
    }

    public static Spec bossCombat(String factionId, String fleetType, ChartTier tier, float sizePercent) {
        return new Spec(factionId, fleetType, FleetStrength.boss(tier, sizePercent), 0f, 0f);
    }

    public static Spec freighters(String factionId, String fleetType, ChartTier tier, float freighterPoints, float tankerPoints, float combatPoints) {
        float quality = ChartContentPlanner.fleetQuality(tier);
        return new Spec(factionId, fleetType, new FleetStrength(combatPoints, quality / ChartTier.PERCENT, 0, 0, 0), freighterPoints, tankerPoints);
    }

    static FleetParamsV3 params(Spec spec, Random random) {
        FleetParamsV3 params = spec.strength().toParams(spec.factionId(), spec.fleetType(), random);
        params.freighterPts = spec.freighterPoints();
        params.tankerPts = spec.tankerPoints();
        params.withOfficers = spec.strength().combatPoints() > 0f;
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

    public static boolean addCoreShip(CampaignFleetAPI fleet, String variantId, String aiCoreId, Random random) {
        return fleet != null && ChartContentGenerator.engine().addCoreShip(fleet, variantId, aiCoreId, random);
    }

    public static void prepare(CampaignFleetAPI fleet, String runId, int tier, long fleetSeed, boolean lootable, ChartGenContext context) {
        ChartContentGenerator.prepareFleet(fleet, runId, tier, FleetRole.PATROL, 0, fleetSeed);
        if (!lootable) {
            fleet.getMemoryWithoutUpdate().unset(ChartIds.START_DP_MEMORY_KEY);
            ChartEnemyShipEffects.grant(fleet, context);
            return;
        }
        arm(fleet, false, context, tier);
    }

    public static CampaignFleetAPI planned(String factionId, PlannedFleet planned, Random random, String runId, int tier, long fleetSeed) {
        CampaignFleetAPI fleet = create(ChartContentGenerator.fleetParams(factionId, planned, random));
        if (fleet != null) {
            ChartContentGenerator.prepareFleet(fleet, runId, tier, planned.role(), planned.strengthBonus(), fleetSeed);
        }
        return fleet;
    }

    public static void arm(CampaignFleetAPI fleet, boolean boss, ChartGenContext context, int tier) {
        ChartContentGenerator.armAgainstPlayer(fleet, boss, ChartGenContext.orNone(context), ChartTiers.clamp(tier));
    }

    public static void revealBoss(CampaignFleetAPI boss) {
        ChartContentGenerator.revealBoss(boss);
    }

    public static void unmarkBossStation(CampaignFleetAPI station) {
        ChartContentGenerator.unmarkBossStation(station);
    }

    public static String dress(String runFactionId, RunFactionDress dress) {
        return ChartContentGenerator.engine().dress(runFactionId, dress);
    }

    public static List<ChartSpot> openSpots(StarSystemAPI system, Vector2f entry, Random random) {
        return ChartContentGenerator.clearSpots(ChartContentGenerator.engine().spots(system, random, null, ChartContentGenerator.POI_GAP),
                ChartContentGenerator.hazards(system), entry, ChartContentGenerator.FLEET_WORMHOLE_CLEARANCE);
    }

    public static Vector2f farSideOf(Vector2f entry) {
        return ChartContentGenerator.farSideOf(entry);
    }

    public static void tag(SectorEntityToken entity, String runId) {
        ChartContentGenerator.tagContent(entity, runId);
    }
}
