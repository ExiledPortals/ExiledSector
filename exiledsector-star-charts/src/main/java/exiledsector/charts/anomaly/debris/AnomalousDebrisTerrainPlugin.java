package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.charts.anomaly.ChartAnomalyTerrain;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.VanillaText;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class AnomalousDebrisTerrainPlugin extends ChartAnomalyTerrain {

    public static final String SPRITE_CATEGORY = "terrain";
    public static final String SHEET_KEY = "debrisFieldSheet";
    public static final String GLOW_SHEET_KEY = "debrisFieldGlowSheet";
    public static final String MAP_ICON_KEY = "debrisFieldMapIcon";
    static final float CHECK_SECONDS = 0.25f;
    static final float RENDER_MARGIN = 200f;
    static final float CELL_SIZE = 32f;
    static final float RADIUS_PER_PIECE = 6f;
    static final float MIN_PIECE_SIZE = 8f;
    static final float MAX_PIECE_SIZE = 26f;
    static final float MAX_DRIFT_DEGREES_PER_SECOND = 3f;
    static final float MAX_SPIN_DEGREES_PER_SECOND = 40f;
    static final float GLOW_CHANCE = 0.12f;
    static final float GLOW_ALPHA = 0.6f;
    static final float GLOW_PULSE_PER_SECOND = 1.2f;
    static final float FULL_TURN_DEGREES = 360f;
    static final float FULL_PULSE = (float) Math.PI * 2f;
    static final Color GLOW_COLOUR = new Color(110, 230, 255);
    private static final Logger LOG = Logger.getLogger(AnomalousDebrisTerrainPlugin.class);
    private static final SpriteCache SPRITES = new SpriteCache(AnomalousDebrisTerrainPlugin.class);
    private static final EnumSet<CampaignEngineLayers> LAYERS = EnumSet.of(CampaignEngineLayers.TERRAIN_7A);

    public record Params(String runId, String fleetId, float radius, long seed) {
    }

    record Piece(float orbitRadius, float startAngle, float driftDegreesPerSecond, float startFacing, float spinDegreesPerSecond, float size,
                 int cellX, int cellY, boolean glows, float glowPhase) {
    }

    private String runId;
    private String fleetId;
    private float radius;
    private long seed;
    private boolean revealed;
    private transient float secondsSinceCheck;
    private transient float[] angles;
    private transient float[] facings;
    private transient float[] pulses;
    private transient List<Piece> pieces;
    private transient SpriteAPI sheet;
    private transient SpriteAPI glowSheet;
    private transient int sheetColumns;
    private transient int sheetRows;

    @Override
    public void init(String terrainId, SectorEntityToken entity, Object param) {
        super.init(terrainId, entity, param);
        if (param instanceof Params params) {
            runId = params.runId();
            fleetId = params.fleetId();
            radius = params.radius();
            seed = params.seed();
        }
    }

    public static void preloadTextures() {
        SPRITES.ensureLoaded(sheetPath(SHEET_KEY));
        SPRITES.ensureLoaded(sheetPath(GLOW_SHEET_KEY));
    }

    static String sheetPath(String key) {
        return Global.getSettings().getSpriteName(SPRITE_CATEGORY, key);
    }

    public String runId() {
        return runId;
    }

    public String fleetId() {
        return fleetId;
    }

    public float radius() {
        return radius;
    }

    public boolean isRevealed() {
        return revealed;
    }

    @Override
    protected String terrainId() {
        return AnomalousDebris.TERRAIN_ID;
    }

    @Override
    protected String nameKey() {
        return "chart.debris.name";
    }

    @Override
    protected void advanceLive(float amount) {
        if (entity.isInCurrentLocation() && ensurePhases()) {
            advancePhases(pieces, angles, facings, pulses, amount);
        }
        if (revealed) {
            return;
        }
        secondsSinceCheck += amount;
        if (secondsSinceCheck < CHECK_SECONDS) {
            return;
        }
        secondsSinceCheck = 0f;
        try {
            CampaignFleetAPI player = PlayerFleet.orNull();
            if (PlayerFleet.isIn(entity.getContainingLocation()) && player.getLocation() != null
                    && DebrisRules.reveals(Misc.getDistance(entity.getLocation(), player.getLocation()), player.getRadius(), radius)) {
                reveal();
            }
        } catch (RuntimeException | LinkageError e) {
            revealed = true;
            LOG.error("[ExiledSector] Anomalous Debris in Star Chart run " + runId + " could not reveal its fleet", e);
        }
    }

    void reveal() {
        revealed = true;
        DormantFleets.reveal(fleetIn(entity.getContainingLocation()));
    }

    CampaignFleetAPI fleetIn(LocationAPI location) {
        if (location == null || fleetId == null) {
            return null;
        }
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (fleetId.equals(fleet.getId())) {
                return fleet;
            }
        }
        return null;
    }

    @Override
    public boolean containsPoint(Vector2f point, float pointRadius) {
        return entity != null && point != null && Misc.getDistance(entity.getLocation(), point) < radius + pointRadius;
    }

    @Override
    public float getRenderRange() {
        return radius + RENDER_MARGIN;
    }

    @Override
    public EnumSet<CampaignEngineLayers> getActiveLayers() {
        return LAYERS;
    }

    static List<Piece> pieces(float radius, long seed) {
        Random random = new Random(seed);
        int count = Math.max(1, Math.round(radius / RADIUS_PER_PIECE));
        List<Piece> made = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            float orbit = (float) Math.sqrt(random.nextFloat()) * radius;
            float drift = (random.nextFloat() * 2f - 1f) * MAX_DRIFT_DEGREES_PER_SECOND;
            float spin = (random.nextFloat() * 2f - 1f) * MAX_SPIN_DEGREES_PER_SECOND;
            float size = MIN_PIECE_SIZE + random.nextFloat() * (MAX_PIECE_SIZE - MIN_PIECE_SIZE);
            made.add(new Piece(orbit, random.nextFloat() * 360f, drift, random.nextFloat() * 360f, spin, size, random.nextInt(Integer.MAX_VALUE),
                    random.nextInt(Integer.MAX_VALUE), random.nextFloat() < GLOW_CHANCE, random.nextFloat() * (float) Math.PI * 2f));
        }
        return made;
    }

    private boolean ensurePhases() {
        if (angles != null) {
            return true;
        }
        if (radius <= 0f) {
            return false;
        }
        pieces = pieces(radius, seed);
        angles = new float[pieces.size()];
        facings = new float[pieces.size()];
        pulses = new float[pieces.size()];
        for (int index = 0; index < pieces.size(); index++) {
            Piece piece = pieces.get(index);
            angles[index] = piece.startAngle();
            facings[index] = piece.startFacing();
            pulses[index] = piece.glowPhase();
        }
        return true;
    }

    static void advancePhases(List<Piece> pieces, float[] angles, float[] facings, float[] pulses, float seconds) {
        for (int index = 0; index < pieces.size(); index++) {
            Piece piece = pieces.get(index);
            angles[index] = wrap(angles[index] + piece.driftDegreesPerSecond() * seconds, FULL_TURN_DEGREES);
            facings[index] = wrap(facings[index] + piece.spinDegreesPerSecond() * seconds, FULL_TURN_DEGREES);
            pulses[index] = wrap(pulses[index] + GLOW_PULSE_PER_SECOND * seconds, FULL_PULSE);
        }
    }

    static float wrap(float value, float period) {
        float wrapped = value % period;
        return wrapped < 0f ? wrapped + period : wrapped;
    }

    private boolean ensureSprites() {
        if (sheet != null) {
            return true;
        }
        String sheetPath = sheetPath(SHEET_KEY);
        if (!ensurePhases() || !SPRITES.ensureLoaded(sheetPath)) {
            return false;
        }
        SpriteAPI loaded = Global.getSettings().getSprite(sheetPath);
        sheetColumns = Math.max(1, (int) (loaded.getWidth() / CELL_SIZE));
        sheetRows = Math.max(1, (int) (loaded.getHeight() / CELL_SIZE));
        String glowPath = sheetPath(GLOW_SHEET_KEY);
        glowSheet = SPRITES.ensureLoaded(glowPath) ? Global.getSettings().getSprite(glowPath) : null;
        sheet = loaded;
        return true;
    }

    int cellOf(Piece piece) {
        return piece.cellY() % sheetRows * sheetColumns + piece.cellX() % sheetColumns;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        float alpha = viewport.getAlphaMult();
        if (entity == null || alpha <= 0f || !ensureSprites()) {
            return;
        }
        Vector2f centre = entity.getLocation();
        Color light = entity.getLightSource() != null && entity.getLightColor() != null ? entity.getLightColor() : Color.WHITE;
        for (int index = 0; index < pieces.size(); index++) {
            Piece piece = pieces.get(index);
            double angle = Math.toRadians(angles[index]);
            float x = centre.x + (float) Math.cos(angle) * piece.orbitRadius();
            float y = centre.y + (float) Math.sin(angle) * piece.orbitRadius();
            float facing = facings[index];
            int cell = cellOf(piece);
            SpriteDraw.drawCellAtCenter(sheet, sheetColumns, sheetRows, cell, x, y, piece.size(), piece.size(), facing, light, alpha, false);
            if (piece.glows() && glowSheet != null) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(pulses[index]);
                SpriteDraw.drawCellAtCenter(glowSheet, sheetColumns, sheetRows, cell, x, y, piece.size(), piece.size(), facing, GLOW_COLOUR,
                        alpha * GLOW_ALPHA * pulse, true);
            }
        }
    }

    @Override
    public String getIconSpriteName() {
        return Global.getSettings().getSpriteName(SPRITE_CATEGORY, MAP_ICON_KEY);
    }

    @Override
    protected void tooltipLines(TooltipMakerAPI tooltip) {
        VanillaText.plainPara(tooltip, Translation.gameText("chart.debris.description"), TOOLTIP_PAD, Misc.getTextColor());
    }
}
