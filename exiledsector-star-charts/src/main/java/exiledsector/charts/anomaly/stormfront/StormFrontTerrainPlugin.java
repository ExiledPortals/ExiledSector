package exiledsector.charts.anomaly.stormfront;

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
    private static final SpriteCache SPRITES = new SpriteCache(StormFrontTerrainPlugin.class);
    private static final EnumSet<CampaignEngineLayers> LAYERS = EnumSet.of(CampaignEngineLayers.TERRAIN_6);

    public record Params(String runId, StormCourse course, List<String> markIds, long startTimestamp, long seed, boolean solar,
                         float heldSlowShare) {

        Params(String runId, StormCourse course, List<String> markIds, long startTimestamp, long seed) {
            this(runId, course, markIds, startTimestamp, seed, false, 0f);
        }
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
    private boolean captured;
    private float holeX;
    private float holeY;
    private float innerRadius;
    private float outerRadius;
    private float startDegrees;
    private float orbitDays;
    private boolean solar;
    private float heldSlowShare;
    private float heldDays;
    private transient StormCourse course;
    private transient List<Puff> puffs;
    private transient float[] flashes;
    private transient float animationSeconds;
    private transient Random flashRandom;
    private transient SpriteAPI cloudSheet;
    private transient SpriteAPI flashSprite;
    private transient Vector2f scratchPosition;
    private transient float facingX;
    private transient float facingY;

    @Override
    public void init(String terrainId, SectorEntityToken entity, Object param) {
        super.init(terrainId, entity, param);
        if (param instanceof Params params) {
            runId = params.runId();
            keepCourse(params.course());
            markIds = new ArrayList<>(params.markIds());
            startTimestamp = params.startTimestamp();
            seed = params.seed();
            solar = params.solar();
            heldSlowShare = params.heldSlowShare();
        }
    }

    private void keepCourse(StormCourse startingCourse) {
        bandWidth = startingCourse.bandWidth();
        if (startingCourse instanceof StormPath crossing) {
            angleDegrees = crossing.angleDegrees();
            edgeRadius = crossing.edgeRadius();
            crossingDays = crossing.crossingDays();
        } else if (startingCourse instanceof StormOrbit orbit) {
            captured = true;
            holeX = orbit.holeX();
            holeY = orbit.holeY();
            innerRadius = orbit.innerRadius();
            outerRadius = orbit.outerRadius();
            startDegrees = orbit.startDegrees();
            orbitDays = orbit.orbitDays();
        }
    }

    public static void preloadTextures() {
        List.of(CLOUD_TEXTURE, FLASH_TEXTURE, StormOverlay.HIT_GLOW_TEXTURE).forEach(SPRITES::ensureLoaded);
    }

    public String runId() {
        return runId;
    }

    StormCourse course() {
        if (course == null) {
            course = captured ? new StormOrbit(holeX, holeY, innerRadius, outerRadius, bandWidth, startDegrees, orbitDays)
                    : new StormPath(angleDegrees, edgeRadius, bandWidth, crossingDays);
        }
        return course;
    }

    public boolean captured() {
        return captured;
    }

    public boolean solar() {
        return solar;
    }

    StormPalette palette() {
        return StormPalette.of(solar);
    }

    float elapsedDays() {
        return Global.getSector().getClock().getElapsedDaysSince(startTimestamp);
    }

    float travelledDays() {
        return travelledDays(elapsedDays(), heldDays);
    }

    static float travelledDays(float elapsedDays, float heldDays) {
        return Math.max(0f, elapsedDays - heldDays);
    }

    void holdFor(float days) {
        heldDays += days * heldSlowShare;
    }

    float heldDays() {
        return heldDays;
    }

    public boolean isDone() {
        return course().isDone(travelledDays());
    }

    @Override
    protected String nameKey() {
        return "chart.anomaly.storm_front.name";
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
        Vector2f centre = course().centre(travelledDays());
        entity.setFixedLocation(centre.x, centre.y);
        if (entity.isInCurrentLocation()) {
            animate(amount);
        }
    }

    @Override
    public void applyEffect(SectorEntityToken target, float days) {
        if (target instanceof CampaignFleetAPI fleet && fleet.getStats() != null) {
            if (fleet.isPlayerFleet() && heldSlowShare != 0f) {
                holdFor(days);
            }
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
        return entity != null && course().contains(point, pointRadius, travelledDays());
    }

    @Override
    public float getRenderRange() {
        return course().halfDiagonal() + RENDER_MARGIN;
    }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return LAYERS;
    }

    static List<Puff> puffs(StormCourse path, long seed) {
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
        if (!(course().bandWidth() > 0f) || !(course().bandLength() > 0f)) {
            return false;
        }
        puffs = puffs(course(), seed);
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
        float wobble = (float) Math.sin(puff.wobblePhase() + animationSeconds * WOBBLE_PER_SECOND) * WOBBLE_DISTANCE;
        float along = puff.along() + wobble;
        scratchPosition.set(centre.x + facingX * along - facingY * puff.across(), centre.y + facingY * along + facingX * puff.across());
        return scratchPosition;
    }

    private void faceCourse() {
        double facing = Math.toRadians(course().facingDegrees(travelledDays()));
        facingX = (float) Math.cos(facing);
        facingY = (float) Math.sin(facing);
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        float alpha = viewport.getAlphaMult();
        if (entity == null || alpha <= 0f || isDone() || !ensureSprites()) {
            return;
        }
        faceCourse();
        StormPalette palette = palette();
        Vector2f centre = entity.getLocation();
        float bandLength = course().bandLength();
        for (int index = 0; index < puffs.size(); index++) {
            Puff puff = puffs.get(index);
            Vector2f position = puffPosition(puff, centre);
            if (!viewport.isNearViewport(position, puff.size())) {
                continue;
            }
            float fade = endFade(puff.across(), bandLength);
            SpriteDraw.drawCellAtCenter(cloudSheet, SHEET_COLUMNS, SHEET_ROWS, puff.cell(), position.x, position.y, puff.size(), puff.size(),
                    puff.startFacing() + puff.spinDegreesPerSecond() * animationSeconds, palette.cloud(), alpha * CLOUD_ALPHA * fade, true);
            float flash = flashes[index];
            if (flash > 0f) {
                float flashSize = puff.size() * FLASH_SIZE_MULT;
                flashSprite.setSize(flashSize, flashSize);
                flashSprite.setColor(palette.flash());
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
        faceCourse();
        Color cloudColour = palette().cloud();
        Vector2f centre = entity.getLocation();
        float bandLength = course().bandLength();
        for (Puff puff : puffs) {
            Vector2f position = puffPosition(puff, centre);
            SpriteDraw.drawCellAtCenter(cloudSheet, SHEET_COLUMNS, SHEET_ROWS, puff.cell(), position.x * factor, position.y * factor,
                    puff.size() * factor, puff.size() * factor, puff.startFacing(), cloudColour, alphaMult * MAP_ALPHA * endFade(puff.across(), bandLength),
                    true);
        }
    }

    String intelText() {
        LocationAPI location = entity == null ? null : entity.getContainingLocation();
        float travelledDays = travelledDays();
        StormCourse stormCourse = course();
        int daysLeft = stormCourse instanceof StormPath crossing ? days(crossing.daysLeft(travelledDays)) : 0;
        List<StyledText> ahead = marksAhead(location, travelledDays);
        return I18n.forGameText(() -> {
            String where = stormCourse instanceof StormPath
                    ? Translation.msg("chart.anomaly.storm_front.intel.crossing").count(daysLeft).text()
                    : Translation.gameText("chart.anomaly.storm_front.intel.circling");
            if (ahead.isEmpty()) {
                return where;
            }
            return where + " " + Translation.msg("chart.anomaly.storm_front.intel.reaches").arg("places", Translation.list(ahead)).text();
        });
    }

    private List<StyledText> marksAhead(LocationAPI location, float travelledDays) {
        record Ahead(String name, float days) {
        }
        List<Ahead> ahead = new ArrayList<>();
        for (String markId : markIds == null ? List.<String>of() : markIds) {
            SectorEntityToken mark = location == null ? null : location.getEntityById(markId);
            float days = mark == null ? -1f : course().daysUntilReaches(mark.getLocation(), travelledDays);
            if (days > 0f && mark.getName() != null) {
                ahead.add(new Ahead(mark.getName(), days));
            }
        }
        ahead.sort(Comparator.comparingDouble(Ahead::days));
        List<StyledText> places = new ArrayList<>();
        for (Ahead mark : ahead.subList(0, Math.min(INTEL_MARKS, ahead.size()))) {
            int days = days(mark.days());
            places.add(I18n.forGameText(() -> Translation.msg("chart.anomaly.storm_front.intel.place")
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
        VanillaText.plainPara(tooltip, Translation.gameText("chart.anomaly.storm_front.description"), TOOLTIP_PAD, text);
        int sensorLess = Math.round((1f - StormRules.SENSOR_RANGE_MULT) * 100f);
        VanillaText.addPara(tooltip, Translation.msg("chart.anomaly.storm_front.effect.sensors").arg("value", sensorLess).styled(), TOOLTIP_PAD, text);
        VanillaText.plainPara(tooltip, Translation.gameText("chart.anomaly.storm_front.effect.strikes"), TOOLTIP_PAD, text);
        int itemIncrease = Math.round(StormRules.lootQuantityPercent(StormBattles.contextOf(StormBattles.runOf(this))));
        VanillaText.addPara(tooltip, Translation.msg("chart.anomaly.storm_front.effect.items").arg("value", itemIncrease).styled(), TOOLTIP_PAD, text);
    }
}
