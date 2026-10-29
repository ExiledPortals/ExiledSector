package exiledsector.ui.socket;

import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class WormholeButtonFx {

    static final String CORONA_TEXTURE = "graphics/fx/wormhole_corona.png";
    static final Color VIOLET = new Color(170, 110, 255);
    static final Color CYAN = new Color(110, 210, 255);
    static final float GLOW_REACH = 14f;
    static final float GLOW_ALPHA = 0.45f;
    static final float BORDER_ALPHA = 0.8f;
    static final int MOTES = 10;
    static final float MOTE_LAPS_PER_SECOND = 0.06f;
    static final float MOTE_SPIN_DEGREES = 70f;
    static final float MOTE_SIZE_SHARE = 0.9f;
    static final float MOTE_ALPHA = 0.55f;
    static final float IDLE_ALPHA = 0.35f;
    private static final SpriteCache SPRITES = new SpriteCache(WormholeButtonFx.class);

    private float elapsedSeconds;

    void advance(float amount) {
        elapsedSeconds += amount;
    }

    void render(float left, float bottom, float width, float height, boolean active, float alpha) {
        if (alpha <= 0f || width <= 0f || height <= 0f) {
            return;
        }
        float strength = (active ? 1f : IDLE_ALPHA) * alpha;
        float pulse = (float) Math.sin(elapsedSeconds * 2.4f) * 0.5f + 0.5f;
        Color edge = Misc.interpolateColor(VIOLET, CYAN, pulse);
        try (GlScope glow = GlScope.flat().additive()) {
            outerGlow(left, bottom, width, height, GLOW_REACH, edge, GLOW_ALPHA * (0.7f + 0.3f * pulse) * strength);
        }
        for (int mote = 0; mote < MOTES; mote++) {
            float travel = (mote / (float) MOTES + elapsedSeconds * MOTE_LAPS_PER_SECOND) % 1f;
            float[] point = perimeterPoint(left, bottom, width, height, travel);
            float flicker = (float) Math.sin(elapsedSeconds * 3f + mote * 1.7f) * 0.3f + 0.7f;
            float size = height * MOTE_SIZE_SHARE * (0.8f + 0.4f * flicker);
            SpriteDraw.drawAdditiveAtCenter(SPRITES, CORONA_TEXTURE, point[0], point[1], size, size, mote % 2 == 0 ? VIOLET : CYAN,
                    MOTE_ALPHA * flicker * strength, elapsedSeconds * MOTE_SPIN_DEGREES + mote * 37f);
        }
        GLDraw.strokeQuad(left, bottom, width, height, edge, 1.5f, BORDER_ALPHA * strength);
    }

    static float[] perimeterPoint(float left, float bottom, float width, float height, float travel) {
        float distance = travel * 2f * (width + height);
        if (distance < width) {
            return new float[]{left + distance, bottom};
        }
        distance -= width;
        if (distance < height) {
            return new float[]{left + width, bottom + distance};
        }
        distance -= height;
        if (distance < width) {
            return new float[]{left + width - distance, bottom + height};
        }
        distance -= width;
        return new float[]{left, bottom + height - Math.min(height, distance)};
    }

    private static void outerGlow(float left, float bottom, float width, float height, float reach, Color colour, float alpha) {
        float right = left + width;
        float top = bottom + height;
        GL11.glBegin(GL11.GL_QUADS);
        band(colour, alpha, left, bottom, right, bottom, right + reach, bottom - reach, left - reach, bottom - reach);
        band(colour, alpha, right, bottom, right, top, right + reach, top + reach, right + reach, bottom - reach);
        band(colour, alpha, right, top, left, top, left - reach, top + reach, right + reach, top + reach);
        band(colour, alpha, left, top, left, bottom, left - reach, bottom - reach, left - reach, top + reach);
        GL11.glEnd();
    }

    private static void band(Color colour, float alpha, float innerAX, float innerAY, float innerBX, float innerBY, float outerBX, float outerBY,
                             float outerAX, float outerAY) {
        Misc.setColor(colour, alpha);
        GL11.glVertex2f(innerAX, innerAY);
        GL11.glVertex2f(innerBX, innerBY);
        Misc.setColor(colour, 0f);
        GL11.glVertex2f(outerBX, outerBY);
        GL11.glVertex2f(outerAX, outerAY);
    }
}
