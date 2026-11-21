package exiledsector.charts.anomaly.stormfront;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.ChartIds;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.AnomalyTerrains;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.anomaly.StarAnomalies;
import exiledsector.charts.anomaly.StellarFlares;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.items.ItemData;
import exiledsector.items.modifier.RolledModifier;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class StormFront implements ContentPlacer, RunParticipant, LootShaper {

    public static final String ANOMALY_ID = "storm_front";
    public static final String LOOT_KEY = ChartModifier.CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT.anomalyTuningKey();
    public static final String STRIKE_KEY = ChartModifier.CHART_STORM_STRIKE_FREQUENCY_PERCENT.anomalyTuningKey();
    public static final String SIZE_KEY = ChartModifier.CHART_STORM_FRONT_SIZE_PERCENT.anomalyTuningKey();
    public static final String TERRAIN_ID = "exiledSector_chart_storm_front";
    public static final String FRONT_TAG = ChartIds.PREFIX + "storm_front";
    public static final StormFront INSTANCE = new StormFront();
    static final long PATH_SALT = 0x53544f524d4652L;

    private StormFront() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        List<SectorEntityToken> marks = marks(site);
        Random random = site.random(PATH_SALT);
        StormTuning tuning = StormTuning.of(site.context());
        List<String> planned = ChartAnomalies.planned(site.context());
        float heading = random.nextFloat() * 360f;
        float bandWidth = StormRules.bandWidth(site.context().anomalyMult(SIZE_KEY));
        PlanetAPI blackHole = tuning.captured() && planned.contains(StarAnomalies.BLACK_HOLE.id()) ? site.system().getStar() : null;
        StormCourse course = blackHole == null ? new StormPath(heading, edgeRadius(marks), bandWidth, StormRules.CROSSING_DAYS)
                : capturedOrbit(blackHole, bandWidth, heading);
        boolean solar = tuning.luckyBiomeModifiers() && planned.contains(StellarFlares.ANOMALY_ID);
        List<String> markIds = marks.stream().map(SectorEntityToken::getId).toList();
        site.addTerrain(TERRAIN_ID, new StormFrontTerrainPlugin.Params(site.runId(), course, markIds, Global.getSector().getClock().getTimestamp(),
                random.nextLong(), solar, StormRules.heldSlowShare(tuning.heldSpeedReducedPercent())), course.centre(0f), FRONT_TAG);
    }

    static StormOrbit capturedOrbit(SectorEntityToken blackHole, float bandWidth, float startDegrees) {
        Vector2f hole = blackHole.getLocation() == null ? new Vector2f() : blackHole.getLocation();
        return new StormOrbit(hole.x, hole.y, blackHole.getRadius() + StormRules.CAPTURED_INNER_GAP,
                StarAnomalies.DERELICT_REACH + StormRules.CAPTURED_OUTER_MARGIN, bandWidth, startDegrees, StormRules.CAPTURED_ORBIT_DAYS);
    }

    static List<SectorEntityToken> marks(AnomalyContentSite site) {
        List<SectorEntityToken> marks = new ArrayList<>(site.worlds());
        for (SectorEntityToken poi : site.pois()) {
            if (!marks.contains(poi)) {
                marks.add(poi);
            }
        }
        return marks;
    }

    static float edgeRadius(List<SectorEntityToken> marks) {
        float farthest = 0f;
        for (SectorEntityToken mark : marks) {
            if (mark.getLocation() != null) {
                farthest = Math.max(farthest, mark.getLocation().length() + mark.getRadius());
            }
        }
        return Math.max(StormRules.MIN_EDGE_RADIUS, farthest + StormRules.EDGE_MARGIN);
    }

    @Override
    public InteractionDialogPlugin dialogFor(AnomalyRun run, SectorEntityToken target) {
        StormFrontTerrainPlugin front = StormBattles.playerFront();
        if (target instanceof CampaignFleetAPI fleet && front != null && run.runId().equals(front.runId())) {
            RedactedFront.reinforce(run, fleet);
        }
        return null;
    }

    @Override
    public void clearFrom(CampaignFleetAPI fleet) {
        StormFrontTerrainPlugin.liftFrom(fleet);
    }

    @Override
    public void preloadTextures() {
        StormFrontTerrainPlugin.preloadTextures();
    }

    @Override
    public boolean holdsPlayerBattle() {
        return StormBattles.playerInStorm();
    }

    @Override
    public float battleQuantityPercent(ChartGenContext context) {
        return StormRules.lootQuantityPercent(context);
    }

    @Override
    public ItemData shapeBattleDrop(SectorAPI sector, ItemData item, LocationAPI location) {
        return StormLoot.shapedDrop(sector, item, location);
    }

    @Override
    public Predicate<RolledModifier> battleLuckyModifiers() {
        return StormLoot.luckyModifiers();
    }

    @Override
    public String intelText(AnomalyRun run) {
        for (StormFrontTerrainPlugin front : frontsIn(run.system())) {
            if (!front.isDone()) {
                return front.intelText();
            }
        }
        return null;
    }

    public static List<StormFrontTerrainPlugin> frontsIn(LocationAPI location) {
        return AnomalyTerrains.in(location, StormFrontTerrainPlugin.class);
    }

    public static StormFrontTerrainPlugin frontHolding(CampaignFleetAPI fleet) {
        return fleet == null ? null : AnomalyTerrains.holding(fleet, frontsIn(fleet.getContainingLocation()));
    }
}
