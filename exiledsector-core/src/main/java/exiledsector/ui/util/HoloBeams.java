package exiledsector.ui.util;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.skills.skilleffect.render.SpriteCache;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class HoloBeams {

    static final String BEAM_TEXTURE = "graphics/fx/beam_laser_core.png";
    static final String RING_TEXTURE = "graphics/fx/shields256ringd.png";
    static final float BEAM_V_FROM = 8f / 32f;
    static final float BEAM_V_TO = 24f / 32f;
    static final float BEAM_BAND_SHARE = 10f / 16f;
    static final float RING_RADIUS_SHARE = 116.5f / 128f;
    static final float RING_LINE_SHARE = 2.6f / 256f;
    static final float RING_LINE_FADE_FROM = 0.6f;
    static final float RING_LINE_FADE_TO = 1.1f;
    static final float FALLBACK_RING_WIDTH = 1f;
    static final float MIN_CORE_WIDTH = 1.5f;
    static final float HALO_WIDTH_SCALE = 2.6f;
    static final float HALO_ALPHA = 0.28f;
    static final float CORE_WHITENING = 0.45f;
    private static final float MAX_ARC_STEP = (float) Math.toRadians(4.0);
    private static final int CIRCLE_STEPS = Math.max(1, (int) Math.ceil(HoloShapes.TWO_PI / MAX_ARC_STEP));
    private static final UnitCircle CIRCLE = UnitCircle.of(CIRCLE_STEPS);
    private static final SpriteCache TEXTURES = new SpriteCache(HoloBeams.class);
    private static final Pen HALO_PEN = new Pen(true);
    private static final Pen CORE_PEN = new Pen(false);
    private static final Pen[] PENS = {HALO_PEN, CORE_PEN};

    static final class Pen {

        private final boolean halo;
        private int red;
        private int green;
        private int blue;
        private int colorAlpha;
        private float alphaMult;
        private float halfWidth;
        private float u;
        private float vFrom;
        private float vTo;

        Pen(boolean halo) {
            this.halo = halo;
        }

        void load(Color color, float width, float alpha, SpriteAPI texture) {
            float coreWidth = Math.max(MIN_CORE_WIDTH, width);
            float thinning = Math.min(1f, width / MIN_CORE_WIDTH);
            red = halo ? color.getRed() : whitened(color.getRed());
            green = halo ? color.getGreen() : whitened(color.getGreen());
            blue = halo ? color.getBlue() : whitened(color.getBlue());
            colorAlpha = halo ? color.getAlpha() : whitened(color.getAlpha());
            alphaMult = alpha * thinning * (halo ? HALO_ALPHA : 1f);
            float bandWidth = halo ? coreWidth * HALO_WIDTH_SCALE : coreWidth;
            halfWidth = bandWidth / BEAM_BAND_SHARE / 2f;
            u = texture.getTextureWidth() / 2f;
            vFrom = BEAM_V_FROM * texture.getTextureHeight();
            vTo = BEAM_V_TO * texture.getTextureHeight();
        }

        static int whitened(int channel) {
            return Math.round(channel + (255f - channel) * CORE_WHITENING);
        }

        int red() {
            return red;
        }

        int green() {
            return green;
        }

        int blue() {
            return blue;
        }

        int glAlpha() {
            return (int) (colorAlpha * alphaMult);
        }

        float halfWidth() {
            return halfWidth;
        }

        void apply() {
            GL11.glColor4ub((byte) red, (byte) green, (byte) blue, (byte) glAlpha());
        }
    }

    private HoloBeams() {
    }

    public static void ring(float cx, float cy, float radius, float rotationDeg, Color color, float alpha) {
        if (radius <= 0f || alpha <= 0f) {
            return;
        }
        float half = radius / RING_RADIUS_SHARE;
        float artShare = ringArtShare(half * 2f * RING_LINE_SHARE);
        circle(cx, cy, radius, FALLBACK_RING_WIDTH, color, alpha * (1f - artShare));
        SpriteAPI texture = TEXTURES.texture(RING_TEXTURE);
        if (texture == null || artShare <= 0f) {
            return;
        }
        float angle = (float) Math.toRadians(rotationDeg);
        float cos = (float) Math.cos(angle) * half;
        float sin = (float) Math.sin(angle) * half;
        float textureWidth = texture.getTextureWidth();
        float textureHeight = texture.getTextureHeight();
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
            texture.bindTexture();
            Misc.setColor(color, alpha * artShare);
            GL11.glBegin(GL11.GL_QUADS);
            corner(cx - cos + sin, cy - sin - cos, 0f, 0f);
            corner(cx + cos + sin, cy + sin - cos, textureWidth, 0f);
            corner(cx + cos - sin, cy + sin + cos, textureWidth, textureHeight);
            corner(cx - cos - sin, cy - sin + cos, 0f, textureHeight);
            GL11.glEnd();
        }
    }

    static float ringArtShare(float ringLineWidth) {
        return Math.max(0f, Math.min(1f, (ringLineWidth - RING_LINE_FADE_FROM) / (RING_LINE_FADE_TO - RING_LINE_FADE_FROM)));
    }

    public static void circle(float cx, float cy, float radius, float width, Color color, float alpha) {
        SpriteAPI texture = beamTexture(width, alpha);
        if (texture == null) {
            return;
        }
        try (GlScope scope = beginStroke(texture, color, width, alpha)) {
            for (int step = 0; step < CIRCLE_STEPS; step++) {
                int next = (step + 1) % CIRCLE_STEPS;
                arcPiece(cx, cy, radius, CIRCLE.cos(step), CIRCLE.sin(step), CIRCLE.cos(next), CIRCLE.sin(next));
            }
            GL11.glEnd();
        }
    }

    public static void arcSegments(float cx, float cy, float radius, int segments, float fill, float rotationDeg, float width, Color color,
                                   float alpha) {
        SpriteAPI texture = beamTexture(width, alpha);
        if (texture == null) {
            return;
        }
        float slot = HoloShapes.TWO_PI / segments;
        float rotation = (float) Math.toRadians(rotationDeg);
        float sweep = slot * fill;
        int steps = Math.max(1, (int) Math.ceil(sweep / MAX_ARC_STEP));
        try (GlScope scope = beginStroke(texture, color, width, alpha)) {
            for (int i = 0; i < segments; i++) {
                float start = rotation + i * slot;
                float fromCos = (float) Math.cos(start);
                float fromSin = (float) Math.sin(start);
                for (int step = 0; step < steps; step++) {
                    float to = start + sweep * (step + 1) / steps;
                    float toCos = (float) Math.cos(to);
                    float toSin = (float) Math.sin(to);
                    arcPiece(cx, cy, radius, fromCos, fromSin, toCos, toSin);
                    fromCos = toCos;
                    fromSin = toSin;
                }
            }
            GL11.glEnd();
        }
    }

    public static void ticks(float cx, float cy, float radius, int count, int majorEvery, float majorLength, float minorLength, float rotationDeg,
                             float width, Color color, float alpha) {
        SpriteAPI texture = beamTexture(width, alpha);
        if (texture == null) {
            return;
        }
        float rotation = (float) Math.toRadians(rotationDeg);
        try (GlScope scope = beginStroke(texture, color, width, alpha)) {
            for (int i = 0; i < count; i++) {
                float angle = rotation + HoloShapes.TWO_PI * i / count;
                float length = i % majorEvery == 0 ? majorLength : minorLength;
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                segment(cx + cos * radius, cy + sin * radius, cx + cos * (radius - length), cy + sin * (radius - length));
            }
            GL11.glEnd();
        }
    }

    public static void line(float fromX, float fromY, float toX, float toY, float width, Color color, float alpha) {
        SpriteAPI texture = beamTexture(width, alpha);
        if (texture == null) {
            return;
        }
        try (GlScope scope = beginStroke(texture, color, width, alpha)) {
            segment(fromX, fromY, toX, toY);
            GL11.glEnd();
        }
    }

    public static void square(float cx, float cy, float half, float width, Color color, float alpha) {
        SpriteAPI texture = beamTexture(width, alpha);
        if (texture == null) {
            return;
        }
        try (GlScope scope = beginStroke(texture, color, width, alpha)) {
            for (Pen pen : PENS) {
                float reach = half + pen.halfWidth;
                float side = half - pen.halfWidth;
                segment(pen, cx - reach, cy - half, cx + reach, cy - half);
                segment(pen, cx - reach, cy + half, cx + reach, cy + half);
                segment(pen, cx - half, cy - side, cx - half, cy + side);
                segment(pen, cx + half, cy - side, cx + half, cy + side);
            }
            GL11.glEnd();
        }
    }

    public static void brackets(float cx, float cy, float half, float arm, float width, Color color, float alpha) {
        SpriteAPI texture = beamTexture(width, alpha);
        if (texture == null) {
            return;
        }
        try (GlScope scope = beginStroke(texture, color, width, alpha)) {
            for (Pen pen : PENS) {
                for (int corner = 0; corner < 4; corner++) {
                    float sx = corner % 2 == 0 ? -1f : 1f;
                    float sy = corner < 2 ? -1f : 1f;
                    float x = cx + sx * half;
                    float y = cy + sy * half;
                    segment(pen, x + sx * pen.halfWidth, y, x - sx * arm, y);
                    segment(pen, x, y - sy * pen.halfWidth, x, y - sy * arm);
                }
            }
            GL11.glEnd();
        }
    }

    private static SpriteAPI beamTexture(float width, float alpha) {
        SpriteAPI texture = TEXTURES.texture(BEAM_TEXTURE);
        return texture == null || alpha <= 0f || width <= 0f ? null : texture;
    }

    private static GlScope beginStroke(SpriteAPI texture, Color color, float width, float alpha) {
        GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        texture.bindTexture();
        GL11.glBegin(GL11.GL_QUADS);
        HALO_PEN.load(color, width, alpha, texture);
        CORE_PEN.load(color, width, alpha, texture);
        return scope;
    }

    private static void arcPiece(float cx, float cy, float radius, float fromCos, float fromSin, float toCos, float toSin) {
        for (Pen pen : PENS) {
            float outer = radius + pen.halfWidth;
            float inner = radius - pen.halfWidth;
            pen.apply();
            corner(cx + fromCos * outer, cy + fromSin * outer, pen.u, pen.vFrom);
            corner(cx + toCos * outer, cy + toSin * outer, pen.u, pen.vFrom);
            corner(cx + toCos * inner, cy + toSin * inner, pen.u, pen.vTo);
            corner(cx + fromCos * inner, cy + fromSin * inner, pen.u, pen.vTo);
        }
    }

    private static void segment(float fromX, float fromY, float toX, float toY) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0f) {
            return;
        }
        float unitNormalX = -dy / length;
        float unitNormalY = dx / length;
        for (Pen pen : PENS) {
            quad(pen, fromX, fromY, toX, toY, unitNormalX * pen.halfWidth, unitNormalY * pen.halfWidth);
        }
    }

    private static void segment(Pen pen, float fromX, float fromY, float toX, float toY) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0f) {
            return;
        }
        quad(pen, fromX, fromY, toX, toY, -dy / length * pen.halfWidth, dx / length * pen.halfWidth);
    }

    private static void quad(Pen pen, float fromX, float fromY, float toX, float toY, float normalX, float normalY) {
        pen.apply();
        corner(fromX + normalX, fromY + normalY, pen.u, pen.vFrom);
        corner(toX + normalX, toY + normalY, pen.u, pen.vFrom);
        corner(toX - normalX, toY - normalY, pen.u, pen.vTo);
        corner(fromX - normalX, fromY - normalY, pen.u, pen.vTo);
    }

    private static void corner(float x, float y, float u, float v) {
        GL11.glTexCoord2f(u, v);
        GL11.glVertex2f(x, y);
    }
}
