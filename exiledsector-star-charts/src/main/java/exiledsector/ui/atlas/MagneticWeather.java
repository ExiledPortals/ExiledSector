package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt.Field;

final class MagneticWeather {

    record Style(int loopsPerSide, float innermostLoopReach, float loopReachStep, float flowSeconds, float auroraInner, float auroraOuter,
                 float auroraAlpha, Surge surge, boolean poleGlows, boolean sparks) {
    }

    enum Surge { NONE, STORM, FLARE }

    static final int DOTS_PER_LOOP = 9;
    static final float PULSE_WIDTH = 0.22f;
    static final float SURGE_AFTERGLOW_SHARE_OF_SLOT = 1f;
    static final float SURGE_LOOP_SWELL = 0.18f;
    static final float SURGE_AURORA_SWELL = 0.3f;
    private static final float FIRST_DOT_ALONG = 0.06f;
    private static final float PULSE_SALT_OFFSET = 0.37f;

    static final Style PLAIN = new Style(2, 1.55f, 0.35f, 3f, 1f, 1.4f, 0.6f, Surge.NONE, false, false);
    static final Style CHARGED = new Style(3, 1.35f, 0.22f, 1.4f, 1f, 1.32f, 0.65f, Surge.NONE, false, true);
    static final Style HARVEST = new Style(2, 1.6f, 0.45f, 4.5f, 1f, 1.55f, 0.75f, Surge.NONE, true, false);
    static final Style ION = new Style(2, 1.5f, 0.4f, 2.5f, 1f, 1.45f, 0.4f, Surge.STORM, false, false);
    static final Style SURGE = new Style(3, 1.45f, 0.3f, 2.2f, 1.05f, 1.6f, 0.5f, Surge.FLARE, true, false);

    private MagneticWeather() {
    }

    static Style styleOf(Field field) {
        return switch (field) {
            case CHARGED -> CHARGED;
            case HARVEST -> HARVEST;
            case ION -> ION;
            case SURGE -> SURGE;
            case NONE, PLAIN -> PLAIN;
        };
    }

    static float loopReach(Style style, int loop) {
        return style.innermostLoopReach() + style.loopReachStep() * loop;
    }

    static float dotAlong(int dot) {
        return FIRST_DOT_ALONG + (1f - 2f * FIRST_DOT_ALONG) * dot / (DOTS_PER_LOOP - 1);
    }

    static float loopAcross(float loopReach, float along) {
        double polar = loopPolarAngle(loopReach, along);
        double sin = Math.sin(polar);
        return (float) (loopReach * sin * sin * sin);
    }

    static float loopAxial(float loopReach, float along) {
        double polar = loopPolarAngle(loopReach, along);
        double sin = Math.sin(polar);
        return (float) (loopReach * sin * sin * Math.cos(polar));
    }

    private static double loopPolarAngle(float loopReach, float along) {
        double foot = Math.asin(Math.sqrt(1.0 / Math.max(1f, loopReach)));
        return foot + along * (Math.PI - 2.0 * foot);
    }

    static float flowGlow(float along, float flowSeconds, float fieldSeconds, int loopSeed) {
        float position = fieldSeconds / Math.max(flowSeconds, 0.01f) + StormWeather.unit(loopSeed, 0, 7) + PULSE_SALT_OFFSET;
        float pulse = position - (float) Math.floor(position);
        float gap = Math.abs(along - pulse) / PULSE_WIDTH;
        if (gap >= 1f) {
            return 0f;
        }
        float glow = 1f - gap;
        return glow * glow;
    }

    static float stormSurge(int seed, float realSeconds, float flashChance) {
        float progress = StormWeather.burstProgress(seed, realSeconds, StormWeather.SCATTERED_FLASH_SECONDS, flashChance,
                SURGE_AFTERGLOW_SHARE_OF_SLOT);
        if (progress < 0f) {
            return 0f;
        }
        float fade = 1f - progress;
        return fade * fade;
    }

    static float flareSurge(int seed, float flareSeconds, FlareWeather.Style flareStyle) {
        float surge = 0f;
        for (int stream = 0; stream < flareStyle.streams(); stream++) {
            float progress = FlareWeather.progress(FlareWeather.streamSeed(seed, stream), flareSeconds, flareStyle);
            if (progress >= 0f) {
                surge = Math.max(surge, StarFlareGlow.archAlpha(progress));
            }
        }
        return surge;
    }

    static float shimmer(int seed, float fieldSeconds) {
        return 0.7f + 0.18f * (float) Math.sin(fieldSeconds * 1.7f + seed) + 0.12f * (float) Math.sin(fieldSeconds * 0.61f + seed * 3f);
    }
}
