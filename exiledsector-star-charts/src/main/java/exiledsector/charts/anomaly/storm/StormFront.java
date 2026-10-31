package exiledsector.charts.anomaly.storm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.ChartIds;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.AnomalyTerrains;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.items.ItemDrops;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class StormFront implements ContentPlacer, RunParticipant, LootShaper {

    public static final String ANOMALY_ID = "storm_front";
    public static final String LOOT_KEY = ChartEffect.CHART_STORM_FRONT_LOOT_QUANTITY_PERCENT.anomalyTuningKey();
    public static final String STRIKE_KEY = ChartEffect.CHART_STORM_STRIKE_FREQUENCY_PERCENT.anomalyTuningKey();
    public static final String SIZE_KEY = ChartEffect.CHART_STORM_FRONT_SIZE_PERCENT.anomalyTuningKey();
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
        StormPath path = new StormPath(random.nextFloat() * 360f, edgeRadius(marks),
                StormRules.bandWidth(site.context().anomalyMult(SIZE_KEY)), StormRules.CROSSING_DAYS);
        List<String> markIds = marks.stream().map(SectorEntityToken::getId).toList();
        site.addTerrain(TERRAIN_ID, new StormFrontTerrainPlugin.Params(site.runId(), path, markIds, Global.getSector().getClock().getTimestamp(),
                random.nextLong()), path.centre(0f), FRONT_TAG);
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
    public void clearFrom(CampaignFleetAPI fleet) {
        StormFrontTerrainPlugin.liftFrom(fleet);
    }

    @Override
    public void preloadTextures() {
        StormFrontTerrainPlugin.preloadTextures();
    }

    @Override
    public void loadItems() {
        StormLoot.reserveModifiers();
    }

    @Override
    public boolean holdsPlayerBattle() {
        return StormBattles.playerInStorm();
    }

    @Override
    public float battleLootMult(ChartGenContext context) {
        return StormRules.lootQuantityMult(context);
    }

    @Override
    public ItemDrops.SiteLoot touchBattleLoot(ItemDrops.SiteLoot loot, Random random) {
        return StormLoot.touch(loot, random);
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
        if (fleet == null) {
            return null;
        }
        for (StormFrontTerrainPlugin front : frontsIn(fleet.getContainingLocation())) {
            if (front.containsEntity(fleet)) {
                return front;
            }
        }
        return null;
    }
}
