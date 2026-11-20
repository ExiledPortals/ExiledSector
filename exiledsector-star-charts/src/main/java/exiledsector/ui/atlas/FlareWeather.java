package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasBodyArt.Flare;
import exiledsector.ui.atlas.AtlasBodyPainter.Lighting;
import exiledsector.ui.atlas.StarFlareGlow.Rise;
import exiledsector.ui.decoration.ConvoyCrossing;

import java.awt.Color;

final class FlareWeather {

    record Style(int streams, float slotSeconds, float chance, float burstShare, Color colour, Rise rise) {

        boolean arched() {
            return rise == null;
        }

        Color colourOr(Color anomalyColour) {
            return colour == null ? anomalyColour : colour;
        }
    }

    static final float GOLDEN_ANGLE_DEGREES = 137.5f;
    static final float LICK_SPREAD_DEGREES = 35f;
    private static final int STREAM_SALT = 0x2f6b1d;
    private static final int ANGLE_SALT = 6;
    private static final float LICK_PAST_LANE = 0.15f;

    static final Style PLAIN = new Style(1, 2.8f, 0.45f, 0.6f, null, new Rise(0.9f, 0.22f, 0.38f, 0.85f, 2));
    static final Style OFTEN = new Style(3, 1.5f, 0.75f, 0.75f, new Color(190, 220, 255), new Rise(0.85f, 0.18f, 0.32f, 0.9f, 2));
    static final Style WARM = new Style(2, 2.6f, 0.6f, 0.85f, new Color(255, 185, 85), new Rise(0.75f, 0.3f, 0.5f, 0.8f, 3));
    static final Style GENTLE = new Style(1, 5f, 0.65f, 0.9f, new Color(255, 125, 85), new Rise(0.45f, 0.28f, 0.25f, 0.55f, 2));
    static final Style PERFECT = new Style(1, 6.5f, 0.4f, 0.8f, new Color(255, 252, 245), null);
    static final Style LICKING = new Style(2, 2f, 0.65f, 0.7f, new Color(255, 200, 120),
            new Rise(ConvoyCrossing.LANE_SHARE - 1f + LICK_PAST_LANE, 0.2f, 0.35f, 0.9f, 3));
    static final Style SURGE = new Style(2, 2.4f, 0.55f, 0.75f, new Color(255, 150, 110), new Rise(0.8f, 0.24f, 0.4f, 0.85f, 2));

    private FlareWeather() {
    }

    static Style styleOf(Flare flare) {
        return switch (flare) {
            case OFTEN -> OFTEN;
            case WARM -> WARM;
            case GENTLE -> GENTLE;
            case PERFECT -> PERFECT;
            case LICKING -> LICKING;
            case SURGE -> SURGE;
            case NONE, PLAIN -> PLAIN;
        };
    }

    static float glowShare(Lighting lighting) {
        return switch (lighting) {
            case LIT -> 1f;
            case AVAILABLE -> 0.6f;
            case DULL -> 0.35f;
        };
    }

    static int streamSeed(int seed, int stream) {
        return seed + stream * STREAM_SALT;
    }

    static float progress(int streamSeed, float flareSeconds, Style style) {
        return StormWeather.burstProgress(streamSeed, flareSeconds, style.slotSeconds(), style.chance(), style.burstShare());
    }

    static int slotOf(int streamSeed, float flareSeconds, Style style) {
        return StormWeather.slotOf(streamSeed, flareSeconds, style.slotSeconds());
    }

    static float angleDegrees(Flare flare, int streamSeed, int slot, float headingDegrees) {
        return switch (flare) {
            case PERFECT -> streamSeed + slot * GOLDEN_ANGLE_DEGREES;
            case LICKING -> headingDegrees + ConvoyCrossing.LANE_SIDE_DEGREES
                    + StormWeather.signedUnit(streamSeed, slot, ANGLE_SALT) * LICK_SPREAD_DEGREES;
            default -> StormWeather.unit(streamSeed, slot, ANGLE_SALT) * 360f;
        };
    }

    static float shimmer(int seed, float flareSeconds) {
        return 0.5f + 0.25f * (float) Math.sin(flareSeconds * 1.3f + seed) + 0.25f * (float) Math.sin(flareSeconds * 0.47f + seed * 2f);
    }
}
