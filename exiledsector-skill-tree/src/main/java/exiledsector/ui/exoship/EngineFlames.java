package exiledsector.ui.exoship;

import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;

final class EngineFlames {

    static final String FLAME_PATH = "graphics/fx/engineflame32.png";
    static final String NOZZLE_GLOW_PATH = "graphics/fx/hit_glow.png";
    static final float MAX_LENGTH_PER_WIDTH = 3.5f;
    static final float MIN_FLAME_PIXELS = 3f;
    private static final float CORE_LENGTH_SHARE = 0.6f;
    private static final float CORE_WIDTH_SHARE = 0.45f;
    private static final float CORE_ALPHA = 0.8f;
    private static final float NOZZLE_GLOW_WIDTHS = 1.4f;
    private static final float NOZZLE_GLOW_ALPHA = 0.6f;
    private static final Color CORE_COLOR = new Color(255, 236, 205);

    private EngineFlames() {
    }

    static float flicker(float elapsedSeconds, int engineIndex) {
        return 1f + 0.025f * (float) Math.sin(elapsedSeconds * 6f + engineIndex * 1.7f)
                + 0.01f * (float) Math.sin(elapsedSeconds * 11f + engineIndex * 2.9f);
    }

    static float cappedLength(float flameLength, float flameWidth) {
        return Math.min(flameLength, flameWidth * MAX_LENGTH_PER_WIDTH);
    }

    static float flameAngle(float facingDeg) {
        return 180f - facingDeg;
    }

    static void draw(SpriteCache spriteCache, float nozzleX, float nozzleY, float forwardX, float forwardY, float facingDeg,
                     float flameLength, float flameWidth, Color color, float alpha) {
        if (alpha <= 0f || flameWidth < MIN_FLAME_PIXELS) {
            return;
        }
        float length = cappedLength(flameLength, flameWidth);
        float angle = flameAngle(facingDeg);
        SpriteDraw.drawAdditiveAtCenter(spriteCache, FLAME_PATH, nozzleX - forwardX * length / 2f, nozzleY - forwardY * length / 2f,
                length, flameWidth, color, alpha, angle);
        float coreLength = length * CORE_LENGTH_SHARE;
        SpriteDraw.drawAdditiveAtCenter(spriteCache, FLAME_PATH, nozzleX - forwardX * coreLength / 2f, nozzleY - forwardY * coreLength / 2f,
                coreLength, flameWidth * CORE_WIDTH_SHARE, CORE_COLOR, alpha * CORE_ALPHA, angle);
        float glowSize = flameWidth * NOZZLE_GLOW_WIDTHS;
        SpriteDraw.drawAdditiveAtCenter(spriteCache, NOZZLE_GLOW_PATH, nozzleX, nozzleY, glowSize, glowSize, color, alpha * NOZZLE_GLOW_ALPHA);
    }
}
