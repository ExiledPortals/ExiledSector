package exiledsector.charts.phasing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEngineLayers;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseCustomEntityPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WarpingSpriteRendererUtil;
import exiledsector.ui.VanillaText;
import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

public class PhaseGateEntityPlugin extends BaseCustomEntityPlugin {

    private static final Logger LOG = Logger.getLogger(PhaseGateEntityPlugin.class);

    static final Color PHASE_COLOUR = new Color(255, 175, 255);
    static final String SPRITE_CATEGORY = "exiledSector_phase_gate";
    static final String LOOP_SOUND = "active_gate_loop";
    static final String ARRIVAL_SOUND = "system_phase_cloak_activate";
    static final String PHASE_IN_SOUND = "system_phase_cloak_deactivate";
    static final String ARC_SOUND = "mote_attractor_impact_emp_arc";
    static final float MIN_FRAME_RADIUS = 96f;
    static final float INNER_OPENING_SHARE = 0.78f;
    static final float FRAME_MARGIN = 1.15f;
    static final float MIN_GLOW = 0.35f;
    static final float MIN_GHOST_ALPHA = 0.2f;
    static final float MAX_GHOST_ALPHA = 0.6f;
    static final float STARFIELD_ALPHA = 0.55f;
    static final float ARRIVAL_SECONDS = 4f;
    static final float PHASE_IN_SECONDS = 2.5f;
    static final float ARCS_PER_SECOND = 14f;
    static final float ARC_SOUND_SHARE = 0.25f;
    static final int JITTER_COPIES = 6;
    static final float JITTER_RANGE_SHARE = 0.12f;
    static final float JITTER_ALPHA = 0.45f;
    static final float PHASE_IN_GLOW_DROP = 0.75f;
    static final float RENDER_PAD = 500f;
    static final float SPIN_DEGREES_PER_SECOND = 60f;
    static final int WARP_CELLS = 6;
    static final float SOUND_FULL_RANGE = 300f;
    static final float SOUND_FADE_RANGE = 100f;
    static final float SOUND_SOURCE_DISTANCE = 500f;
    static final float TOOLTIP_PAD = 10f;

    private float progressShare;
    private String planetTexture;
    private float planetRadius;
    private float arrivalSeconds = -1f;
    private boolean planetPlaced;

    private transient GateSprites sprites;
    private transient boolean spritesMissing;
    private transient PhasedPlanetGhost ghost;
    private transient boolean ghostMissing;
    private transient WarpingSpriteRendererUtil starfieldWarp;
    private transient PhaseArcs arcs;
    private transient Random effectRandom;
    private transient float arcBudget;
    private transient float spinDegrees;

    record GateSprites(SpriteAPI frame, SpriteAPI starfield, SpriteAPI ring, SpriteAPI rays, SpriteAPI concentric, SpriteAPI innerWhirl,
                       SpriteAPI outerWhirl, SpriteAPI arcFringe, SpriteAPI arcCore) {
    }

    record ArrivalLevels(float phaseIn, float settle) {
    }

