package exiledsector.ui.cargo;

import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;

public final class AnimatedItemIcons {

    public static final String BEACON_PATH = "graphics/stations/buoy00.png";
    static final String BEACON_GLOW_PATH = "graphics/stations/buoy00glow.png";
    static final float BEACON_SIZE_SHARE = 1.6f;
    private static final Color BEACON_GLOW_COLOR = new Color(247, 121, 86);
    private static final float GLOW_CYCLES_PER_SECOND = 1f;
    private static final float GLOW_WOBBLE_DEGREES = 12f;
    private static final float STEADY_GLOW = 0.6f;

    private AnimatedItemIcons() {
    }

    public static boolean isAnimated(String path) {
        return BEACON_PATH.equals(path);
    }

    public static void drawAtCenter(SpriteCache cache, String path, float cx, float cy, float width, float height, Color tint,
                                    float alphaMult) {
        if (!isAnimated(path)) {
            SpriteDraw.drawAtCenter(cache, path, cx, cy, width, height, tint, alphaMult);
            return;
        }
        float beaconWidth = width * BEACON_SIZE_SHARE;
        float beaconHeight = height * BEACON_SIZE_SHARE;
        SpriteDraw.drawAtCenter(cache, BEACON_PATH, cx, cy, beaconWidth, beaconHeight, tint, alphaMult);
        drawBeaconGlow(cache, cx, cy, beaconWidth, beaconHeight, alphaMult * brightness(tint));
    }

    private static void drawBeaconGlow(SpriteCache cache, float cx, float cy, float width, float height, float alphaMult) {
        if (ItemAnimationSetting.mode() != ItemAnimationSetting.Mode.FULL) {
            SpriteDraw.drawAdditiveAtCenter(cache, BEACON_GLOW_PATH, cx, cy, width, height, BEACON_GLOW_COLOR, alphaMult * STEADY_GLOW);
            return;
        }
        float glowPhase = (CargoIconFx.seconds() * GLOW_CYCLES_PER_SECOND) % 1f;
        float pulse = pulse(glowPhase);
        float firstWobble = ((glowPhase * 1.3f) % 1f - 0.5f) * GLOW_WOBBLE_DEGREES;
        float secondWobble = ((glowPhase * 1.9f) % 1f - 0.5f) * GLOW_WOBBLE_DEGREES;
        SpriteDraw.drawAdditiveAtCenter(cache, BEACON_GLOW_PATH, cx, cy, width, height, BEACON_GLOW_COLOR, alphaMult * pulse, firstWobble);
        SpriteDraw.drawAdditiveAtCenter(cache, BEACON_GLOW_PATH, cx, cy, width, height, BEACON_GLOW_COLOR, alphaMult * pulse * 0.5f,
                secondWobble);
    }

    static float pulse(float glowPhase) {
        return glowPhase < 0.5f ? glowPhase * 2f : 1f - (glowPhase - 0.5f) * 2f;
    }

    static float brightness(Color tint) {
        if (tint == null) {
            return 1f;
        }
        return Math.max(tint.getRed(), Math.max(tint.getGreen(), tint.getBlue())) / 255f;
    }
}
