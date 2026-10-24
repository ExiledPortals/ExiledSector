package exiledsector.ui.util;

import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public final class HoloShapes {

    public static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final int CIRCLE_STEPS = 72;
    private static final int ARC_STEPS = 8;
    private static final UnitCircle UNIT_CIRCLE = UnitCircle.of(CIRCLE_STEPS);

    private HoloShapes() {
    }

    public static void circle(float cx, float cy, float radius, Color color, float width, float alpha) {
        GL11.glLineWidth(width);
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < CIRCLE_STEPS; i++) {
            GL11.glVertex2f(cx + UNIT_CIRCLE.cos(i) * radius, cy + UNIT_CIRCLE.sin(i) * radius);
        }
        GL11.glEnd();
        GL11.glLineWidth(1f);
    }

    public static void disc(float cx, float cy, float radius, Color color, float alpha) {
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= CIRCLE_STEPS; i++) {
            int index = i % CIRCLE_STEPS;
            GL11.glVertex2f(cx + UNIT_CIRCLE.cos(index) * radius, cy + UNIT_CIRCLE.sin(index) * radius);
        }
        GL11.glEnd();
    }

    public static void arcSegments(float cx, float cy, float radius, int segments, float fill, float rotationDeg, float width, Color color,
                                   float alpha) {
        float slot = TWO_PI / segments;
        float rotation = (float) Math.toRadians(rotationDeg);
        GL11.glLineWidth(width);
        Misc.setColor(color, alpha);
        for (int i = 0; i < segments; i++) {
            float start = rotation + i * slot;
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int step = 0; step <= ARC_STEPS; step++) {
                float angle = start + slot * fill * step / ARC_STEPS;
                GL11.glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
            }
            GL11.glEnd();
        }
        GL11.glLineWidth(1f);
    }

    public static void ticks(float cx, float cy, float radius, int count, int majorEvery, float majorLength, float minorLength, float rotationDeg,
                             Color color, float alpha) {
        float rotation = (float) Math.toRadians(rotationDeg);
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < count; i++) {
            float angle = rotation + TWO_PI * i / count;
            float length = i % majorEvery == 0 ? majorLength : minorLength;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            GL11.glVertex2f(cx + cos * radius, cy + sin * radius);
            GL11.glVertex2f(cx + cos * (radius - length), cy + sin * (radius - length));
        }
        GL11.glEnd();
    }

    public static void radialGlow(float cx, float cy, float inner, float outer, Color color, float alpha) {
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= CIRCLE_STEPS; i++) {
            int index = i % CIRCLE_STEPS;
            float cos = UNIT_CIRCLE.cos(index);
            float sin = UNIT_CIRCLE.sin(index);
            Misc.setColor(color, alpha);
            GL11.glVertex2f(cx + cos * inner, cy + sin * inner);
            Misc.setColor(color, 0f);
            GL11.glVertex2f(cx + cos * outer, cy + sin * outer);
        }
        GL11.glEnd();
    }

    public static void brackets(float cx, float cy, float half, float arm, Color color, float alpha) {
        GL11.glLineWidth(2.5f);
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINES);
        for (int corner = 0; corner < 4; corner++) {
            float sx = corner % 2 == 0 ? -1f : 1f;
            float sy = corner < 2 ? -1f : 1f;
            float x = cx + sx * half;
            float y = cy + sy * half;
            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x - sx * arm, y);
            GL11.glVertex2f(x, y);
            GL11.glVertex2f(x, y - sy * arm);
        }
        GL11.glEnd();
        GL11.glLineWidth(1f);
    }

    public static void diamond(float x, float y, float size, Color color, float alpha) {
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x - size, y);
        GL11.glVertex2f(x, y - size);
        GL11.glVertex2f(x + size, y);
        GL11.glVertex2f(x, y + size);
        GL11.glEnd();
    }

    public static void line(float fromX, float fromY, float toX, float toY, float width, Color color, float alpha) {
        GL11.glLineWidth(width);
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(fromX, fromY);
        GL11.glVertex2f(toX, toY);
        GL11.glEnd();
        GL11.glLineWidth(1f);
    }
}
