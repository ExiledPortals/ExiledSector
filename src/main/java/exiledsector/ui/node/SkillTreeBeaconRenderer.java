package exiledsector.ui.node;

import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;

final class SkillTreeBeaconRenderer {

    static final String BEACON_PATH = "graphics/stations/buoy00.png";
    static final String BEACON_GLOW_PATH = "graphics/stations/buoy00glow.png";
    static final Color GLOW_COLOR = new Color(247, 121, 86);

    private static final float SIZE_PER_FOOTPRINT = 1.9f;
    private static final float GLOW_CYCLES_PER_SECOND = 1f;
    private static final float GLOW_WOBBLE_DEGREES = 12f;
    private static final float DORMANT_GLOW = 0.25f;
    private static final float AVAILABLE_GLOW = 0.6f;
    private static final Color DORMANT_TINT = new Color(150, 150, 150);

    private final SpriteCache spriteCache = new SpriteCache(SkillTreeBeaconRenderer.class);
    private float glowPhase;

    void advance(float amount) {
        glowPhase = (glowPhase + amount * GLOW_CYCLES_PER_SECOND) % 1f;
    }

    void draw(float centreX, float centreY, float footprintSize, boolean live, boolean available, float alphaMult) {
        float size = footprintSize * SIZE_PER_FOOTPRINT;
        SpriteDraw.drawAtCenter(spriteCache, BEACON_PATH, centreX, centreY, size, size, live ? null : DORMANT_TINT, alphaMult);
        float pulse = glowPhase < 0.5f ? glowPhase * 2f : 1f - (glowPhase - 0.5f) * 2f;
        float strength = live ? 1f : available ? AVAILABLE_GLOW : DORMANT_GLOW;
        float firstWobble = ((glowPhase * 1.3f) % 1f - 0.5f) * GLOW_WOBBLE_DEGREES;
        float secondWobble = ((glowPhase * 1.9f) % 1f - 0.5f) * GLOW_WOBBLE_DEGREES;
        SpriteDraw.drawAdditiveAtCenter(spriteCache, BEACON_GLOW_PATH, centreX, centreY, size, size, GLOW_COLOR,
                alphaMult * strength * pulse, firstWobble);
        SpriteDraw.drawAdditiveAtCenter(spriteCache, BEACON_GLOW_PATH, centreX, centreY, size, size, GLOW_COLOR,
                alphaMult * strength * pulse * 0.5f, secondWobble);
    }
}
