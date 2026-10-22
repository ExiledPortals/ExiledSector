package exiledlib.ui.util;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class TexturedQuadBatch implements AutoCloseable {

    interface Backend {

        void bindTexture(SpriteAPI texture);

        void beginQuads();

        void color(byte red, byte green, byte blue, byte alpha);

        void texCoord(float textureX, float textureY);

        void vertex(float x, float y);

        void end();
    }

    private static final Backend LWJGL_BACKEND = new Backend() {
        @Override
        public void bindTexture(SpriteAPI texture) {
            texture.bindTexture();
        }

        @Override
        public void beginQuads() {
            GL11.glBegin(GL11.GL_QUADS);
        }

        @Override
        public void color(byte red, byte green, byte blue, byte alpha) {
            GL11.glColor4ub(red, green, blue, alpha);
        }

        @Override
        public void texCoord(float textureX, float textureY) {
            GL11.glTexCoord2f(textureX, textureY);
        }

        @Override
        public void vertex(float x, float y) {
            GL11.glVertex2f(x, y);
        }

        @Override
        public void end() {
            GL11.glEnd();
        }
    };

    private static Backend backend = LWJGL_BACKEND;

    private float textureLeft;
    private float textureBottom;
    private float textureRight;
    private float textureTop;
    private boolean drawing;

    public TexturedQuadBatch beginNormal(SpriteAPI texture) {
        return begin(texture, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public TexturedQuadBatch beginAdditive(SpriteAPI texture) {
        return begin(texture, GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    }

    private TexturedQuadBatch begin(SpriteAPI texture, int sourceFactor, int destinationFactor) {
        if (drawing) {
            throw new IllegalStateException("TexturedQuadBatch begun twice without closing");
        }
        textureLeft = texture.getTexX();
        textureBottom = texture.getTexY();
        textureRight = textureLeft + texture.getTexWidth();
        textureTop = textureBottom + texture.getTexHeight();
        GlScope.textured(sourceFactor, destinationFactor);
        drawing = true;
        backend.bindTexture(texture);
        backend.beginQuads();
        return this;
    }

    public void quadAtCenter(float centerX, float centerY, float quadWidth, float quadHeight, float angleDeg, Color tint,
                             float alphaMult) {
        backend.color((byte) tint.getRed(), (byte) tint.getGreen(), (byte) tint.getBlue(), (byte) (int) (tint.getAlpha() * alphaMult));
        float halfWidth = quadWidth / 2f;
        float halfHeight = quadHeight / 2f;
        float cos = 1f;
        float sin = 0f;
        if (angleDeg != 0f) {
            double angle = Math.toRadians(angleDeg);
            cos = (float) Math.cos(angle);
            sin = (float) Math.sin(angle);
        }
        corner(centerX, centerY, -halfWidth, -halfHeight, cos, sin, textureLeft, textureBottom);
        corner(centerX, centerY, -halfWidth, halfHeight, cos, sin, textureLeft, textureTop);
        corner(centerX, centerY, halfWidth, halfHeight, cos, sin, textureRight, textureTop);
        corner(centerX, centerY, halfWidth, -halfHeight, cos, sin, textureRight, textureBottom);
    }

    private static void corner(float centerX, float centerY, float offsetX, float offsetY, float cos, float sin, float textureX,
                               float textureY) {
        backend.texCoord(textureX, textureY);
        backend.vertex(centerX + offsetX * cos - offsetY * sin, centerY + offsetX * sin + offsetY * cos);
    }

    @Override
    public void close() {
        if (!drawing) {
            return;
        }
        drawing = false;
        try {
            backend.end();
        } finally {
            GlScope.restore();
        }
    }

    static Backend useBackend(Backend replacement) {
        Backend previous = backend;
        backend = replacement;
        return previous;
    }
}
