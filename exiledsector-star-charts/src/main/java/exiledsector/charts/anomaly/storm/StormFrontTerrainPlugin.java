package exiledsector.charts.anomaly.storm;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.ChartAnomalyTerrain;
import exiledsector.charts.anomaly.SlowsFleets;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.VanillaText;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class StormFrontTerrainPlugin extends ChartAnomalyTerrain implements SlowsFleets {

    public static final String CLOUD_TEXTURE = "graphics/fx/cleaner_clouds00.png";
    public static final String FLASH_TEXTURE = "graphics/fx/starburst_glow1.png";
    static final int SHEET_COLUMNS = 2;
    static final int SHEET_ROWS = 2;
    static final float PUFF_SPACING = 700f;
    static final int MIN_PUFFS = 24;
    static final int MAX_PUFFS = 320;
    static final float MIN_PUFF_SIZE = 700f;
    static final float MAX_PUFF_SIZE = 1200f;
    static final float MAX_SPIN_DEGREES_PER_SECOND = 4f;
    static final float WOBBLE_DISTANCE = 120f;
    static final float WOBBLE_PER_SECOND = 0.3f;
    static final float END_FADE_SHARE = 0.15f;
    static final float CLOUD_ALPHA = 0.3f;
    static final float MAP_ALPHA = 0.6f;
    static final float FLASHES_PER_SECOND = 2f;
    static final float FLASH_SECONDS = 0.35f;
    static final float FLASH_SIZE_MULT = 1.3f;
    static final float RENDER_MARGIN = 1500f;
    static final float SENSOR_MOD_DAYS = 0.1f;
    static final String SENSOR_MOD_ID = StormFront.TERRAIN_ID + "_stat_mod";
    static final String IN_FRONT_MEMORY_KEY = "$terrain_" + StormFront.TERRAIN_ID;
    static final int INTEL_MARKS = 3;
    static final Color CLOUD_COLOUR = new Color(90, 110, 255);
    static final Color FLASH_COLOUR = new Color(170, 200, 255);
    private static final SpriteCache SPRITES = new SpriteCache(StormFrontTerrainPlugin.class);
    private static final EnumSet<CampaignEngineLayers> LAYERS = EnumSet.of(CampaignEngineLayers.TERRAIN_6);

    public record Params(String runId, StormPath path, List<String> markIds, long startTimestamp, long seed) {
    }

    record Puff(float along, float across, float size, int cell, float startFacing, float spinDegreesPerSecond, float wobblePhase) {
    }

    private String runId;
    private float angleDegrees;
    private float edgeRadius;
    private float bandWidth;
    private float crossingDays;
    private ArrayList<String> markIds;
    private long startTimestamp;
    private long seed;
    private transient StormPath path;
    private transient List<Puff> puffs;
    private transient float[] flashes;
    private transient float animationSeconds;
    private transient Random flashRandom;
    private transient SpriteAPI cloudSheet;
    private transient SpriteAPI flashSprite;
    private transient Vector2f scratchPosition;

    @Override
    public void init(String terrainId, SectorEntityToken entity, Object param) {
        super.init(terrainId, entity, param);
        if (param instanceof Params params) {
            runId = params.runId();
            angleDegrees = params.path().angleDegrees();
            edgeRadius = params.path().edgeRadius();
            bandWidth = params.path().bandWidth();
            crossingDays = params.path().crossingDays();
            markIds = new ArrayList<>(params.markIds());
            startTimestamp = params.startTimestamp();
            seed = params.seed();
        }
    }

    public static void preloadTextures() {
        List.of(CLOUD_TEXTURE, FLASH_TEXTURE, StormOverlay.HIT_GLOW_TEXTURE).forEach(SPRITES::ensureLoaded);
    }

    public String runId() {
        return runId;
    }

    StormPath path() {
        if (path == null) {
            path = new StormPath(angleDegrees, edgeRadius, bandWidth, crossingDays);
        }
        return path;
    }

    float elapsedDays() {
        return Global.getSector().getClock().getElapsedDaysSince(startTimestamp);
    }

    public boolean isDone() {
        return path().isDone(elapsedDays());
    }

    @Override
    protected String nameKey() {
        return "chart.storm.name";
    }

    @Override
    protected boolean shouldCheckFleetsToApplyEffect() {
        return true;
    }

    @Override
    protected boolean isGone() {
        return isDone();
    }

    @Override
    protected void advanceLive(float amount) {
        Vector2f centre = path().centre(elapsedDays());
        entity.setFixedLocation(centre.x, centre.y);
        if (entity.isInCurrentLocation()) {
            animate(amount);
        }
    }

    @Override
    public void applyEffect(SectorEntityToken target, float days) {
        if (target instanceof CampaignFleetAPI fleet && fleet.getStats() != null) {
            fleet.getStats().addTemporaryModMult(SENSOR_MOD_DAYS, SENSOR_MOD_ID, getTerrainName(), StormRules.SENSOR_RANGE_MULT,
                    fleet.getStats().getSensorRangeMod());
        }
    }

    static void liftFrom(CampaignFleetAPI fleet) {
        if (fleet == null) {
            return;
        }
        MutableFleetStatsAPI stats = fleet.getStats();
        if (stats != null) {
            stats.removeTemporaryMod(SENSOR_MOD_ID);
            if (stats.getSensorRangeMod() != null) {
                stats.getSensorRangeMod().unmodify(SENSOR_MOD_ID);
            }
        }
        MemoryAPI fleetMemory = fleet.getMemoryWithoutUpdate();
        if (fleetMemory != null) {
            fleetMemory.unset(IN_FRONT_MEMORY_KEY);
        }
    }

    @Override
    public boolean containsPoint(Vector2f point, float pointRadius) {
        return entity != null && path().contains(point, pointRadius, elapsedDays());
    }

    @Override
    public float getRenderRange() {
        return path().halfDiagonal() + RENDER_MARGIN;
    }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return LAYERS;
    }

    static List<Puff> puffs(StormPath path, long seed) {
        Random random = new Random(seed);
        float area = path.bandWidth() * path.bandLength();
        int count = Math.max(MIN_PUFFS, Math.min(MAX_PUFFS, Math.round(area / (PUFF_SPACING * PUFF_SPACING))));
        List<Puff> made = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            float along = (random.nextFloat() - 0.5f) * path.bandWidth();
            float across = (random.nextFloat() - 0.5f) * path.bandLength();
            float size = MIN_PUFF_SIZE + random.nextFloat() * (MAX_PUFF_SIZE - MIN_PUFF_SIZE);
            float spin = (random.nextFloat() * 2f - 1f) * MAX_SPIN_DEGREES_PER_SECOND;
            made.add(new Puff(along, across, size, random.nextInt(SHEET_COLUMNS * SHEET_ROWS), random.nextFloat() * 360f, spin,
                    random.nextFloat() * (float) Math.PI * 2f));
        }
        return made;
    }

    static float endFade(float across, float bandLength) {
        float fadeLength = bandLength * END_FADE_SHARE;
        if (!(fadeLength > 0f)) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, (bandLength / 2f - Math.abs(across)) / fadeLength));
    }

    private boolean ensurePuffs() {
        if (puffs != null) {
            return true;
        }
        if (!(bandWidth > 0f) || !(edgeRadius > 0f)) {
            return false;
        }
        puffs = puffs(path(), seed);
        flashes = new float[puffs.size()];
        flashRandom = new Random(seed);
        return true;
    }

    private void animate(float amount) {
        if (!ensurePuffs()) {
            return;
        }
        animationSeconds += amount;
        for (int index = 0; index < flashes.length; index++) {
            flashes[index] = Math.max(0f, flashes[index] - amount / FLASH_SECONDS);
        }
        if (flashRandom.nextFloat() < amount * FLASHES_PER_SECOND) {
            flashes[flashRandom.nextInt(flashes.length)] = 1f;
        }
    }

    private boolean ensureSprites() {
        if (cloudSheet != null) {
            return true;
        }
        if (!ensurePuffs()) {
            return false;
        }
        flashSprite = SPRITES.sprite(FLASH_TEXTURE);
        cloudSheet = flashSprite == null ? null : SPRITES.sprite(CLOUD_TEXTURE);
        return cloudSheet != null;
    }

    private Vector2f puffPosition(Puff puff, Vector2f centre) {
        if (scratchPosition == null) {
            scratchPosition = new Vector2f();
        }
        StormPath stormPath = path();
        float wobble = (float) Math.sin(puff.wobblePhase() + animationSeconds * WOBBLE_PER_SECOND) * WOBBLE_DISTANCE;
        float along = puff.along() + wobble;
        scratchPosition.set(centre.x + stormPath.directionX() * along - stormPath.directionY() * puff.across(),
                centre.y + stormPath.directionY() * along + stormPath.directionX() * puff.across());
        return scratchPosition;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        float alpha = viewport.getAlphaMult();
        if (entity == null || alpha <= 0f || isDone() || !ensureSprites()) {
            return;
        }
        Vector2f centre = entity.getLocation();
        float bandLength = path().bandLength();
        for (int index = 0; index < puffs.size(); index++) {
            Puff puff = puffs.get(index);
            Vector2f position = puffPosition(puff, centre);
            if (!viewport.isNearViewport(position, puff.size())) {
                continue;
            }
            float fade = endFade(puff.across(), bandLength);
            SpriteDraw.drawCellAtCenter(cloudSheet, SHEET_COLUMNS, SHEET_ROWS, puff.cell(), position.x, position.y, puff.size(), puff.size(),
                    puff.startFacing() + puff.spinDegreesPerSecond() * animationSeconds, CLOUD_COLOUR, alpha * CLOUD_ALPHA * fade, true);
            float flash = flashes[index];
            if (flash > 0f) {
                float flashSize = puff.size() * FLASH_SIZE_MULT;
                flashSprite.setSize(flashSize, flashSize);
                flashSprite.setColor(FLASH_COLOUR);
                flashSprite.setAdditiveBlend();
                flashSprite.setAlphaMult(alpha * flash * fade);
                flashSprite.renderAtCenter(position.x, position.y);
            }
        }
    }

    @Override
    public void renderOnMap(float factor, float alphaMult) {
        if (entity == null || alphaMult <= 0f || isDone() || !ensureSprites()) {
            return;
        }
        Vector2f centre = entity.getLocation();
        float bandLength = path().bandLength();
        for (Puff puff : puffs) {
            Vector2f position = puffPosition(puff, centre);
            SpriteDraw.drawCellAtCenter(cloudSheet, SHEET_COLUMNS, SHEET_ROWS, puff.cell(), position.x * factor, position.y * factor,
                    puff.size() * factor, puff.size() * factor, puff.startFacing(), CLOUD_COLOUR, alphaMult * MAP_ALPHA * endFade(puff.across(), bandLength),
                    true);
        }
    }

    String intelText() {
        LocationAPI location = entity == null ? null : entity.getContainingLocation();
        float elapsedDays = elapsedDays();
        int daysLeft = days(path().daysLeft(elapsedDays));
        List<StyledText> ahead = marksAhead(location, elapsedDays);
        return I18n.forGameText(() -> {
            String crossing = Translation.msg("chart.storm.intel.crossing").count(daysLeft).text();
            if (ahead.isEmpty()) {
                return crossing;
            }
            return crossing + " " + Translation.msg("chart.storm.intel.reaches").arg("places", Translation.list(ahead)).text();
        });
    }

    private List<StyledText> marksAhead(LocationAPI location, float elapsedDays) {
        record Ahead(String name, float days) {
        }
        List<Ahead> ahead = new ArrayList<>();
        for (String markId : markIds == null ? List.<String>of() : markIds) {
            SectorEntityToken mark = location == null ? null : location.getEntityById(markId);
            float days = mark == null ? -1f : path().daysUntilReaches(mark.getLocation(), elapsedDays);
            if (days > 0f && mark.getName() != null) {
                ahead.add(new Ahead(mark.getName(), days));
            }
        }
        ahead.sort(Comparator.comparingDouble(Ahead::days));
        List<StyledText> places = new ArrayList<>();
        for (Ahead mark : ahead.subList(0, Math.min(INTEL_MARKS, ahead.size()))) {
            int days = days(mark.days());
            places.add(I18n.forGameText(() -> Translation.msg("chart.storm.intel.place")
                    .arg("name", StyledText.styled(mark.name(), Style.HIGHLIGHT)).count(days).styled()));
        }
        return places;
    }

    static int days(float days) {
        return Math.max(1, Math.round(days));
    }

    @Override
    protected void tooltipLines(TooltipMakerAPI tooltip) {
        Color text = Misc.getTextColor();
        VanillaText.plainPara(tooltip, Translation.gameText("chart.storm.description"), TOOLTIP_PAD, text);
        int sensorLess = Math.round((1f - StormRules.SENSOR_RANGE_MULT) * 100f);
        VanillaText.addPara(tooltip, Translation.msg("chart.storm.effect.sensors").arg("value", sensorLess).styled(), TOOLTIP_PAD, text);
        VanillaText.plainPara(tooltip, Translation.gameText("chart.storm.effect.strikes"), TOOLTIP_PAD, text);
        float lootMult = StormRules.lootQuantityMult(StormBattles.contextOf(StormBattles.runOf(this)));
        int itemIncrease = Math.round((lootMult - 1f) * 100f);
        VanillaText.addPara(tooltip, Translation.msg("chart.storm.effect.items").arg("value", itemIncrease).styled(), TOOLTIP_PAD, text);
    }
}
