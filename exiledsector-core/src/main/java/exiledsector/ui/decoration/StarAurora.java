package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.terrain.AuroraRenderer;
import com.fs.starfarer.api.impl.campaign.terrain.RangeBlockerUtil;
import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GlScope;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public final class StarAurora {

    public enum Side {
        WHOLE, BACK, FRONT
    }

    public static final String TEXTURE_CATEGORY = "terrain";
    public static final String TEXTURE_ID = "aurora";
    static final float INNER_RADIUS_MULT = 1.1f;
    static final float OUTER_RADIUS_MULT = 1.6f;
    static final float MIN_THICKNESS = 10f;
    static final float VERTEX_WOBBLE = 50f * 0.33f;
    static final float SHORTEN_MULT = 0.85f;
    static final int BAND_ALPHA = 25;
    static final float BAND_WIDTH_IN_TEXTURE = 256f;
    static final float SIDE_BLEND_SINE = 0.2f;

    private final Delegate delegate = new Delegate();
    private final AuroraRenderer renderer = new AuroraRenderer(delegate);

    public void advance(float amount) {
        renderer.advance(amount);
    }

    public static float reach(float detailRadius) {
        float inner = detailRadius * INNER_RADIUS_MULT;
        float thickness = Math.max(detailRadius * OUTER_RADIUS_MULT - inner, MIN_THICKNESS);
        return inner + thickness * (1f + SHORTEN_MULT) + VERTEX_WOBBLE;
    }

    public void render(SpriteAPI texture, float screenX, float screenY, float detailRadius, float screenScale, Color coronaColor, float alphaMult) {
        render(texture, screenX, screenY, detailRadius, screenScale, 1f, Side.WHOLE, coronaColor, alphaMult);
    }

    public void render(SpriteAPI texture, float screenX, float screenY, float detailRadius, float screenScale, float flattening, Side side,
                       Color coronaColor, float alphaMult) {
        delegate.centerLoc.set(0f, 0f);
        delegate.innerRadius = detailRadius * INNER_RADIUS_MULT;
        delegate.outerRadius = detailRadius * OUTER_RADIUS_MULT;
        delegate.useCoronaColor(coronaColor);
        delegate.bandTexture = texture;
        delegate.side = side;
        try (GlScope scope = GlScope.save(GlScope.DRAW_ATTRIBS)) {
            GL11.glPushMatrix();
            GL11.glTranslatef(screenX, screenY, 0f);
            GL11.glScalef(screenScale, screenScale * flattening, 1f);
            renderer.render(alphaMult);
            GL11.glPopMatrix();
        }
    }

    static float sideAlpha(Side side, float angleDegrees) {
        if (side == Side.WHOLE) {
            return 1f;
        }
        float sine = (float) Math.sin(Math.toRadians(angleDegrees));
        float backShare = Math.max(0f, Math.min(1f, 0.5f + sine / (2f * SIDE_BLEND_SINE)));
        return side == Side.BACK ? backShare : 1f - backShare;
    }

    private static final class Delegate implements AuroraRenderer.AuroraRendererDelegate {
        private final Vector2f centerLoc = new Vector2f();
        private float innerRadius;
        private float outerRadius;
        private Color coronaColor;
        private Color bandColor = Color.WHITE;
        private SpriteAPI bandTexture;
        private Side side = Side.WHOLE;

        private void useCoronaColor(Color color) {
            if (color != coronaColor) {
                coronaColor = color;
                bandColor = Misc.setAlpha(color, BAND_ALPHA);
            }
        }

        @Override
        public float getAuroraInnerRadius() {
            return innerRadius;
        }

        @Override
        public float getAuroraOuterRadius() {
            return outerRadius;
        }

        @Override
        public Vector2f getAuroraCenterLoc() {
            return centerLoc;
        }

        @Override
        public Color getAuroraColorForAngle(float angle) {
            return bandColor;
        }

        @Override
        public float getAuroraAlphaMultForAngle(float angle) {
            return sideAlpha(side, angle);
        }

        @Override
        public float getAuroraShortenMult(float angle) {
            return SHORTEN_MULT;
        }

        @Override
        public float getAuroraInnerOffsetMult(float angle) {
            return 1f;
        }

        @Override
        public float getAuroraThicknessMult(float angle) {
            return 1f;
        }

        @Override
        public float getAuroraThicknessFlat(float angle) {
            return 0f;
        }

        @Override
        public float getAuroraTexPerSegmentMult() {
            return 1f;
        }

        @Override
        public float getAuroraBandWidthInTexture() {
            return BAND_WIDTH_IN_TEXTURE;
        }

        @Override
        public SpriteAPI getAuroraTexture() {
            return bandTexture;
        }

        @Override
        public RangeBlockerUtil getAuroraBlocker() {
            return null;
        }
    }
}
