package exiledsector.ui.util;

import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class GLDraw {

    private GLDraw() {
    }

    public static void fillQuad(float x, float y, float width, float height, Color color, float alphaMult) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Misc.setColor(color, alphaMult);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void strokeQuad(float x, float y, float width, float height, Color color, float lineWidth, float alphaMult) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Misc.setColor(color, alphaMult);
        GL11.glLineWidth(lineWidth);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
        GL11.glLineWidth(1f);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void horizontalLines(float x, float y, float width, float height, float spacing, Color color, float alphaMult) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Misc.setColor(color, alphaMult);
        GL11.glLineWidth(1f);
        GL11.glBegin(GL11.GL_LINES);
        for (float lineY = y + spacing; lineY < y + height; lineY += spacing) {
            GL11.glVertex2f(x, lineY);
            GL11.glVertex2f(x + width, lineY);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void innerGlow(float x, float y, float width, float height, float glowWidth, Color color, float alphaMult) {
        float inset = Math.min(glowWidth, Math.min(width, height) / 2f);
        float left = x + inset;
        float right = x + width - inset;
        float bottom = y + inset;
        float top = y + height - inset;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glBegin(GL11.GL_QUADS);
        edge(color, alphaMult, x, y, x + width, y);
        edge(color, 0f, right, bottom, left, bottom);
        edge(color, alphaMult, x + width, y, x + width, y + height);
        edge(color, 0f, right, top, right, bottom);
        edge(color, alphaMult, x + width, y + height, x, y + height);
        edge(color, 0f, left, top, right, top);
        edge(color, alphaMult, x, y + height, x, y);
        edge(color, 0f, left, bottom, left, top);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_BLEND);
    }

    private static void edge(Color color, float alpha, float ax, float ay, float bx, float by) {
        Misc.setColor(color, alpha);
        GL11.glVertex2f(ax, ay);
        GL11.glVertex2f(bx, by);
    }
}
