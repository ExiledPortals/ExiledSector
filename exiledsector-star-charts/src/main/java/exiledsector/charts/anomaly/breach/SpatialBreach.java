package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import exiledsector.i18n.Translation;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.AnomalyTerrains;
import exiledsector.charts.anomaly.ContentPlacer;
import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.RunParticipant;
import exiledsector.items.ItemChances;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

public final class SpatialBreach implements ContentPlacer, RunParticipant, LootShaper {

    public static final String ANOMALY_ID = "spatial_breach";
    public static final String GROWTH_KEY = ChartEffect.CHART_BREACH_GROWTH_PERCENT.anomalyTuningKey();
    public static final String CACHE_KEY = ChartEffect.CHART_BREACH_CACHE_PERCENT.anomalyTuningKey();
    public static final String CACHE_ITEM_KEY = ChartEffect.CHART_BREACH_CACHE_ITEM_PERCENT.anomalyTuningKey();
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
    static final float POI_OFFSET_SHARE = 0.5f;
    static final float ENTRY_MARGIN = 500f;

    private SpatialBreach() {
    }

    @Override
    public String id() {
        return ANOMALY_ID;
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
        site.addTerrain(TERRAIN_ID, params, centre);
        SectorEntityToken core = site.track(system.addCustomEntity(coreId(site.runId()), Translation.gameText("chart.breach.core.name"), CORE_TYPE,
                Factions.NEUTRAL));
        core.setFixedLocation(centre.x, centre.y);
        core.addTag(CORE_TAG);
        core.addTag(Tags.NON_CLICKABLE);
    }

    static Vector2f pickCentre(AnomalyContentSite site, float maxRadius, Random random) {
        return site.findSpot((poi, spotRandom) -> spotRandom.nextFloat() * maxRadius * POI_OFFSET_SHARE,
                candidate -> site.distanceToEntry(candidate) > maxRadius + ENTRY_MARGIN, random);
    }

    @Override
    public String intelText(AnomalyRun run) {
        return breachIn(run.system()) != null ? intelLine() : null;
    }

    @Override
    public void clearFrom(CampaignFleetAPI fleet) {
        BreachTerrainPlugin.liftFrom(fleet);
    }

    @Override
    public ItemChances siteChances(MemoryAPI siteMemory, ItemChances chances) {
        ItemChances scaled = siteMemory.contains(ITEM_MULT_KEY) ? chances.scaled(siteMemory.getFloat(ITEM_MULT_KEY)) : chances;
        return siteMemory.contains(RARITY_KEY) ? scaled.withRarity(scaled.rarity() * (1f + siteMemory.getFloat(RARITY_KEY) / ChartGenContext.PERCENT)) : scaled;
    }

    public static String coreId(String runId) {
        return ChartIds.PREFIX + "breach_core_" + runId;
    }

    public static BreachTerrainPlugin breachIn(LocationAPI location) {
        return AnomalyTerrains.in(location, BreachTerrainPlugin.class).stream().filter(breach -> !breach.isClosed()).findFirst().orElse(null);
    }
}
