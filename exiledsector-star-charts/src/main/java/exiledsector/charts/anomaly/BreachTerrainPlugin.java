package exiledsector.charts.anomaly;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTerrain;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Translation;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartRun;
import exiledsector.charts.ChartRunEffects;
import exiledsector.charts.ChartRuns;
import exiledsector.charts.ChartSites;
import exiledsector.ui.VanillaText;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class BreachTerrainPlugin extends BaseTerrain {

    static final float TICK_DAYS = 0.1f;
    public static final String MOD_ID = SpatialBreach.TERRAIN_ID + "_stat_mod";
    static final String SENSOR_MOD_ID = MOD_ID + "_sensor";
    static final String BURN_MOD_ID = MOD_ID + "_burn";
    static final String SUPPLY_BUFF_ID = MOD_ID + "_supplies";
    static final float MOD_DAYS = TICK_DAYS * 2.5f;
    static final float PERCENT = 100f;
    static final float RENDER_MARGIN = 500f;
    static final float TOOLTIP_PAD = 10f;
    static final float ANIMATION_WRAP_SECONDS = 3600f;
    static final float BODY_ALPHA = 0.45f;
    static final float MAP_ALPHA = 0.5f;
    static final Color BODY_COLOUR = new Color(120, 60, 200);
    private static final Logger LOG = Logger.getLogger(BreachTerrainPlugin.class);

    public record Params(String runId, int tier, BreachRules.Growth growth, List<BreachRules.CacheSite> caches, float coreItemMult, long seed) {
    }

    private String runId;
    private int tier;
    private float maxRadius;
    private float openSeconds;
    private float collapseSeconds;
    private boolean opened;
    private float secondsOpen;
    private float radius;
    private float peakRadius;
    private float coreItemMult;
    private long seed;
    private float[] cacheX;
    private float[] cacheY;
    private float[] cacheDepth;
    private boolean[] cacheFormed;
    private boolean closed;
    private transient float daysSinceTick;
    private transient float elapsedSeconds;
    private transient SpriteAPI bodySprite;

    @Override
    public void init(String terrainId, SectorEntityToken entity, Object param) {
        super.init(terrainId, entity, param);
        if (param instanceof Params params) {
            runId = params.runId();
            tier = params.tier();
            maxRadius = params.growth().maxRadius();
            openSeconds = params.growth().openSeconds();
            collapseSeconds = params.growth().collapseSeconds();
            coreItemMult = params.coreItemMult();
            seed = params.seed();
            int count = params.caches() == null ? 0 : params.caches().size();
            cacheX = new float[count];
            cacheY = new float[count];
            cacheDepth = new float[count];
            cacheFormed = new boolean[count];
            for (int i = 0; i < count; i++) {
                BreachRules.CacheSite site = params.caches().get(i);
                cacheX[i] = site.x();
                cacheY[i] = site.y();
                cacheDepth[i] = site.depth();
            }
        }
    }

    BreachRules.Growth growth() {
        return new BreachRules.Growth(maxRadius, openSeconds, collapseSeconds);
    }

    public String runId() {
        return runId;
    }

    public int tier() {
        return tier;
    }

    public float radius() {
        return radius;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isOpened() {
        return opened;
    }

    public float grownFraction() {
        return growth().grownFraction(peakRadius);
    }

    public float coreItemMult() {
        return coreItemMult;
    }

    int cachesFormed() {
        int formed = 0;
        for (boolean cache : cacheFormed == null ? new boolean[0] : cacheFormed) {
            formed += cache ? 1 : 0;
        }
        return formed;
    }

    @Override
    protected boolean shouldCheckFleetsToApplyEffect() {
        return false;
    }

    @Override
    public String getEffectCategory() {
        return SpatialBreach.TERRAIN_ID;
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || closed || entity == null || entity.getContainingLocation() == null) {
            return;
        }
        super.advance(amount);
        elapsedSeconds = (elapsedSeconds + amount) % ANIMATION_WRAP_SECONDS;
        if (!opened && !playerFleetOver()) {
            return;
        }
        daysSinceTick += Global.getSector().getClock().convertToDays(amount);
        try {
            elapse(amount);
            if (closed || daysSinceTick < TICK_DAYS) {
                return;
            }
            float days = daysSinceTick;
            daysSinceTick = 0f;
            tick(days);
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] The Spatial Breach in Star Chart run " + runId + " failed to update", e);
        }
    }

    private boolean playerFleetOver() {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        return player != null && player.getContainingLocation() == entity.getContainingLocation() && player.getLocation() != null
                && BreachRules.triggers(Misc.getDistance(entity.getLocation(), player.getLocation()), player.getRadius());
    }

    void elapse(float seconds) {
        opened = true;
        secondsOpen += Math.max(0f, seconds);
        BreachRules.Growth growth = growth();
        radius = growth.radiusAt(secondsOpen);
        peakRadius = Math.max(peakRadius, radius);
        if (growth.isCollapsed(secondsOpen)) {
            collapse();
        }
    }

    void tick(float days) {
        LocationAPI location = entity.getContainingLocation();
        formCaches(location);
        if (growth().isCollapsing(secondsOpen)) {
            dissolveCaches(location);
        }
        touchSites(location);
        for (CampaignFleetAPI fleet : location.getFleets()) {
            applyTo(fleet, days);
        }
    }

    private void formCaches(LocationAPI location) {
        if (cacheFormed == null || !(location instanceof StarSystemAPI system)) {
            return;
        }
        Vector2f centre = entity.getLocation();
        ChartGenContext context = null;
        for (int i = 0; i < cacheFormed.length; i++) {
            if (cacheFormed[i] || !BreachRules.reached(Misc.getDistance(centre, new Vector2f(cacheX[i], cacheY[i])), radius)) {
                continue;
            }
            cacheFormed[i] = true;
            if (context == null) {
                context = runContext();
            }
            formCache(system, i, context);
        }
    }

    private void formCache(StarSystemAPI system, int index, ChartGenContext context) {
        try {
            Random random = new Random(seed ^ (index * 0x9E3779B97F4A7C15L));
            SectorEntityToken cache = ChartSites.addCache(system, runId, tier, context, new Vector2f(cacheX[index], cacheY[index]), random);
            if (cache != null) {
                cache.addTag(SpatialBreach.CACHE_TAG);
                float rarity = new BreachRules.CacheSite(cacheX[index], cacheY[index], cacheDepth[index]).rarityPercent();
                cache.getMemoryWithoutUpdate().set(SpatialBreach.RARITY_KEY, rarity);
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.error("[ExiledSector] A Spatial Breach cache could not form in Star Chart run " + runId, e);
        }
    }

    private void dissolveCaches(LocationAPI location) {
        Vector2f centre = entity.getLocation();
        for (SectorEntityToken cache : new ArrayList<>(location.getEntitiesWithTag(SpatialBreach.CACHE_TAG))) {
            if (cache.getLocation() != null && !BreachRules.reached(Misc.getDistance(centre, cache.getLocation()), radius)) {
                location.removeEntity(cache);
            }
        }
    }

    private void touchSites(LocationAPI location) {
        Vector2f centre = entity.getLocation();
        for (SectorEntityToken site : location.getEntitiesWithTag(ChartIds.CONTENT_TAG)) {
            if (site instanceof CampaignFleetAPI || site.hasTag(SpatialBreach.CACHE_TAG) || site.getLocation() == null) {
                continue;
            }
            MemoryAPI memory = site.getMemoryWithoutUpdate();
            if (memory == null || memory.contains(SpatialBreach.RARITY_KEY) || Misc.getDistance(centre, site.getLocation()) > radius) {
                continue;
            }
            memory.set(SpatialBreach.RARITY_KEY, BreachRules.TOUCHED_RARITY_PERCENT);
        }
    }

    private void applyTo(CampaignFleetAPI fleet, float days) {
        if (fleet == null || fleet.isStationMode() || fleet.getStats() == null || fleet.getLocation() == null) {
            return;
        }
        float intensity = BreachRules.intensity(Misc.getDistance(entity.getLocation(), fleet.getLocation()) - fleet.getRadius(), radius);
        if (intensity <= 0f) {
            return;
        }
        MutableFleetStatsAPI stats = fleet.getStats();
        String source = getTerrainName();
        stats.addTemporaryModMult(MOD_DAYS, SENSOR_MOD_ID, source, BreachRules.sensorMult(intensity), stats.getSensorRangeMod());
        stats.addTemporaryModMult(MOD_DAYS, BURN_MOD_ID, source, BreachRules.burnMult(intensity), stats.getFleetwideMaxBurnMod());
        if (fleet.isPlayerFleet()) {
            raiseSupplyUse(fleet, intensity, source);
            drainFuel(fleet, intensity, days);
        }
    }

    private static void raiseSupplyUse(CampaignFleetAPI fleet, float intensity, String source) {
        if (fleet.getFleetData() == null) {
            return;
        }
        float percent = BreachRules.supplyUsePercent(intensity);
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.getBuffManager() != null) {
                member.getBuffManager().addBuffOnlyUpdateStat(new BreachSupplyBuff(SUPPLY_BUFF_ID, percent, source, MOD_DAYS));
            }
        }
    }

    private static void drainFuel(CampaignFleetAPI fleet, float intensity, float days) {
        if (fleet.getLogistics() == null || fleet.getCargo() == null) {
            return;
        }
        float fuel = fleet.getLogistics().getFuelCostPerLightYear() * BreachRules.FUEL_LIGHT_YEARS_PER_DAY * intensity * days;
        float held = fleet.getCargo().getFuel();
        if (fuel > 0f && held > 0f) {
            fleet.getCargo().removeFuel(Math.min(fuel, held));
        }
    }

    private ChartGenContext runContext() {
        ChartRun run = ChartRuns.currentOrNull();
        return run != null && run.runId().equals(runId) ? ChartRunEffects.contextOf(run) : ChartGenContext.none();
    }

    public float close() {
        if (closed) {
            return grownFraction();
        }
        closed = true;
        for (CampaignFleetAPI fleet : entity == null || entity.getContainingLocation() == null ? List.<CampaignFleetAPI>of()
                : entity.getContainingLocation().getFleets()) {
            liftFrom(fleet);
        }
        LocationAPI location = entity == null ? null : entity.getContainingLocation();
        if (location != null) {
            for (SectorEntityToken cache : new ArrayList<>(location.getEntitiesWithTag(SpatialBreach.CACHE_TAG))) {
                location.removeEntity(cache);
            }
            location.removeEntity(entity);
        }
        return grownFraction();
    }

    void collapse() {
        LocationAPI location = entity == null ? null : entity.getContainingLocation();
        close();
        SectorEntityToken core = location == null ? null : location.getEntityById(SpatialBreach.coreId(runId));
        if (core != null) {
            location.removeEntity(core);
        }
    }

    public static void liftFrom(CampaignFleetAPI fleet) {
        MutableFleetStatsAPI stats = fleet == null ? null : fleet.getStats();
        if (stats == null) {
            return;
        }
        for (String modId : List.of(SENSOR_MOD_ID, BURN_MOD_ID)) {
            stats.removeTemporaryMod(modId);
        }
        if (stats.getSensorRangeMod() != null) {
            stats.getSensorRangeMod().unmodify(SENSOR_MOD_ID);
        }
        if (stats.getFleetwideMaxBurnMod() != null) {
            stats.getFleetwideMaxBurnMod().unmodify(BURN_MOD_ID);
        }
        if (fleet.getFleetData() == null) {
            return;
        }
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.getBuffManager() != null && member.getBuffManager().getBuff(SUPPLY_BUFF_ID) != null) {
                member.getBuffManager().removeBuff(SUPPLY_BUFF_ID);
            }
        }
    }

    @Override
    public boolean containsPoint(Vector2f point, float pointRadius) {
        return !closed && radius > 0f && entity != null && point != null && Misc.getDistance(entity.getLocation(), point) < radius + pointRadius;
    }

    @Override
    public float getRenderRange() {
        return radius + RENDER_MARGIN;
    }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return EnumSet.of(CampaignEngineLayers.TERRAIN_1);
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (closed || entity == null || radius <= 0f) {
            return;
        }
        BreachFieldRenderer.render(entity.getLocation(), radius, viewport.getAlphaMult(), elapsedSeconds, viewport.getViewMult());
    }

    @Override
    public void renderOnMap(float factor, float alphaMult) {
        if (closed || entity == null || radius <= 0f || !loadSprites()) {
            return;
        }
        Vector2f centre = entity.getLocation();
        drawBody(centre.x * factor, centre.y * factor, radius * 2f * factor, alphaMult * MAP_ALPHA);
    }

    private void drawBody(float x, float y, float size, float alpha) {
        bodySprite.setSize(size, size);
        bodySprite.setColor(BODY_COLOUR);
        bodySprite.setAlphaMult(alpha * BODY_ALPHA);
        bodySprite.setAdditiveBlend();
        bodySprite.renderAtCenter(x, y);
    }

    private boolean loadSprites() {
        if (bodySprite != null) {
            return true;
        }
        try {
            bodySprite = Global.getSettings().getSprite("misc", "fx_radial");
        } catch (RuntimeException e) {
            bodySprite = null;
        }
        return bodySprite != null;
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    @Override
    public boolean isTooltipExpandable() {
        return false;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        I18n.forGameText(() -> addTooltipLines(tooltip));
    }

    private void addTooltipLines(TooltipMakerAPI tooltip) {
        Color text = Misc.getTextColor();
        tooltip.addTitle(getTerrainName());
        VanillaText.plainPara(tooltip, Translation.gameText("chart.breach.description"), TOOLTIP_PAD, text);
        int sensorLoss = Math.round(BreachRules.SENSOR_LOSS * PERCENT);
        int burnLoss = Math.round(BreachRules.BURN_LOSS * PERCENT);
        VanillaText.addPara(tooltip, Translation.msg("chart.breach.effect.sensor").arg("value", sensorLoss).styled(), TOOLTIP_PAD, text);
        VanillaText.addPara(tooltip, Translation.msg("chart.breach.effect.burn").arg("value", burnLoss).styled(), TOOLTIP_PAD, text);
        int supplyIncrease = Math.round(BreachRules.SUPPLY_USE_PERCENT);
        VanillaText.addPara(tooltip, Translation.msg("chart.breach.effect.supplies").arg("value", supplyIncrease).styled(), TOOLTIP_PAD, text);
        VanillaText.addPara(tooltip, Translation.msg("chart.breach.effect.fuel").styled(), TOOLTIP_PAD, text);
        VanillaText.plainPara(tooltip, Translation.gameText("chart.breach.effect.depth"), TOOLTIP_PAD, Misc.getGrayColor());
    }

    @Override
    public String getTerrainName() {
        return Translation.gameText("chart.breach.name");
    }

    @Override
    public String getNameForTooltip() {
        return getTerrainName();
    }

    @Override
    public float getMaxEffectRadius(Vector2f locFrom) {
        return radius;
    }

    @Override
    public float getOptimalEffectRadius(Vector2f locFrom) {
        return radius * 0.5f;
    }
}
