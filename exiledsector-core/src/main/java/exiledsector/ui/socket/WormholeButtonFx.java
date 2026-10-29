package exiledsector.ui.socket;

import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class WormholeButtonFx {

    static final String CORONA_TEXTURE = "graphics/fx/wormhole_corona.png";
    static final Color VIOLET = new Color(170, 110, 255);
    static final Color CYAN = new Color(110, 210, 255);
    static final Color TEXT_COLOR = new Color(215, 195, 255);
    static final Color BACKGROUND_COLOR = new Color(40, 22, 70);
    static final float GLOW_REACH = 18f;
    static final float GLOW_ALPHA = 0.5f;
    static final float MOUTH_SIZE_SHARE = 2.4f;
    static final float MOUTH_SPIN_DEGREES = 40f;
    static final float MOUTH_ALPHA = 0.8f;
    static final float IDLE_ALPHA = 0.5f;
    static final float CORNER_CUT = 7f;
    static final float OUTSET_CUT_SHARE = 0.4142f;
    static final int OUTLINE_POINTS = 8;
    static final float PULSE_SPEED = 2.4f;
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
        float pulse = (float) Math.sin(elapsedSeconds * PULSE_SPEED) * 0.5f + 0.5f;
        float centreY = bottom + height / 2f;
        float mouthSize = height * MOUTH_SIZE_SHARE * (0.92f + 0.08f * pulse);
        float spin = elapsedSeconds * MOUTH_SPIN_DEGREES;
        SpriteDraw.drawAdditiveAtCenter(SPRITES, CORONA_TEXTURE, left, centreY, mouthSize, mouthSize, VIOLET, MOUTH_ALPHA * strength, spin);
        SpriteDraw.drawAdditiveAtCenter(SPRITES, CORONA_TEXTURE, left + width, centreY, mouthSize, mouthSize, CYAN, MOUTH_ALPHA * strength, -spin);
        try (GlScope glow = GlScope.flat().additive()) {
            outerGlow(left, bottom, width, height, GLOW_REACH, GLOW_ALPHA * (0.75f + 0.25f * pulse) * strength);
        }
    }

    private static void outerGlow(float left, float bottom, float width, float height, float reach, float alpha) {
        float cut = Math.min(CORNER_CUT, Math.min(width, height) / 2f);
        float[] inner = chamferedOutline(left, bottom, width, height, cut);
        float[] outer = chamferedOutline(left - reach, bottom - reach, width + reach * 2f, height + reach * 2f, cut + reach * OUTSET_CUT_SHARE);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int corner = 0; corner <= OUTLINE_POINTS; corner++) {
            int index = (corner % OUTLINE_POINTS) * 2;
            vertex(inner[index], inner[index + 1], left, width, alpha);
            vertex(outer[index], outer[index + 1], left, width, 0f);
        }
        GL11.glEnd();
    }

    static float[] chamferedOutline(float left, float bottom, float width, float height, float cut) {
        float right = left + width;
        float top = bottom + height;
        return new float[]{left + cut, bottom, right - cut, bottom, right, bottom + cut, right, top - cut,
                right - cut, top, left + cut, top, left, top - cut, left, bottom + cut};
    }

    private static void vertex(float x, float y, float left, float width, float alpha) {
        float share = Math.max(0f, Math.min(1f, (x - left) / width));
        Misc.setColor(Misc.interpolateColor(VIOLET, CYAN, share), alpha);
        GL11.glVertex2f(x, y);
    }
}
