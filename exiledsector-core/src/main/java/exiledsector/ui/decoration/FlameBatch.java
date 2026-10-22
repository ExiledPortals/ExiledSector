package exiledsector.ui.decoration;

import com.fs.starfarer.api.graphics.SpriteAPI;
import exiledlib.ui.util.GlScope;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class FlameBatch implements AutoCloseable {

    private static final float COLOR_CHANNEL_MAX = 255f;

    private float textureRight;
    private float textureTop;
    private boolean drawing;

    FlameBatch begin(SpriteAPI flameTexture) {
        if (drawing) {
            throw new IllegalStateException("FlameBatch begun twice without closing");
        }
        textureRight = flameTexture.getTextureWidth();
        textureTop = flameTexture.getTextureHeight();
        GlScope.textured(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        try {
            flameTexture.bindTexture();
        } catch (RuntimeException e) {
            GlScope.restore();
            throw e;
        }
        GL11.glBegin(GL11.GL_QUADS);
        drawing = true;
        return this;
    }

    float textureRight() {
        return textureRight;
    }

    float textureTop() {
        return textureTop;
    }

    void vertex(Color color, float alpha, float textureX, float textureY, float x, float y) {
        GL11.glColor4f(color.getRed() / COLOR_CHANNEL_MAX, color.getGreen() / COLOR_CHANNEL_MAX, color.getBlue() / COLOR_CHANNEL_MAX, alpha);
        GL11.glTexCoord2f(textureX, textureY);
        GL11.glVertex2f(x, y);
    }

    @Override
    public void close() {
        if (!drawing) {
            return;
        }
        drawing = false;
        try {
            GL11.glEnd();
        } finally {
            GlScope.restore();
        }
    }
}
