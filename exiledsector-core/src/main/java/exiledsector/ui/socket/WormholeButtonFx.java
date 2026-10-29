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
        float right = left + width;
        float top = bottom + height;
        Color leftColour = VIOLET;
        Color rightColour = CYAN;
        GL11.glBegin(GL11.GL_QUADS);
        band(leftColour, rightColour, alpha, left, bottom, right, bottom, right + reach, bottom - reach, left - reach, bottom - reach);
        band(rightColour, rightColour, alpha, right, bottom, right, top, right + reach, top + reach, right + reach, bottom - reach);
        band(rightColour, leftColour, alpha, right, top, left, top, left - reach, top + reach, right + reach, top + reach);
        band(leftColour, leftColour, alpha, left, top, left, bottom, left - reach, bottom - reach, left - reach, top + reach);
        GL11.glEnd();
    }

    private static void band(Color colourA, Color colourB, float alpha, float innerAX, float innerAY, float innerBX, float innerBY,
                             float outerBX, float outerBY, float outerAX, float outerAY) {
        Misc.setColor(colourA, alpha);
        GL11.glVertex2f(innerAX, innerAY);
        Misc.setColor(colourB, alpha);
        GL11.glVertex2f(innerBX, innerBY);
        Misc.setColor(colourB, 0f);
        GL11.glVertex2f(outerBX, outerBY);
        Misc.setColor(colourA, 0f);
        GL11.glVertex2f(outerAX, outerAY);
    }
}
