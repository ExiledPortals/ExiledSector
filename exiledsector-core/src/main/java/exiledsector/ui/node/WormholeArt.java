package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.belt.RadialBand;
import exiledsector.ui.belt.WormholeBandRenderer;
import exiledsector.ui.util.TexturedQuadBatch;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public final class WormholeArt {

    public static final float MIN_SCALE = 0.25f;

    private static final String CORONA_TEXTURE_PATH = "graphics/fx/wormhole_corona.png";
    private static final float CORONA_SIZE_RATIO = 2.2f;
    private static final int CORONA_COUNT = 6;
    private static final float CORONA_ORBIT_RATIO = 0.12f;
    private static final float CORONA_ROTATION_SPEED_DEG = 12f;
    private static final float CORONA_PULSE_SPEED_DEG = 90f;
    private static final float CORONA_PULSE_SIZE_RATIO = 0.1f;

    private static final float RING_OUTER_RADIUS_RATIO = 1.55f;
    private static final int RING_COUNT = 16;
    private static final float RING_RADIUS_DECAY = 0.94f;

    private static final String BAND_TEXTURE_PATH = "graphics/fx/portal_textures_small.png";
    private static final float BAND_INNER_RADIUS_RATIO = 0.45f;
    private static final float BAND_THICKNESS_RATIO = 0.35f;
    private static final float BAND_ROTATION_SPEED_DEG = 5f;
    private static final float BAND_ALPHA = 0.75f;

    private static final String GLOW_TEXTURE_PATH = "graphics/fx/hit_glow.png";
    private static final float GLOW_SIZE_RATIO = 1.3f;
    private static final float GLOW_ALPHA = 0.67f;

    private final NodeRingArt ringArt;
    private final SpriteCache spriteCache = new SpriteCache(WormholeArt.class);
    private final TexturedQuadBatch quads = new TexturedQuadBatch();

    public WormholeArt(NodeRingArt ringArt) {
        this.ringArt = ringArt;
    }

    public static float easedOpenness(float rawOpenness) {
        return rawOpenness * rawOpenness;
    }

    public static float visualScale(float openness) {
        return MIN_SCALE + (1f - MIN_SCALE) * openness;
    }

    public void draw(float cx, float cy, float footprintSize, float alphaMult, String nodeId, Color wormholeColor, float rawOpenness) {
        float openness = easedOpenness(rawOpenness);
        float baseRadius = footprintSize / 2f * visualScale(openness);

        drawCorona(cx, cy, baseRadius, wormholeColor, alphaMult);
        if (openness < 1f) {
            drawBands(cx, cy, baseRadius, wormholeColor, (1f - openness) * BAND_ALPHA * alphaMult);
        }
        if (openness > 0f) {
            ringArt.drawRingStack(cx, cy, baseRadius * 2f * RING_OUTER_RADIUS_RATIO, nodeId, RING_COUNT, RING_RADIUS_DECAY, wormholeColor,
                    openness * alphaMult);
        }
        if (openness < 1f) {
            drawGlow(cx, cy, baseRadius, wormholeColor, alphaMult, openness);
        }
    }

    public void drawDormant(float cx, float cy, float baseRadius, Color wormholeColor, float alphaMult, float openness) {
        if (openness >= 1f) {
            return;
        }
        float closedness = 1f - openness;
        drawCorona(cx, cy, baseRadius, wormholeColor, closedness * alphaMult);
        drawBands(cx, cy, baseRadius, wormholeColor, closedness * BAND_ALPHA * alphaMult);
        drawGlow(cx, cy, baseRadius, wormholeColor, alphaMult, openness);
    }

    private void drawCorona(float cx, float cy, float baseRadius, Color wormholeColor, float alphaMult) {
        SpriteAPI texture = spriteCache.texture(CORONA_TEXTURE_PATH);
        if (texture == null) return;
        float elapsed = ringArt.elapsedSeconds();
        float size = baseRadius * 2f * CORONA_SIZE_RATIO;
        float orbit = baseRadius * CORONA_ORBIT_RATIO;

        try (TexturedQuadBatch batch = quads.beginAdditive(texture)) {
            for (int i = 0; i < CORONA_COUNT; i++) {
                float baseAngle = 360f * i / CORONA_COUNT;
                float angle = baseAngle + elapsed * CORONA_ROTATION_SPEED_DEG;
                float rad = (float) Math.toRadians(angle);
                float pulse = 1f + (float) Math.sin(Math.toRadians(elapsed * CORONA_PULSE_SPEED_DEG + baseAngle)) * CORONA_PULSE_SIZE_RATIO;

                batch.quadAtCenter(cx + (float) Math.cos(rad) * orbit, cy + (float) Math.sin(rad) * orbit, size * pulse, size * pulse, angle,
                        wormholeColor, alphaMult);
            }
        }
    }

    private void drawBands(float cx, float cy, float baseRadius, Color wormholeColor, float alphaMult) {
        SpriteAPI texture = spriteCache.texture(BAND_TEXTURE_PATH);
        if (texture == null) return;
        float elapsed = ringArt.elapsedSeconds();
        float innerRadius = baseRadius * BAND_INNER_RADIUS_RATIO;
        float outerRadius = innerRadius + baseRadius * BAND_THICKNESS_RATIO;
        float rotationA = (elapsed * BAND_ROTATION_SPEED_DEG) % 360f;
        float rotationB = (-elapsed * BAND_ROTATION_SPEED_DEG) % 360f;

        RadialBand band = new RadialBand(new Vector2f(cx, cy), innerRadius, outerRadius);
        WormholeBandRenderer.render(texture, band, 0, rotationA, wormholeColor, alphaMult, elapsed);
        WormholeBandRenderer.render(texture, band, 1, rotationB, wormholeColor, alphaMult, elapsed);
    }

    private void drawGlow(float cx, float cy, float baseRadius, Color wormholeColor, float alphaMult, float openness) {
        float size = baseRadius * 2f * GLOW_SIZE_RATIO;
        float closedness = 1f - openness;
        float alpha = GLOW_ALPHA * closedness * closedness * closedness * alphaMult;

        SpriteAPI texture = spriteCache.texture(GLOW_TEXTURE_PATH);
        if (texture == null) return;
        try (TexturedQuadBatch batch = quads.beginAdditive(texture)) {
            batch.quadAtCenter(cx, cy, size, size, 0f, wormholeColor, alpha);
            batch.quadAtCenter(cx, cy, size, size, 0f, wormholeColor, alpha);
        }
    }
}
