package exiledsector.ui.node;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.UnitCircle;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BeamStroke {

    public record Style(String fringeTexture, String coreTexture, float fringeWidth, float coreWidth, float tileLengthPerWidth, float scrollSpeed) {
    }

    public record Helix(float amplitude, float wavelength, float phase, float backShade, float backWidth) {

        public float offset(float distance) {
            return amplitude * (float) Math.sin(angle(distance));
        }

        public float slope(float distance) {
            return amplitude * 2f * (float) Math.PI / wavelength * depth(distance);
        }

        public float depth(float distance) {
            return (float) Math.cos(angle(distance));
        }

        public float shade(float distance) {
            return backShade + (1f - backShade) * (depth(distance) + 1f) / 2f;
        }

        public float width(float distance) {
            return backWidth + (1f - backWidth) * (depth(distance) + 1f) / 2f;
        }

        private double angle(float distance) {
            return 2.0 * Math.PI * distance / wavelength + phase;
        }
    }

    public record Look(Color fringe, Color core, float fringeAlpha, float coreAlpha) {

        public Look fringeOnly() {
            return new Look(fringe, core, fringeAlpha, 0f);
        }

        public Look coreOnly() {
            return new Look(fringe, core, 0f, coreAlpha);
        }
    }

    static final float RING_PIECE_LENGTH = 8f;
    static final int MIN_RING_PIECES = 12;
    static final int MAX_RING_PIECES = 45;
    static final float MIN_PIECE = 0.5f;
    static final float TILE_EPSILON = 0.001f;
    static final float HELIX_PIECES_PER_WAVE = 6f;
    private static final int FLOATS_PER_VERTEX = 8;

    private interface Path {

        float length();

        void at(float distance, float[] out);

        default float shade(float distance) {
            return 1f;
        }
    }

    private final SpriteCache spriteCache = new SpriteCache(BeamStroke.class);
    private final float[] point = new float[4];
    private final float[] nextPoint = new float[4];
    private final LinePath linePath = new LinePath();
    private final RingPath ringPath = new RingPath();
    private final HelixPath helixPath = new HelixPath();
    private final Map<String, QuadBuffer> pending = new LinkedHashMap<>();

    public void addLine(float fromX, float fromY, float toX, float toY, float distanceOffset, Style style, Look look, float startAlpha, float endAlpha,
                        float elapsedSeconds, float widthScale) {
        linePath.set(fromX, fromY, toX, toY);
        if (!drawable(linePath.length()) || (startAlpha <= 0f && endAlpha <= 0f)) {
            return;
        }
        stroke(linePath, style, look, startAlpha, endAlpha, elapsedSeconds, widthScale, 0f, distanceOffset);
    }

    public void addHelix(float fromX, float fromY, float toX, float toY, float distanceOffset, Helix helix, Style style, Look look,
                         float startAlpha, float endAlpha, float elapsedSeconds, float widthScale) {
        linePath.set(fromX, fromY, toX, toY);
        if (!drawable(linePath.length()) || (startAlpha <= 0f && endAlpha <= 0f)) {
            return;
        }
        helixPath.set(linePath, helix, distanceOffset);
        stroke(helixPath, style, look, startAlpha, endAlpha, elapsedSeconds, widthScale, helix.wavelength() / HELIX_PIECES_PER_WAVE, distanceOffset);
    }

    public void addSprite(String texturePath, float centreX, float centreY, float size, Color colour, float alpha) {
        SpriteAPI texture = spriteCache.texture(texturePath);
        if (texture == null || size <= 0f || alpha <= 0f) {
            return;
        }
        QuadBuffer buffer = pending.computeIfAbsent(texturePath, key -> new QuadBuffer());
        float half = size / 2f;
        float textureWidth = texture.getTextureWidth();
        float textureHeight = texture.getTextureHeight();
        float red = colour.getRed() / 255f;
        float green = colour.getGreen() / 255f;
        float blue = colour.getBlue() / 255f;
        buffer.corner(centreX - half, centreY - half, 0f, 0f, red, green, blue, alpha);
        buffer.corner(centreX + half, centreY - half, textureWidth, 0f, red, green, blue, alpha);
        buffer.corner(centreX + half, centreY + half, textureWidth, textureHeight, red, green, blue, alpha);
        buffer.corner(centreX - half, centreY + half, 0f, textureHeight, red, green, blue, alpha);
    }

    public void addRing(float cx, float cy, float radius, float[] outlineRadii, Style style, Look look, float alpha, float elapsedSeconds,
                        float widthScale) {
        if (!drawable(radius) || alpha <= 0f) {
            return;
        }
        ringPath.set(cx, cy, radius, outlineRadii);
        stroke(ringPath, style, look, alpha, alpha, elapsedSeconds, widthScale, ringPiece(ringPath.length()), 0f);
    }

    public void flush() {
        for (Map.Entry<String, QuadBuffer> entry : pending.entrySet()) {
            QuadBuffer buffer = entry.getValue();
            if (buffer.vertexCount == 0) {
                continue;
            }
            SpriteAPI texture = spriteCache.texture(entry.getKey());
            if (texture != null) {
                buffer.draw(texture);
            }
            buffer.vertexCount = 0;
        }
    }

    private void stroke(Path path, Style style, Look look, float startAlpha, float endAlpha, float elapsedSeconds, float widthScale, float maxPiece,
                        float distanceOffset) {
        if (look.fringeAlpha() > 0f && look.fringe() != null) {
            layer(path, style.fringeTexture(), style.fringeWidth() * widthScale, style, look.fringe(), look.fringeAlpha(), startAlpha, endAlpha,
                    elapsedSeconds, widthScale, maxPiece, distanceOffset);
        }
        if (look.coreAlpha() > 0f && look.core() != null) {
            layer(path, style.coreTexture(), style.coreWidth() * widthScale, style, look.core(), look.coreAlpha(), startAlpha, endAlpha,
                    elapsedSeconds, widthScale, maxPiece, distanceOffset);
        }
    }

    private void layer(Path path, String texturePath, float width, Style style, Color colour, float layerAlpha, float startAlpha, float endAlpha,
                       float elapsedSeconds, float widthScale, float maxPiece, float distanceOffset) {
        SpriteAPI texture = spriteCache.texture(texturePath);
        if (texture == null || width <= 0f) {
            return;
        }
        QuadBuffer buffer = pending.computeIfAbsent(texturePath, key -> new QuadBuffer());
        float tile = Math.max(MIN_PIECE, style.fringeWidth() * widthScale * style.tileLengthPerWidth());
        float scroll = positiveModulo(elapsedSeconds * style.scrollSpeed() * widthScale + distanceOffset, tile);
        float length = path.length();
        float half = width / 2f;
        float textureWidth = texture.getTextureWidth();
        float textureHeight = texture.getTextureHeight();
        float red = colour.getRed() / 255f;
        float green = colour.getGreen() / 255f;
        float blue = colour.getBlue() / 255f;
        float start = 0f;
        while (start < length) {
            float tileStart = tileStart(start, tile, scroll);
            float end = pieceEnd(start, tileStart, length, tile, maxPiece);
            if (end <= start) {
                return;
            }
            float startU = (start - tileStart) / tile * textureWidth;
            float endU = (end - tileStart) / tile * textureWidth;
            path.at(start, point);
            path.at(end, nextPoint);
            float startShade = layerAlpha * lerp(startAlpha, endAlpha, start / length) * path.shade(start);
            float endShade = layerAlpha * lerp(startAlpha, endAlpha, end / length) * path.shade(end);
            buffer.vertex(point, half, startU, 0f, red, green, blue, startShade);
            buffer.vertex(nextPoint, half, endU, 0f, red, green, blue, endShade);
            buffer.vertex(nextPoint, -half, endU, textureHeight, red, green, blue, endShade);
            buffer.vertex(point, -half, startU, textureHeight, red, green, blue, startShade);
            start = end;
        }
    }

    private static boolean drawable(float size) {
        return size > MIN_PIECE && Float.isFinite(size);
    }

    static float ringPiece(float ringLength) {
        int pieces = Math.max(MIN_RING_PIECES, Math.min(MAX_RING_PIECES, Math.round(ringLength / RING_PIECE_LENGTH)));
        return ringLength / pieces;
    }

    static float tileStart(float start, float tile, float scroll) {
        float intoTile = positiveModulo(start + scroll, tile);
        float snap = Math.max(TILE_EPSILON, 4f * Math.ulp(Math.abs(start) + tile));
        return tile - intoTile < snap ? start : start - intoTile;
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

    private static final class QuadBuffer {
        private float[] vertices = new float[FLOATS_PER_VERTEX * 256];
        private int vertexCount;

        void vertex(float[] at, float offset, float u, float v, float red, float green, float blue, float alpha) {
            corner(at[0] + at[2] * offset, at[1] + at[3] * offset, u, v, red, green, blue, alpha);
        }

        void corner(float x, float y, float u, float v, float red, float green, float blue, float alpha) {
            int index = vertexCount * FLOATS_PER_VERTEX;
            if (index + FLOATS_PER_VERTEX > vertices.length) {
                vertices = Arrays.copyOf(vertices, vertices.length * 2);
            }
            vertices[index] = x;
            vertices[index + 1] = y;
            vertices[index + 2] = u;
            vertices[index + 3] = v;
            vertices[index + 4] = red;
            vertices[index + 5] = green;
            vertices[index + 6] = blue;
            vertices[index + 7] = alpha;
            vertexCount++;
        }

        void draw(SpriteAPI texture) {
            try (GlScope scope = GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE)) {
                texture.bindTexture();
                GL11.glBegin(GL11.GL_QUADS);
                for (int index = 0; index < vertexCount * FLOATS_PER_VERTEX; index += FLOATS_PER_VERTEX) {
                    GL11.glColor4f(vertices[index + 4], vertices[index + 5], vertices[index + 6], vertices[index + 7]);
                    GL11.glTexCoord2f(vertices[index + 2], vertices[index + 3]);
                    GL11.glVertex2f(vertices[index], vertices[index + 1]);
                }
                GL11.glEnd();
            }
        }
    }

    static final class LinePath implements Path {
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

    static final class HelixPath implements Path {
        private Path base;
        private Helix helix;
        private float distanceOffset;

        void set(Path basePath, Helix strand, float offset) {
            base = basePath;
            helix = strand;
            distanceOffset = offset;
        }

        @Override
        public float length() {
            return base.length();
        }

        @Override
        public void at(float distance, float[] out) {
            base.at(distance, out);
            float along = distanceOffset + distance;
            float offset = helix.offset(along);
            float slope = helix.slope(along);
            float normalX = out[2];
            float normalY = out[3];
            float width = helix.width(along) / (float) Math.sqrt(1f + slope * slope);
            out[0] += normalX * offset;
            out[1] += normalY * offset;
            out[2] = (normalX - normalY * slope) * width;
            out[3] = (normalY + normalX * slope) * width;
        }

        @Override
        public float shade(float distance) {
            return helix.shade(distanceOffset + distance);
        }
    }

    static final class RingPath implements Path {
        private float cx;
        private float cy;
        private float radius;
        private float[] outlineRadii;
        private float[] cumulative = new float[0];
        private float length;

        void set(float centreX, float centreY, float ringRadius, float[] radii) {
            cx = centreX;
            cy = centreY;
            radius = ringRadius;
            outlineRadii = radii;
            if (radii == null) {
                length = (float) (2.0 * Math.PI * radius);
                return;
            }
            UnitCircle circle = UnitCircle.of(radii.length);
            if (cumulative.length != radii.length + 1) {
                cumulative = new float[radii.length + 1];
            }
            for (int i = 0; i < radii.length; i++) {
                int next = (i + 1) % radii.length;
                float dx = (circle.cos(next) * radii[next] - circle.cos(i) * radii[i]) * radius;
                float dy = (circle.sin(next) * radii[next] - circle.sin(i) * radii[i]) * radius;
                cumulative[i + 1] = cumulative[i] + (float) Math.sqrt(dx * dx + dy * dy);
            }
            length = cumulative[radii.length];
        }

        @Override
        public float length() {
            return length;
        }

        @Override
        public void at(float distance, float[] out) {
            double angle;
            float pointRadius = radius;
            if (outlineRadii == null) {
                angle = distance / radius;
            } else {
                int samples = outlineRadii.length;
                int segment = 0;
                while (segment < samples - 1 && cumulative[segment + 1] < distance) {
                    segment++;
                }
                float segmentLength = cumulative[segment + 1] - cumulative[segment];
                float fraction = segmentLength > 0f ? Math.max(0f, Math.min(1f, (distance - cumulative[segment]) / segmentLength)) : 0f;
                angle = (segment + fraction) * 2.0 * Math.PI / samples;
                pointRadius = radius * lerp(outlineRadii[segment], outlineRadii[(segment + 1) % samples], fraction);
            }
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            out[0] = cx + cos * pointRadius;
            out[1] = cy + sin * pointRadius;
            out[2] = cos;
            out[3] = sin;
        }
    }
}
