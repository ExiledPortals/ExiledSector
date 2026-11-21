package exiledsector.ui.atlas;

import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;

final class StarFlareGlow {

    record Rise(float lift, float startSize, float growth, float peakAlpha, int trailGlows) {
    }

    static final Rise MAP_RISE = new Rise(0.7f, 0.6f, 1.4f, 0.9f, 0);
    static final int ARCH_GLOWS = 9;
    static final float ARCH_HALF_SPAN_DEGREES = 22f;
    static final float ARCH_HEIGHT = 1.1f;
    private static final String GLOW_PATH = AtlasSystemArt.GLOW;
    private static final float TRAIL_SIZE_FROM = 0.45f;
    private static final float TRAIL_SIZE_TO = 0.8f;
    private static final float TRAIL_ALPHA = 0.75f;
    private static final float ARCH_GLOW_SIZE = 0.44f;
    private static final float ARCH_FOOT_GLOW_SIZE = 0.6f;

    private StarFlareGlow() {
    }

    static void drawRising(SpriteCache sprites, float starX, float starY, float starRadius, double angleRadians, float progress, Rise rise,
                           Color colour, float alpha) {
        drawRising(sprites, starX, starY, starRadius, angleRadians, 1f, progress, rise, colour, alpha);
    }

    static void drawRising(SpriteCache sprites, float starX, float starY, float starRadius, double angleRadians, float flattening, float progress,
                           Rise rise, Color colour, float alpha) {
        float headAlpha = (1f - progress) * rise.peakAlpha() * alpha;
        if (headAlpha <= 0f) {
            return;
        }
        float cos = (float) Math.cos(angleRadians);
        float sin = (float) Math.sin(angleRadians) * flattening;
        float headDistance = starRadius * (1f + rise.lift() * progress);
        float headSize = starRadius * (rise.startSize() + rise.growth() * progress);
        for (int trail = 1; trail <= rise.trailGlows(); trail++) {
            float share = trail / (float) (rise.trailGlows() + 1);
            float distance = starRadius + (headDistance - starRadius) * share;
            float size = headSize * (TRAIL_SIZE_FROM + (TRAIL_SIZE_TO - TRAIL_SIZE_FROM) * share);
            SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_PATH, starX + cos * distance, starY + sin * distance, size, size, colour,
                    headAlpha * TRAIL_ALPHA);
        }
        SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_PATH, starX + cos * headDistance, starY + sin * headDistance, headSize, headSize, colour,
                headAlpha);
    }

    static void drawArch(SpriteCache sprites, float starX, float starY, float starRadius, double angleRadians, float progress, Color colour,
                         float alpha) {
        float archAlpha = archAlpha(progress) * alpha;
        if (archAlpha <= 0f) {
            return;
        }
        float height = archHeight(progress);
        double halfSpan = Math.toRadians(ARCH_HALF_SPAN_DEGREES);
        for (int glow = 0; glow < ARCH_GLOWS; glow++) {
            float along = glow / (float) (ARCH_GLOWS - 1);
            double angle = angleRadians + (along * 2f - 1f) * halfSpan;
            float distance = starRadius * (1f + height * archProfile(along));
            boolean foot = glow == 0 || glow == ARCH_GLOWS - 1;
            float size = starRadius * (foot ? ARCH_FOOT_GLOW_SIZE : ARCH_GLOW_SIZE);
            SpriteDraw.drawAdditiveAtCenter(sprites, GLOW_PATH, starX + (float) Math.cos(angle) * distance, starY + (float) Math.sin(angle) * distance,
                    size, size, colour, archAlpha);
        }
    }

    static float archHeight(float progress) {
        float remaining = 1f - progress;
        return ARCH_HEIGHT * (1f - remaining * remaining);
    }

    static float archAlpha(float progress) {
        return progress <= 0f || progress >= 1f ? 0f : (float) Math.sin(Math.PI * progress);
    }

    static float archProfile(float along) {
        return (float) Math.sin(Math.PI * along);
    }
}
