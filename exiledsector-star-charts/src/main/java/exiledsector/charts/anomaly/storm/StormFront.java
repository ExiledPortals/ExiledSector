package exiledsector.charts.anomaly.storm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.ItemDrops;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class StormFront implements ChartAnomaly {

    public static final String ID = "storm_front";
    public static final String LOOT_KEY = ID + ".loot";
    public static final String STRIKE_KEY = ID + ".strikes";
    public static final String SIZE_KEY = ID + ".size";
    public static final String TERRAIN_ID = "exiledSector_chart_storm_front";
    public static final String FRONT_TAG = ChartIds.PREFIX + "storm_front";
    public static final StormFront INSTANCE = new StormFront();
    static final long PATH_SALT = 0x53544f524d4652L;

    private StormFront() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        StarSystemAPI system = site.system();
        List<SectorEntityToken> marks = marks(system, site.pois());
        Random random = site.random(PATH_SALT);
        StormPath path = new StormPath(random.nextFloat() * 360f, edgeRadius(marks),
                StormRules.bandWidth(site.context().anomalyMult(SIZE_KEY)), StormRules.CROSSING_DAYS);
        List<String> markIds = marks.stream().map(SectorEntityToken::getId).toList();
        SectorEntityToken terrain = system.addTerrain(TERRAIN_ID, new StormFrontTerrainPlugin.Params(site.runId(), path, markIds,
                Global.getSector().getClock().getTimestamp(), random.nextLong()));
        site.track(terrain);
        Vector2f start = path.centre(0f);
        terrain.setFixedLocation(start.x, start.y);
        terrain.addTag(ChartIds.ENTITY_TAG);
        terrain.addTag(ChartIds.runTag(site.runId()));
        terrain.addTag(FRONT_TAG);
    }

    static List<SectorEntityToken> marks(StarSystemAPI system, List<SectorEntityToken> pois) {
        List<SectorEntityToken> marks = new ArrayList<>();
        for (PlanetAPI planet : system.getPlanets()) {
            if (!planet.isStar()) {
                marks.add(planet);
            }
        }
        for (SectorEntityToken poi : pois) {
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
    public boolean isActiveIn(LocationAPI location) {
        for (StormFrontTerrainPlugin front : frontsIn(location)) {
            if (!front.isDone()) {
                return true;
            }
        }
        return false;
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
    public String intelText(ChartRun run, LocationAPI location) {
        for (StormFrontTerrainPlugin front : frontsIn(location)) {
            if (!front.isDone()) {
                return front.intelText();
            }
        }
        return null;
    }

    public static List<StormFrontTerrainPlugin> frontsIn(LocationAPI location) {
        List<StormFrontTerrainPlugin> fronts = new ArrayList<>();
        if (location == null) {
            return fronts;
        }
        for (CampaignTerrainAPI terrain : location.getTerrainCopy()) {
            if (terrain.getPlugin() instanceof StormFrontTerrainPlugin front) {
                fronts.add(front);
            }
        }
        return fronts;
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
