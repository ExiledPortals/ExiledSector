package exiledsector.ui.cargo;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.SpriteDraw;
import exiledsector.ui.util.TexturedQuadBatch;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class IconFxKit {

    static final String LIGHT_TEXTURE = "graphics/fx/glow64.png";
    static final float IDLE_WAVE = -10f;
    static final float FLASH_WIDTH = 0.035f;
    static final float FRAME_HALF = 38.25f;
    private static final float DESIGN_SIZE = 80f;
    private static final float DESIGN_CENTRE = 40f;
    private static final float FRAME_OUTER = 39f;
    private static final float FRAME_LINE_INNER = 37.5f;
    private static final float FRAME_GLOW_INNER = 35f;
    private static final int LIGHTS_PER_SIDE = 10;
    private static final float LIGHT_SPACING = 0.2f;
    private static final float LIGHT_WIDTH = 0.13f;
    private static final float LIGHT_IDLE_SIZE = 3f;
    private static final float LIGHT_LIT_SIZE = 6f;
    private static final float FLASH_LIGHTEN = 0.6f;
    private static final TexturedQuadBatch LIGHT_BATCH = new TexturedQuadBatch();

    private IconFxKit() {
    }

    static void drawStill(SpriteCache sprites, String iconPath, Color color, float cx, float cy, float scale, float unit, boolean framed,
                          float alphaMult) {
        SpriteAPI still = iconPath == null ? null : sprites.sprite(iconPath);
        if (still != null) {
            SpriteDraw.drawAtCenter(still, cx, cy, DESIGN_SIZE * scale, DESIGN_SIZE * scale, null, alphaMult, 0f);
        }
        if (framed) {
            drawBorder(sprites, color, cx, cy, unit, IDLE_WAVE, 0f, alphaMult);
        }
    }

    static void drawBorder(SpriteCache sprites, Color color, float cx, float cy, float unit, float wave, float flash, float alphaMult) {
        drawFrame(flash > 0.01f ? CargoIconFx.lighten(color, FLASH_LIGHTEN * flash) : color, cx, cy, unit, alphaMult);
        drawLights(sprites, color, cx, cy, unit, wave, alphaMult);
    }

    static float cyclicDistance(float first, float second) {
        float distance = Math.abs(first - second) % 1f;
        return Math.min(distance, 1f - distance);
    }

    static float flash(float cycle, float key) {
        float offset = cyclicDistance(cycle, key) / FLASH_WIDTH;
        return (float) Math.exp(-offset * offset);
    }

    static float wave(float cycle, float key) {
        float lead = cycle - key;
        lead -= (float) Math.floor(lead + 0.8f);
        return 2f + lead * 2.8f;
    }

    static float lightLevel(float distance, float wave) {
        float offset = (distance - wave) / LIGHT_WIDTH;
        return (float) Math.exp(-offset * offset);
    }

    static float[] framePoint(int path, float distance) {
        float along = distance < 1f ? distance : distance - 1f;
        float edge = -FRAME_HALF + along * 2f * FRAME_HALF;
        if (path == 0) {
            return distance < 1f ? new float[]{edge, -FRAME_HALF} : new float[]{FRAME_HALF, edge};
        }
        return distance < 1f ? new float[]{-FRAME_HALF, edge} : new float[]{edge, FRAME_HALF};
    }

    private static void drawFrame(Color color, float cx, float cy, float unit, float alphaMult) {
        try (GlScope scope = GlScope.flat().additive()) {
            Misc.setColor(color, 0.16f * alphaMult);
            frameBand(cx, cy, FRAME_GLOW_INNER * unit, FRAME_OUTER * unit);
            scope.normalBlend();
            Misc.setColor(color, 0.85f * alphaMult);
            frameBand(cx, cy, FRAME_LINE_INNER * unit, FRAME_OUTER * unit);
        }
    }

    private static void frameBand(float cx, float cy, float inner, float outer) {
        GL11.glBegin(GL11.GL_QUADS);
        quad(cx - outer, cy + inner, cx + outer, cy + outer);
        quad(cx - outer, cy - outer, cx + outer, cy - inner);
        quad(cx - outer, cy - inner, cx - inner, cy + inner);
        quad(cx + inner, cy - inner, cx + outer, cy + inner);
        GL11.glEnd();
    }

    private static void quad(float fromX, float fromY, float toX, float toY) {
        GL11.glVertex2f(fromX, fromY);
        GL11.glVertex2f(toX, fromY);
        GL11.glVertex2f(toX, toY);
        GL11.glVertex2f(fromX, toY);
    }

    private static void drawLights(SpriteCache sprites, Color color, float cx, float cy, float unit, float wave, float alphaMult) {
        SpriteAPI texture = sprites.texture(LIGHT_TEXTURE);
        if (texture == null) {
            return;
        }
        Color tint = CargoIconFx.lighten(color, 0.55f);
        try (TexturedQuadBatch batch = LIGHT_BATCH.beginAdditive(texture)) {
            for (int path = 0; path < 2; path++) {
                for (int light = 0; light < LIGHTS_PER_SIDE; light++) {
                    float distance = LIGHT_SPACING / 2f + light * LIGHT_SPACING;
                    float level = lightLevel(distance, wave);
                    float[] point = framePoint(path, distance);
                    float lightSize = LIGHT_IDLE_SIZE + LIGHT_LIT_SIZE * level;
                    float limit = DESIGN_CENTRE - lightSize / 2f;
                    float lightX = Math.max(-limit, Math.min(limit, point[0]));
                    float lightY = Math.max(-limit, Math.min(limit, point[1]));
                    batch.quadAtCenter(cx + lightX * unit, cy - lightY * unit, lightSize * unit, lightSize * unit, 0f, tint,
                            (0.25f + 0.75f * level) * alphaMult);
                }
            }
        }
    }
}
