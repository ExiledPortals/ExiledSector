package exiledsector.ui.exoship;

import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class ExoshipRenderer {

    static final String HULL_PATH = "graphics/fx/rat_exoship.png";
    static final String LIGHTS_PATH = "graphics/fx/rat_exoship_lights.png";
    static final String EXTERIOR_LIGHTS_PATH = "graphics/fx/rat_exoship_ext_lights.png";
    static final String FLASH_CORE_PATH = "graphics/fx/hit_glow.png";
    static final String FLASH_HALO_PATH = "graphics/fx/star_halo.png";
    static final Color EXO_ORANGE = new Color(248, 172, 44);
    static final Color EXO_EMBER = new Color(247, 121, 86);
    static final Color WARP_PURPLE = new Color(130, 4, 189);

    public static final float WIDTH_PER_LENGTH = 95f / 150f;
    private static final float LIGHTS_LENGTH_RATIO = 140f / 150f;
    private static final float[][] ENGINES = {
            {-0.35f, 0f, 0.17f, 0.5f},
            {-0.27f, 0.11f, 0.1f, 0.27f},
            {-0.25f, 0.14f, 0.1f, 0.27f},
            {-0.27f, -0.11f, 0.1f, 0.27f},
            {-0.25f, -0.14f, 0.1f, 0.27f}
    };
    private static final float TRAIL_SPAWN_SECONDS = 0.05f;
    private static final float TRAIL_BASE_ALPHA = 0.3f;
    private static final float TRAIL_BASE_LIFE = 0.5f;
    private static final float TRAIL_EXTRA_LIFE = 1.5f;
    private static final int STREAK_STEPS = 60;
    private static final float STREAK_SPACING_PER_LENGTH = 0.36f;
    private static final float STREAK_SECONDS_PER_STEP = 0.05f;
    private static final float STREAK_ALPHA = 0.3f;
    private static final float FLASH_SECONDS = 1.2f;
    private static final float FLASH_CORE_LENGTHS = 5f;
    private static final float FLASH_HALO_LENGTHS = 25f;
    private static final float UNDOCKED_LIGHTS_ALPHA = 0.35f;
    private static final float HOVER_GLOW_ALPHA = 0.35f;
    private static final int TRAIL_COLOR_STEPS = 16;
    private static final Color[] TRAIL_COLORS = trailColors();

    private static final class Afterimage {
        float treeX;
        float treeY;
        float facingDeg;
        float length;
        float ageSeconds;
        float lifeSeconds;
        float startAlpha;
    }

    private static final class Flash {
        float treeX;
        float treeY;
        float length;
        float ageSeconds;
    }

    private final SpriteCache spriteCache = new SpriteCache(ExoshipRenderer.class);
    private final List<Afterimage> afterimages = new ArrayList<>();
    private final List<Flash> flashes = new ArrayList<>();
    private final ExoshipEscorts escorts = new ExoshipEscorts();
    private float secondsSinceTrail;
    private float elapsedSeconds;

    public void advance(float amount, ExoshipVoyage voyage, float shipLength) {
        elapsedSeconds += amount;
        escorts.advance(amount);
        for (Afterimage afterimage : afterimages) {
            afterimage.ageSeconds += amount;
        }
        afterimages.removeIf(afterimage -> afterimage.ageSeconds >= afterimage.lifeSeconds);
        for (Flash flash : flashes) {
            flash.ageSeconds += amount;
        }
        flashes.removeIf(flash -> flash.ageSeconds >= FLASH_SECONDS);
        float speedLevel = voyage.speedLevel();
        secondsSinceTrail += amount;
        if (speedLevel > 0f && voyage.isVisible() && secondsSinceTrail >= TRAIL_SPAWN_SECONDS) {
            secondsSinceTrail = 0f;
            addAfterimage(voyage.shipX(), voyage.shipY(), voyage.facingDeg(), shipLength, 0f,
                    TRAIL_BASE_LIFE + TRAIL_EXTRA_LIFE * speedLevel, TRAIL_BASE_ALPHA * speedLevel);
        }
    }

    public void departed(float shipX, float shipY, float headingX, float headingY, float shipLength) {
        addFlash(shipX, shipY, shipLength);
        float facingDeg = (float) Math.toDegrees(Math.atan2(headingY, headingX));
        float spacing = shipLength * STREAK_SPACING_PER_LENGTH;
        for (int step = 0; step < STREAK_STEPS; step++) {
            float fade = 1f - (float) step / STREAK_STEPS;
            addAfterimage(shipX + headingX * spacing * step, shipY + headingY * spacing * step, facingDeg, shipLength, 0f,
                    STREAK_SECONDS_PER_STEP * (step + 1), STREAK_ALPHA * fade);
        }
    }

    public void arrived(float shipX, float shipY, float headingX, float headingY, float shipLength) {
        addFlash(shipX, shipY, shipLength);
        float facingDeg = (float) Math.toDegrees(Math.atan2(headingY, headingX));
        float spacing = shipLength * STREAK_SPACING_PER_LENGTH;
        for (int step = 0; step < STREAK_STEPS; step++) {
            float fade = (float) step / STREAK_STEPS;
            addAfterimage(shipX - headingX * spacing * step, shipY - headingY * spacing * step, facingDeg, shipLength, 0f,
                    STREAK_SECONDS_PER_STEP * (STREAK_STEPS - step), STREAK_ALPHA * fade);
        }
    }

    private void addAfterimage(float treeX, float treeY, float facingDeg, float length, float ageSeconds, float lifeSeconds,
                               float startAlpha) {
        Afterimage afterimage = new Afterimage();
        afterimage.treeX = treeX;
        afterimage.treeY = treeY;
        afterimage.facingDeg = facingDeg;
        afterimage.length = length;
        afterimage.ageSeconds = ageSeconds;
        afterimage.lifeSeconds = lifeSeconds;
        afterimage.startAlpha = startAlpha;
        afterimages.add(afterimage);
    }

    private void addFlash(float treeX, float treeY, float length) {
        Flash flash = new Flash();
        flash.treeX = treeX;
        flash.treeY = treeY;
        flash.length = length;
        flashes.add(flash);
    }

    public void render(TreeViewport viewport, ExoshipVoyage voyage, float shipLength, float dockedLevel, boolean hovered, float escortLevel,
                       float alphaMult) {
        if (alphaMult <= 0f) {
            return;
        }
        float zoom = viewport.zoom();
        for (Afterimage afterimage : afterimages) {
            float progress = afterimage.ageSeconds / afterimage.lifeSeconds;
            float alpha = afterimage.startAlpha * (1f - progress) * alphaMult;
            float screenX = viewport.screenX(afterimage.treeX);
            float screenY = viewport.screenY(afterimage.treeY);
            float screenLength = afterimage.length * zoom;
            if (alpha <= 0f || !viewport.isVisible(screenX, screenY, screenLength)) {
                continue;
            }
            SpriteDraw.drawAdditiveAtCenter(spriteCache, HULL_PATH, screenX, screenY, screenLength * WIDTH_PER_LENGTH, screenLength,
                    trailColor(progress), alpha, spriteAngle(afterimage.facingDeg));
        }
        if (voyage.isVisible()) {
            float escortAlpha = alphaMult * escortLevel;
            escorts.render(viewport, voyage.shipX(), voyage.shipY(), shipLength, false, escortAlpha);
            renderShip(viewport, voyage, shipLength, dockedLevel, hovered, alphaMult);
            escorts.render(viewport, voyage.shipX(), voyage.shipY(), shipLength, true, escortAlpha);
        }
        for (Flash flash : flashes) {
            renderFlash(viewport, flash, alphaMult);
        }
    }

    private void renderShip(TreeViewport viewport, ExoshipVoyage voyage, float shipLength, float dockedLevel, boolean hovered,
                            float alphaMult) {
        float screenX = viewport.screenX(voyage.shipX());
        float screenY = viewport.screenY(voyage.shipY());
        float screenLength = shipLength * viewport.zoom();
        if (!viewport.isVisible(screenX, screenY, screenLength)) {
            return;
        }
        float angle = spriteAngle(voyage.facingDeg());
        renderEngines(screenX, screenY, screenLength, voyage.facingDeg(), voyage.thrustLevel(), alphaMult);
        SpriteDraw.drawAtCenter(spriteCache, HULL_PATH, screenX, screenY, screenLength * WIDTH_PER_LENGTH, screenLength, null, alphaMult, angle);
        if (hovered) {
            SpriteDraw.drawAdditiveAtCenter(spriteCache, HULL_PATH, screenX, screenY, screenLength * WIDTH_PER_LENGTH, screenLength, EXO_ORANGE,
                    alphaMult * HOVER_GLOW_ALPHA, angle);
        }
        float lightsAlpha = alphaMult * (UNDOCKED_LIGHTS_ALPHA + (1f - UNDOCKED_LIGHTS_ALPHA) * dockedLevel);
        float lightsLength = screenLength * LIGHTS_LENGTH_RATIO;
        SpriteDraw.drawAtCenter(spriteCache, EXTERIOR_LIGHTS_PATH, screenX, screenY, screenLength * WIDTH_PER_LENGTH, lightsLength, null,
                lightsAlpha, angle);
        SpriteDraw.drawAtCenter(spriteCache, LIGHTS_PATH, screenX, screenY, screenLength * WIDTH_PER_LENGTH, lightsLength, null,
                lightsAlpha, angle);
    }

    private void renderEngines(float screenX, float screenY, float screenLength, float facingDeg, float thrustLevel, float alphaMult) {
        double screenFacing = Math.toRadians(-facingDeg);
        float forwardX = (float) Math.cos(screenFacing);
        float forwardY = (float) Math.sin(screenFacing);
        for (int engineIndex = 0; engineIndex < ENGINES.length; engineIndex++) {
            float[] engine = ENGINES[engineIndex];
            float forwardOffset = engine[0] * screenLength;
            float leftOffset = engine[1] * screenLength;
            float engineX = screenX + forwardX * forwardOffset - forwardY * leftOffset;
            float engineY = screenY + forwardY * forwardOffset + forwardX * leftOffset;
            float flicker = EngineFlames.flicker(elapsedSeconds, engineIndex);
            float flameWidth = engine[2] * screenLength * (0.7f + 0.3f * thrustLevel);
            float flameLength = engine[3] * screenLength * (0.3f + thrustLevel) * flicker;
            EngineFlames.draw(spriteCache, engineX, engineY, forwardX, forwardY, facingDeg, flameLength, flameWidth, EXO_EMBER,
                    alphaMult * (0.5f + 0.5f * thrustLevel) * Math.min(1f, flicker));
        }
    }

    private void renderFlash(TreeViewport viewport, Flash flash, float alphaMult) {
        float progress = flash.ageSeconds / FLASH_SECONDS;
        float fade = (1f - progress) * (1f - progress);
        float screenX = viewport.screenX(flash.treeX);
        float screenY = viewport.screenY(flash.treeY);
        float haloSize = flash.length * FLASH_HALO_LENGTHS * (0.4f + 0.6f * progress) * viewport.zoom();
        if (!viewport.isVisible(screenX, screenY, haloSize / 2f)) {
            return;
        }
        SpriteDraw.drawAdditiveAtCenter(spriteCache, FLASH_HALO_PATH, screenX, screenY, haloSize, haloSize, WARP_PURPLE, alphaMult * 0.7f * fade);
        float coreSize = flash.length * FLASH_CORE_LENGTHS * (0.6f + 1.4f * progress) * viewport.zoom();
        SpriteDraw.drawAdditiveAtCenter(spriteCache, FLASH_CORE_PATH, screenX, screenY, coreSize, coreSize, EXO_ORANGE, alphaMult * fade);
        SpriteDraw.drawAdditiveAtCenter(spriteCache, FLASH_CORE_PATH, screenX, screenY, coreSize * 0.4f, coreSize * 0.4f, Color.WHITE,
                alphaMult * fade);
    }

    int afterimageCount() {
        return afterimages.size();
    }

    int flashCount() {
        return flashes.size();
    }

    static float spriteAngle(float treeFacingDeg) {
        return -treeFacingDeg - 90f;
    }

    static Color trailColor(float progress) {
        int step = Math.round(Math.max(0f, Math.min(1f, progress)) * TRAIL_COLOR_STEPS);
        return TRAIL_COLORS[step];
    }

    private static Color[] trailColors() {
        Color[] colors = new Color[TRAIL_COLOR_STEPS + 1];
        for (int step = 0; step <= TRAIL_COLOR_STEPS; step++) {
            float mix = (float) step / TRAIL_COLOR_STEPS;
            colors[step] = new Color(Math.round(EXO_ORANGE.getRed() + (WARP_PURPLE.getRed() - EXO_ORANGE.getRed()) * mix),
                    Math.round(EXO_ORANGE.getGreen() + (WARP_PURPLE.getGreen() - EXO_ORANGE.getGreen()) * mix),
                    Math.round(EXO_ORANGE.getBlue() + (WARP_PURPLE.getBlue() - EXO_ORANGE.getBlue()) * mix));
        }
        return colors;
    }
}
