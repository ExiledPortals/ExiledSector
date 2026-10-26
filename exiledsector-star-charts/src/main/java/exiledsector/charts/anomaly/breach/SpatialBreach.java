package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledlib.i18n.Translation;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomaly;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

public final class SpatialBreach implements ChartAnomaly {

    public static final String ID = "spatial_breach";
    public static final String GROWTH_KEY = ID + ".growth";
    public static final String CACHE_KEY = ID + ".caches";
    public static final String CACHE_ITEM_KEY = ID + ".cacheItems";
    public static final String TERRAIN_ID = "exiledSector_chart_breach";
    public static final String CORE_TYPE = "exiledSector_chart_breach_core";
    public static final String CORE_TAG = ChartIds.PREFIX + "breach_core";
    public static final String CACHE_TAG = ChartIds.PREFIX + "breach_cache";
    public static final String RARITY_KEY = "$exiledSector_chartBreachRarity";
    public static final String ITEM_MULT_KEY = "$exiledSector_chartBreachItems";
    public static final SpatialBreach INSTANCE = new SpatialBreach();
    static final long CENTRE_SALT = 0x425243454e5452L;
    static final long CACHE_SALT = 0x42524341434845L;
    static final long SPAWN_SALT = 0x42525350574e53L;
    static final int CENTRE_ATTEMPTS = 24;
    static final float POI_OFFSET_SHARE = 0.5f;
    static final float MIN_OPEN_DISTANCE = 2000f;
    static final float MAX_OPEN_DISTANCE = 8000f;
    static final float ENTRY_MARGIN = 500f;

    private SpatialBreach() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void generate(AnomalyContentSite site) {
        StarSystemAPI system = site.system();
        ChartGenContext context = site.context();
        BreachRules.Growth growth = BreachRules.growth(site.tier(), context.anomalyMult(GROWTH_KEY));
        Vector2f centre = pickCentre(site, growth.maxRadius(), site.random(CENTRE_SALT));
        if (centre == null) {
            throw new IllegalStateException("no clear spot for the breach");
        }
        int cacheCount = BreachRules.cacheCount(site.tier(), context.anomalyMult(CACHE_KEY));
        List<BreachRules.CacheSite> caches = BreachRules.cacheSites(centre, growth.maxRadius(), cacheCount, site::isClear, site.random(CACHE_SALT));
        BreachTerrainPlugin.Params params = new BreachTerrainPlugin.Params(site.runId(), site.tier(), growth, caches,
                context.anomalyMult(CACHE_ITEM_KEY), site.random(SPAWN_SALT).nextLong());
        SectorEntityToken terrain = system.addTerrain(TERRAIN_ID, params);
        try {
            terrain.setFixedLocation(centre.x, centre.y);
            SectorEntityToken core = system.addCustomEntity(coreId(site.runId()), Translation.gameText("chart.breach.core.name"), CORE_TYPE,
                    Factions.NEUTRAL);
            core.setFixedLocation(centre.x, centre.y);
            core.addTag(CORE_TAG);
            core.addTag(Tags.NON_CLICKABLE);
        } catch (RuntimeException | LinkageError e) {
            system.removeEntity(terrain);
            throw e;
        }
    }

    static Vector2f pickCentre(AnomalyContentSite site, float maxRadius, Random random) {
        List<SectorEntityToken> pois = site.pois();
        for (int attempt = 0; attempt < CENTRE_ATTEMPTS; attempt++) {
            Vector2f candidate = attempt < CENTRE_ATTEMPTS / 2 && !pois.isEmpty()
                    ? nearPoi(pois.get(random.nextInt(pois.size())), maxRadius * POI_OFFSET_SHARE, random)
                    : inOpenSpace(random);
            if (candidate != null && site.isClear(candidate) && site.distanceToEntry(candidate) > maxRadius + ENTRY_MARGIN) {
                return candidate;
            }
        }
        return null;
    }

    private static Vector2f nearPoi(SectorEntityToken poi, float maxOffset, Random random) {
        if (poi.getLocation() == null) {
            return null;
        }
        return offset(poi.getLocation(), random.nextFloat() * maxOffset, random);
    }

    private static Vector2f inOpenSpace(Random random) {
        return offset(new Vector2f(), MIN_OPEN_DISTANCE + random.nextFloat() * (MAX_OPEN_DISTANCE - MIN_OPEN_DISTANCE), random);
    }

    private static Vector2f offset(Vector2f from, float distance, Random random) {
        double angle = random.nextDouble() * Math.PI * 2d;
        return new Vector2f(from.x + (float) Math.cos(angle) * distance, from.y + (float) Math.sin(angle) * distance);
    }

    @Override
    public boolean isActiveIn(LocationAPI location) {
        return breachIn(location) != null;
    }

    public static String coreId(String runId) {
        return ChartIds.PREFIX + "breach_core_" + runId;
    }

    public static BreachTerrainPlugin breachIn(LocationAPI location) {
        if (location == null) {
            return null;
        }
        for (CampaignTerrainAPI terrain : location.getTerrainCopy()) {
            if (terrain.getPlugin() instanceof BreachTerrainPlugin breach && !breach.isClosed()) {
                return breach;
            }
        }
        return null;
    }
}
