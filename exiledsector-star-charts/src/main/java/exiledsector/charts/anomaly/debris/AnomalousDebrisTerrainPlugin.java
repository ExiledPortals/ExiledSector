package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseTerrain;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.Translation;
import exiledsector.ui.VanillaText;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class AnomalousDebrisTerrainPlugin extends BaseTerrain {

    public static final String SPRITE_CATEGORY = "terrain";
    public static final String SHEET_KEY = "debrisFieldSheet";
    public static final String GLOW_SHEET_KEY = "debrisFieldGlowSheet";
    public static final String MAP_ICON_KEY = "debrisFieldMapIcon";
    static final float CHECK_SECONDS = 0.25f;
    static final float RENDER_MARGIN = 200f;
    static final float TOOLTIP_PAD = 10f;
    static final float CELL_SIZE = 32f;
    static final float RADIUS_PER_PIECE = 6f;
    static final float MIN_PIECE_SIZE = 8f;
    static final float MAX_PIECE_SIZE = 26f;
    static final float MAX_DRIFT_DEGREES_PER_SECOND = 3f;
    static final float MAX_SPIN_DEGREES_PER_SECOND = 40f;
    static final float GLOW_CHANCE = 0.12f;
    static final float GLOW_ALPHA = 0.6f;
    static final float GLOW_PULSE_PER_SECOND = 1.2f;
    static final float ANIMATION_WRAP_SECONDS = 3600f;
    static final Color GLOW_COLOUR = new Color(110, 230, 255);
    private static final Logger LOG = Logger.getLogger(AnomalousDebrisTerrainPlugin.class);

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
    private transient float elapsedSeconds;
    private transient List<Piece> pieces;
    private transient List<SpriteAPI> sprites;
    private transient List<SpriteAPI> glows;
    private transient boolean spritesFailed;

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
        for (String key : List.of(SHEET_KEY, GLOW_SHEET_KEY)) {
            try {
                Global.getSettings().loadTexture(Global.getSettings().getSpriteName(SPRITE_CATEGORY, key));
            } catch (IOException | RuntimeException e) {
                LOG.warn("[ExiledSector] Could not preload vanilla's " + key + " for Anomalous Debris", e);
            }
        }
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
    protected boolean shouldCheckFleetsToApplyEffect() {
        return false;
    }

    @Override
    public String getEffectCategory() {
        return AnomalousDebris.TERRAIN_ID;
    }

    @Override
    public void advance(float amount) {
        if (amount <= 0f || entity == null || entity.getContainingLocation() == null) {
            return;
        }
        super.advance(amount);
        elapsedSeconds = (elapsedSeconds + amount) % ANIMATION_WRAP_SECONDS;
        if (revealed) {
            return;
        }
        secondsSinceCheck += amount;
        if (secondsSinceCheck < CHECK_SECONDS) {
            return;
        }
        secondsSinceCheck = 0f;
        try {
            CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (player != null && player.getContainingLocation() == entity.getContainingLocation() && player.getLocation() != null
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
        return EnumSet.of(CampaignEngineLayers.TERRAIN_7A);
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

    private boolean ensureSprites() {
        if (sprites != null) {
            return true;
        }
        if (spritesFailed) {
            return false;
        }
        try {
            pieces = pieces(radius, seed);
            sprites = new ArrayList<>(pieces.size());
            glows = new ArrayList<>(pieces.size());
            for (Piece piece : pieces) {
                sprites.add(cell(Global.getSettings().getSprite(SPRITE_CATEGORY, SHEET_KEY), piece));
                glows.add(piece.glows() ? cell(Global.getSettings().getSprite(SPRITE_CATEGORY, GLOW_SHEET_KEY), piece) : null);
            }
            return true;
        } catch (RuntimeException e) {
            spritesFailed = true;
            sprites = null;
            LOG.error("[ExiledSector] Anomalous Debris could not load vanilla's debris sprites", e);
            return false;
        }
    }

    private static SpriteAPI cell(SpriteAPI sheet, Piece piece) {
        int columns = Math.max(1, (int) (sheet.getWidth() / CELL_SIZE));
        int rows = Math.max(1, (int) (sheet.getHeight() / CELL_SIZE));
        float cellWidth = sheet.getTextureWidth() / columns;
        float cellHeight = sheet.getTextureHeight() / rows;
        sheet.setTexX(piece.cellX() % columns * cellWidth);
        sheet.setTexY(piece.cellY() % rows * cellHeight);
        sheet.setTexWidth(cellWidth);
        sheet.setTexHeight(cellHeight);
        sheet.setSize(piece.size(), piece.size());
        return sheet;
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
            double angle = Math.toRadians(piece.startAngle() + piece.driftDegreesPerSecond() * elapsedSeconds);
            float x = centre.x + (float) Math.cos(angle) * piece.orbitRadius();
            float y = centre.y + (float) Math.sin(angle) * piece.orbitRadius();
            float facing = piece.startFacing() + piece.spinDegreesPerSecond() * elapsedSeconds;
            SpriteAPI sprite = sprites.get(index);
            sprite.setAngle(facing);
            sprite.setColor(light);
            sprite.setNormalBlend();
            sprite.setAlphaMult(alpha);
            sprite.renderAtCenter(x, y);
            SpriteAPI glow = glows.get(index);
            if (glow != null) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(elapsedSeconds * GLOW_PULSE_PER_SECOND + piece.glowPhase());
                glow.setAngle(facing);
                glow.setColor(GLOW_COLOUR);
                glow.setAdditiveBlend();
                glow.setAlphaMult(alpha * GLOW_ALPHA * pulse);
                glow.renderAtCenter(x, y);
            }
        }
    }

    @Override
    public String getIconSpriteName() {
        return Global.getSettings().getSpriteName(SPRITE_CATEGORY, MAP_ICON_KEY);
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
    public String getTerrainName() {
        return Translation.gameText("chart.debris.name");
    }

    @Override
    public String getNameForTooltip() {
        return getTerrainName();
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addTitle(getTerrainName());
        VanillaText.plainPara(tooltip, Translation.gameText("chart.debris.description"), TOOLTIP_PAD, Misc.getTextColor());
    }
}