    @Override
    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        readResolve();
    }

    Object readResolve() {
        sprites = null;
        spritesMissing = false;
        ghost = null;
        ghostMissing = false;
        starfieldWarp = null;
        arcs = new PhaseArcs();
        effectRandom = new Random();
        return this;
    }

    public static float frameRadiusFor(float planetRadius) {
        return Math.max(MIN_FRAME_RADIUS, planetRadius / INNER_OPENING_SHARE * FRAME_MARGIN);
    }

    void configure(String texture, float radius) {
        if (texture != null && !texture.equals(planetTexture)) {
            ghost = null;
            ghostMissing = false;
        }
        planetTexture = texture;
        planetRadius = radius;
    }

    public float progressShare() {
        return progressShare;
    }

    void setProgressShare(float share) {
        progressShare = Math.max(0f, Math.min(1f, share));
    }

    public boolean isArriving() {
        return arrivalSeconds >= 0f;
    }

    float arrivalSeconds() {
        return arrivalSeconds;
    }

    void beginArrival() {
        if (isArriving()) {
            return;
        }
        arrivalSeconds = 0f;
        playSound(ARRIVAL_SOUND);
    }

    static float glowAlpha(float progressShare) {
        return MIN_GLOW + (1f - MIN_GLOW) * Math.max(0f, Math.min(1f, progressShare));
    }

    static float pullGhostAlpha(float progressShare) {
        return MIN_GHOST_ALPHA + (MAX_GHOST_ALPHA - MIN_GHOST_ALPHA) * Math.max(0f, Math.min(1f, progressShare));
    }

    static ArrivalLevels arrivalLevels(float seconds) {
        if (seconds < 0f) {
            return new ArrivalLevels(0f, 1f);
        }
        float phaseIn = Math.min(1f, seconds / PHASE_IN_SECONDS);
        float settle = 1f - Math.max(0f, Math.min(1f, (seconds - PHASE_IN_SECONDS) / (ARRIVAL_SECONDS - PHASE_IN_SECONDS)));
        return new ArrivalLevels(phaseIn, settle);
    }

    static float arrivalIntensity(float seconds) {
        if (seconds < 0f) {
            return 0f;
        }
        ArrivalLevels levels = arrivalLevels(seconds);
        return seconds < PHASE_IN_SECONDS ? levels.phaseIn() : levels.settle();
    }

    static float ghostAlpha(float progressShare, float arrivalSeconds) {
        if (arrivalSeconds < 0f) {
            return pullGhostAlpha(progressShare);
        }
        ArrivalLevels levels = arrivalLevels(arrivalSeconds);
        if (arrivalSeconds < PHASE_IN_SECONDS) {
            float start = pullGhostAlpha(progressShare);
            return start + (1f - start) * levels.phaseIn();
        }
        return levels.settle();
    }

    @Override
    public void advance(float amount) {
        if (entity == null || amount <= 0f) {
            return;
        }
        if (isArriving()) {
            advanceArrival(amount);
        }
        if (!entity.isInCurrentLocation()) {
            return;
        }
        spinDegrees = Misc.normalizeAngle(spinDegrees + amount * SPIN_DEGREES_PER_SECOND);
        if (starfieldWarp != null) {
            starfieldWarp.advance(amount);
        }
        arcs.advance(amount);
        if (!isArriving() || arrivalSeconds < PHASE_IN_SECONDS) {
            playProximityLoop();
        }
    }

    private void advanceArrival(float amount) {
        arrivalSeconds += amount;
        if (!planetPlaced && arrivalSeconds >= PHASE_IN_SECONDS) {
            planetPlaced = true;
            PhasePull.finishArrival(Global.getSector(), entity);
            playSound(PHASE_IN_SOUND);
        }
        if (entity.isInCurrentLocation()) {
            spawnArcs(amount);
        }
        if (arrivalSeconds >= ARRIVAL_SECONDS) {
            entity.setExpired(true);
        }
    }

    private void spawnArcs(float amount) {
        arcBudget += amount * ARCS_PER_SECOND * arrivalIntensity(arrivalSeconds);
        while (arcBudget >= 1f) {
            arcBudget -= 1f;
            arcs.spawnAround(effectRandom, planetRadius);
            if (effectRandom.nextFloat() < ARC_SOUND_SHARE) {
                playSound(ARC_SOUND);
            }
        }
    }

    private void playSound(String soundId) {
        if (entity != null && entity.isInCurrentLocation()) {
            Global.getSoundPlayer().playSound(soundId, 1f, 1f, entity.getLocation(), Misc.ZERO);
        }
    }

    private void playProximityLoop() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) {
            return;
        }
        float gap = Misc.getDistance(playerFleet.getLocation(), entity.getLocation()) - entity.getRadius() - playerFleet.getRadius();
        float closeness = Math.max(0f, Math.min(1f, 1f - (gap - SOUND_FULL_RANGE) / SOUND_FADE_RANGE));
        if (closeness <= 0f) {
            return;
        }
        Global.getSector().getCampaignUI().suppressMusic(closeness);
        Vector2f source = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(playerFleet.getLocation(), entity.getLocation()));
        source.scale(SOUND_SOURCE_DISTANCE);
        Vector2f.add(entity.getLocation(), source, source);
        Global.getSoundPlayer().playLoop(LOOP_SOUND, entity, 1f, (float) Math.sqrt(closeness), source, Misc.ZERO);
    }

    @Override
    public float getRenderRange() {
        return entity == null ? RENDER_PAD : entity.getRadius() + RENDER_PAD;
    }

    @Override
    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        if (entity == null) {
            return;
        }
        float alpha = viewport.getAlphaMult() * entity.getSensorFaderBrightness() * entity.getSensorContactFaderBrightness();
        if (alpha <= 0f || !loadSprites()) {
            return;
        }
        Vector2f location = entity.getLocation();
        ArrivalLevels levels = arrivalLevels(arrivalSeconds);
        float gateAlpha = alpha * levels.settle();
        float glow = gateAlpha * glowAlpha(progressShare) * (1f - PHASE_IN_GLOW_DROP * levels.phaseIn());
        if (layer == CampaignEngineLayers.BELOW_STATIONS) {
            renderGhost(location, alpha);
            renderStarfield(sprites.starfield(), location.x, location.y, glow * STARFIELD_ALPHA * (1f - levels.phaseIn()));
        } else if (layer == CampaignEngineLayers.STATIONS) {
            renderAdditive(sprites.ring(), location.x, location.y, glow, 0f);
            renderAdditive(sprites.rays(), location.x, location.y, glow, 0f);
            renderAdditive(sprites.concentric(), location.x, location.y, glow, 0f);
            renderAdditive(sprites.innerWhirl(), location.x, location.y, glow, -spinDegrees * 0.25f);
            renderAdditive(sprites.outerWhirl(), location.x, location.y, glow * 0.5f, -spinDegrees * 0.33f);
            renderFrame(sprites.frame(), location, gateAlpha);
        } else if (layer == CampaignEngineLayers.ABOVE_STATIONS) {
            arcs.render(sprites.arcFringe(), sprites.arcCore(), location.x, location.y, PHASE_COLOUR, alpha);
        }
    }

    private void renderGhost(Vector2f location, float alpha) {
        if (!loadGhost()) {
            return;
        }
        float[] light = lightDirection(location);
        ghost.render(location.x, location.y, planetRadius, light, Color.WHITE, alpha * ghostAlpha(progressShare, arrivalSeconds), false);
        float jitter = arrivalIntensity(arrivalSeconds);
        if (jitter <= 0f) {
            return;
        }
        float range = planetRadius * JITTER_RANGE_SHARE * jitter;
        for (int copy = 0; copy < JITTER_COPIES; copy++) {
            float offsetX = (effectRandom.nextFloat() * 2f - 1f) * range;
            float offsetY = (effectRandom.nextFloat() * 2f - 1f) * range;
            ghost.render(location.x + offsetX, location.y + offsetY, planetRadius, light, PHASE_COLOUR, alpha * JITTER_ALPHA * jitter, true);
        }
    }

    private float[] lightDirection(Vector2f location) {
        SectorEntityToken light = entity.getLightSource();
        if (light == null || light.getLocation() == null) {
            return PhasedPlanetGhost.lightDirection(0f, 0f);
        }
        return PhasedPlanetGhost.lightDirection(light.getLocation().x - location.x, light.getLocation().y - location.y);
    }

    private void renderStarfield(SpriteAPI starfield, float centreX, float centreY, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        if (starfieldWarp == null) {
            float cellSize = starfield.getWidth() / 10f;
            starfieldWarp = new WarpingSpriteRendererUtil(WARP_CELLS, WARP_CELLS, cellSize * 0.2f, cellSize * 0.2f, 2f);
        }
        starfield.setAlphaMult(alpha);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        starfieldWarp.renderNoBlendOrRotate(starfield, centreX - starfield.getWidth() / 2f, centreY - starfield.getHeight() / 2f, false);
    }

    private void renderFrame(SpriteAPI frame, Vector2f location, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        Color light = entity.getLightColor();
        frame.setColor(light == null ? Color.WHITE : light);
        frame.setAlphaMult(alpha);
        frame.setNormalBlend();
        frame.renderAtCenter(location.x, location.y);
    }

    private static void renderAdditive(SpriteAPI sprite, float centreX, float centreY, float alpha, float angle) {
        if (alpha <= 0f) {
            return;
        }
        sprite.setAngle(Misc.normalizeAngle(angle));
        sprite.setAlphaMult(alpha);
        sprite.setAdditiveBlend();
        sprite.renderAtCenter(centreX, centreY);
    }

    private boolean loadGhost() {
        if (ghost != null) {
            return true;
        }
        if (ghostMissing || planetTexture == null) {
            return false;
        }
        try {
            ghost = PhasedPlanetGhost.load(planetTexture);
        } catch (IOException | RuntimeException e) {
            LOG.warn("[ExiledSector] The phase gate could not load the incoming planet's texture " + planetTexture, e);
            ghost = null;
        }
        ghostMissing = ghost == null;
        return !ghostMissing;
    }

    private boolean loadSprites() {
        if (sprites != null) {
            return true;
        }
        if (spritesMissing) {
            return false;
        }
        try {
            SpriteAPI frame = Global.getSettings().getSprite(SPRITE_CATEGORY, "frame");
            float frameSize = entity.getRadius() * 2f;
            float scale = frameSize / frame.getWidth();
            frame.setSize(frameSize, frameSize);
            sprites = new GateSprites(frame, scaled("starfield", scale), scaled("ring", scale), scaled("rays", scale),
                    scaled("concentric", scale), scaled("whirl1", scale), scaled("whirl2", scale),
                    Global.getSettings().getSprite(SPRITE_CATEGORY, "arc_fringe"), Global.getSettings().getSprite(SPRITE_CATEGORY, "arc_core"));
        } catch (RuntimeException e) {
            LOG.warn("[ExiledSector] The phase gate sprites could not be loaded; the gate draws nothing", e);
            sprites = null;
        }
        spritesMissing = sprites == null;
        return !spritesMissing;
    }

    private static SpriteAPI scaled(String key, float scale) {
        SpriteAPI sprite = Global.getSettings().getSprite(SPRITE_CATEGORY, key);
        sprite.setSize(sprite.getWidth() * scale, sprite.getHeight() * scale);
        return sprite;
    }

    @Override
    public void appendToCampaignTooltip(TooltipMakerAPI tooltip, SectorEntityToken.VisibilityLevel level) {
        String status = PhasePull.statusLine(PhasePull.gateMarketOf(entity));
        if (status != null) {
            tooltip.setParaFontDefault();
            VanillaText.plainPara(tooltip, status, TOOLTIP_PAD, Misc.getGrayColor());
        }
    }
}
