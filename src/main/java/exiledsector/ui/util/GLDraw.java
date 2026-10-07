package exiledsector.ui.util;

import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class GLDraw {

    private GLDraw() {
    }

    public static void fillQuad(float x, float y, float width, float height, Color color, float alphaMult) {
        try (GlScope scope = GlScope.flat()) {
            Misc.setColor(color, alphaMult);
            rectangle(GL11.GL_QUADS, x, y, width, height);
        }
    }

    public static void strokeQuad(float x, float y, float width, float height, Color color, float lineWidth, float alphaMult) {
        try (GlScope scope = GlScope.flat().lineWidth(lineWidth)) {
            Misc.setColor(color, alphaMult);
            rectangle(GL11.GL_LINE_LOOP, x, y, width, height);
        }
    }

    public static void horizontalLines(float x, float y, float width, float height, float spacing, Color color, float alphaMult) {
        try (GlScope scope = GlScope.flat().lineWidth(1f)) {
            Misc.setColor(color, alphaMult);
            GL11.glBegin(GL11.GL_LINES);
            for (float lineY = y + spacing; lineY < y + height; lineY += spacing) {
                GL11.glVertex2f(x, lineY);
                GL11.glVertex2f(x + width, lineY);
            }
            GL11.glEnd();
        }
    }

    public static void innerGlow(float x, float y, float width, float height, float glowWidth, Color color, float alphaMult) {
        float inset = Math.min(glowWidth, Math.min(width, height) / 2f);
        float left = x + inset;
        float right = x + width - inset;
        float bottom = y + inset;
        float top = y + height - inset;
        try (GlScope scope = GlScope.flat()) {
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
        }
    }

    private static void rectangle(int primitive, float x, float y, float width, float height) {
        GL11.glBegin(primitive);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    }

    private static void edge(Color color, float alpha, float ax, float ay, float bx, float by) {
        Misc.setColor(color, alpha);
        GL11.glVertex2f(ax, ay);
        GL11.glVertex2f(bx, by);
    }
}
