package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.GlScope;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class BeamStroke {

    public record Style(String fringeTexture, String coreTexture, float fringeWidth, float coreWidth, float tileLengthPerWidth, float scrollSpeed) {
    }

    public record Look(Color fringe, Color core, float fringeAlpha, float coreAlpha) {
    }

    static final float RING_STEP_RADIANS = (float) Math.toRadians(8.0);
    static final float MIN_PIECE = 0.5f;
    static final float TILE_EPSILON = 0.001f;

    private interface Path {

        float length();

        void at(float distance, float[] out);
    }

    private final SpriteCache spriteCache = new SpriteCache(BeamStroke.class);
    private final float[] point = new float[4];
    private final float[] nextPoint = new float[4];
    private final LinePath linePath = new LinePath();
    private final RingPath ringPath = new RingPath();

    public void line(float fromX, float fromY, float toX, float toY, Style style, Look look, float startAlpha, float endAlpha, float elapsedSeconds,
                     float widthScale) {
        linePath.set(fromX, fromY, toX, toY);
        if (linePath.length() <= MIN_PIECE) {
            return;
        }
        stroke(linePath, style, look, startAlpha, endAlpha, elapsedSeconds, widthScale, 0f);
    }

    public void ring(float cx, float cy, float radius, Style style, Look look, float alpha, float elapsedSeconds, float widthScale) {
        if (radius <= MIN_PIECE) {
            return;
        }
        ringPath.set(cx, cy, radius);
        stroke(ringPath, style, look, alpha, alpha, elapsedSeconds, widthScale, RING_STEP_RADIANS * radius);
    }

    private void stroke(Path path, Style style, Look look, float startAlpha, float endAlpha, float elapsedSeconds, float widthScale, float maxPiece) {
        if (look.fringeAlpha() > 0f && look.fringe() != null) {
            layer(path, style.fringeTexture(), style.fringeWidth() * widthScale, style, look.fringe(), look.fringeAlpha(), startAlpha, endAlpha,
                    elapsedSeconds, widthScale, maxPiece);
        }
        if (look.coreAlpha() > 0f && look.core() != null) {
            layer(path, style.coreTexture(), style.coreWidth() * widthScale, style, look.core(), look.coreAlpha(), startAlpha, endAlpha,
                    elapsedSeconds, widthScale, maxPiece);
        }
    }

    private void layer(Path path, String texturePath, float width, Style style, Color colour, float layerAlpha, float startAlpha, float endAlpha,
                       float elapsedSeconds, float widthScale, float maxPiece) {
        SpriteAPI texture = spriteCache.texture(texturePath);
        if (texture == null || width <= 0f) {
            return;
        }
        float tile = Math.max(MIN_PIECE, style.fringeWidth() * widthScale * style.tileLengthPerWidth());
        float scroll = positiveModulo(elapsedSeconds * style.scrollSpeed() * widthScale, tile);
        float length = path.length();
        float half = width / 2f;
        float textureWidth = texture.getTextureWidth();
        float textureHeight = texture.getTextureHeight();
        float red = colour.getRed() / 255f;
        float green = colour.getGreen() / 255f;
        float blue = colour.getBlue() / 255f;
        try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
            texture.bindTexture();
            GL11.glBegin(GL11.GL_QUADS);
            float start = 0f;
            while (start < length) {
                float tileStart = tileStart(start, tile, scroll);
                float end = pieceEnd(start, tileStart, length, tile, maxPiece);
                float startU = (start - tileStart) / tile * textureWidth;
                float endU = (end - tileStart) / tile * textureWidth;
                path.at(start, point);
                path.at(end, nextPoint);
                float startShade = layerAlpha * lerp(startAlpha, endAlpha, start / length);
                float endShade = layerAlpha * lerp(startAlpha, endAlpha, end / length);
                vertex(point, half, startU, 0f, red, green, blue, startShade);
                vertex(nextPoint, half, endU, 0f, red, green, blue, endShade);
                vertex(nextPoint, -half, endU, textureHeight, red, green, blue, endShade);
                vertex(point, -half, startU, textureHeight, red, green, blue, startShade);
                start = end;
            }
            GL11.glEnd();
        }
    }

    private static void vertex(float[] at, float offset, float u, float v, float red, float green, float blue, float alpha) {
        GL11.glColor4f(red, green, blue, alpha);
        GL11.glTexCoord2f(u, v);
        GL11.glVertex2f(at[0] + at[2] * offset, at[1] + at[3] * offset);
    }

    static float tileStart(float start, float tile, float scroll) {
        float intoTile = positiveModulo(start + scroll, tile);
        return tile - intoTile < TILE_EPSILON ? start : start - intoTile;
    }

    static float pieceEnd(float start, float tileStart, float length, float tile, float maxPiece) {
        float end = Math.min(length, tileStart + tile);
        return maxPiece > 0f ? Math.min(end, start + maxPiece) : end;
    }

    static float positiveModulo(float value, float modulus) {
        float result = value % modulus;
        return result < 0f ? result + modulus : result;
    }

    private static float lerp(float from, float to, float fraction) {
        return from + (to - from) * fraction;
    }

    private static final class LinePath implements Path {
        private float fromX;
        private float fromY;
        private float dirX;
        private float dirY;
        private float length;

        void set(float x1, float y1, float x2, float y2) {
            float dx = x2 - x1;
            float dy = y2 - y1;
            length = (float) Math.sqrt(dx * dx + dy * dy);
            fromX = x1;
            fromY = y1;
            dirX = length > 0f ? dx / length : 1f;
            dirY = length > 0f ? dy / length : 0f;
        }

        @Override
        public float length() {
            return length;
        }

        @Override
        public void at(float distance, float[] out) {
            out[0] = fromX + dirX * distance;
            out[1] = fromY + dirY * distance;
            out[2] = -dirY;
            out[3] = dirX;
        }
    }

    private static final class RingPath implements Path {
        private float cx;
        private float cy;
        private float radius;

        void set(float centreX, float centreY, float ringRadius) {
            cx = centreX;
            cy = centreY;
            radius = ringRadius;
        }

        @Override
        public float length() {
            return (float) (2.0 * Math.PI * radius);
        }

        @Override
        public void at(float distance, float[] out) {
            double angle = distance / radius;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            out[0] = cx + cos * radius;
            out[1] = cy + sin * radius;
            out[2] = cos;
            out[3] = sin;
        }
    }
}
