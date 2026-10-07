package exiledsector.ui.socket;

import com.fs.starfarer.api.util.Misc;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.util.SpriteDraw;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

final class SocketWorkbenchFlair {

    private static final float LOAD_SECONDS = 0.45f;
    private static final float FLASH_SECONDS = 0.7f;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final int CIRCLE_STEPS = 72;
    private static final int ARC_STEPS = 8;
    private static final float GRID_SPACING = 14f;
    private static final float GRID_ALPHA = 0.06f;
    private static final float OUTER_RING_SPEED = 10f;
    private static final float TICK_RING_SPEED = -6f;
    private static final float SCANNER_SPEED = -40f;
    private static final float SCAN_SPEED = 0.55f;
    private static final float PULSE_SPEED = 0.7f;
    private static final float BRACKET_TRAVEL = 14f;
    private static final float ICON_INSET = 0.12f;
    private static final Color SOCKET_FILL = new Color(0, 0, 0, 170);
    private static final SpriteCache ICONS = new SpriteCache(SocketWorkbenchFlair.class);

    private float elapsedSeconds;
    private float loadProgress;
    private float flashProgress;
    private Color flashColor = Color.WHITE;

    void advance(float amount, boolean loaded) {
        elapsedSeconds += amount;
        float step = amount / LOAD_SECONDS;
        loadProgress = Math.max(0f, Math.min(1f, loadProgress + (loaded ? step : -step)));
        flashProgress = Math.max(0f, flashProgress - amount / FLASH_SECONDS);
    }

    void restartLoad() {
        loadProgress = 0f;
    }

    void flash(Color color) {
        flashProgress = 1f;
        flashColor = color;
    }

    void render(float cx, float cy, float radius, Color accent, Color subjectColor, String iconPath, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        float easedLoad = 1f - (1f - loadProgress) * (1f - loadProgress);
        float pulse = 0.5f + 0.5f * (float) Math.sin(elapsedSeconds * 2.2f);
        float half = radius * 0.42f;
        Color coreColor = subjectColor == null ? accent : subjectColor;

        drawGrid(cx, cy, radius, accent, alpha);
        additive();
        radialGlow(cx, cy, half * 0.8f, radius * 1.05f, coreColor, (0.12f + 0.18f * easedLoad) * (0.7f + 0.3f * pulse) * alpha);
        circle(cx, cy, radius, accent, 1f, 0.25f * alpha);
        arcSegments(cx, cy, radius - 5f, 12, 0.62f, elapsedSeconds * OUTER_RING_SPEED, 3f, accent, 0.55f * alpha);
        ticks(cx, cy, radius * 0.8f, elapsedSeconds * TICK_RING_SPEED, accent, 0.45f * alpha);
        arcSegments(cx, cy, radius * 0.66f, 3, 0.2f, elapsedSeconds * SCANNER_SPEED, 2f, coreColor, (0.35f + 0.5f * easedLoad) * alpha);
        arms(cx, cy, half, radius * 0.62f, accent, coreColor, easedLoad, alpha);
        normalBlend();

        GLDraw.fillQuad(cx - half, cy - half, half * 2f, half * 2f, SOCKET_FILL, alpha);
        GLDraw.innerGlow(cx - half, cy - half, half * 2f, half * 2f, half * 0.5f, coreColor, (0.15f + 0.35f * easedLoad * pulse) * alpha);
        GLDraw.strokeQuad(cx - half, cy - half, half * 2f, half * 2f, coreColor, 1.5f, (0.5f + 0.4f * easedLoad) * alpha);
        if (iconPath != null) {
            float size = half * 2f * (1f - ICON_INSET * 2f);
            SpriteDraw.drawAtCenter(ICONS, iconPath, cx, cy, size, size, Color.WHITE, easedLoad * alpha);
        }
        additive();
        brackets(cx, cy, half + 6f + (1f - easedLoad) * BRACKET_TRAVEL, half * 0.35f, accent, (0.6f + 0.4f * easedLoad) * alpha);
        if (iconPath != null) {
            scanLine(cx, cy, half, coreColor, easedLoad * alpha);
        }
        if (flashProgress > 0f) {
            float spread = 1f - flashProgress;
            circle(cx, cy, half + (radius + 4f - half) * spread, flashColor, 3f, flashProgress * alpha);
            circle(cx, cy, half + (radius - half) * spread * 0.6f, flashColor, 1.5f, flashProgress * 0.6f * alpha);
            GLDraw.fillQuad(cx - half, cy - half, half * 2f, half * 2f, flashColor, flashProgress * flashProgress * 0.5f * alpha);
            additive();
        }
        normalBlend();
    }

    private void drawGrid(float cx, float cy, float radius, Color accent, float alpha) {
        float extent = radius * 1.05f;
        GLDraw.horizontalLines(cx - extent, cy - extent, extent * 2f, extent * 2f, GRID_SPACING, accent, GRID_ALPHA * alpha);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Misc.setColor(accent, GRID_ALPHA * alpha);
        GL11.glBegin(GL11.GL_LINES);
        for (float x = cx - extent + GRID_SPACING; x < cx + extent; x += GRID_SPACING) {
            GL11.glVertex2f(x, cy - extent);
            GL11.glVertex2f(x, cy + extent);
        }
        GL11.glEnd();
    }

    private void arms(float cx, float cy, float half, float reach, Color accent, Color coreColor, float easedLoad, float alpha) {
        float inner = half * 1.45f;
        for (int i = 0; i < 4; i++) {
            float angle = TWO_PI * (i / 4f + 0.125f);
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);
            GL11.glLineWidth(1.5f);
            Misc.setColor(accent, 0.4f * alpha);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(cx + dx * inner, cy + dy * inner);
            GL11.glVertex2f(cx + dx * reach, cy + dy * reach);
            GL11.glEnd();
            float travel = (elapsedSeconds * PULSE_SPEED + i * 0.25f) % 1f;
            float distance = reach - (reach - inner) * travel;
            dot(cx + dx * distance, cy + dy * distance, 2.5f, coreColor, (0.3f + 0.7f * easedLoad) * (1f - travel * 0.5f) * alpha);
            dot(cx + dx * reach, cy + dy * reach, 3f, accent, 0.8f * alpha);
        }
        GL11.glLineWidth(1f);
    }

    private void scanLine(float cx, float cy, float half, Color coreColor, float alpha) {
        float travel = (elapsedSeconds * SCAN_SPEED) % 1f;
        float y = cy + half - travel * half * 2f;
        for (int i = 0; i < 4; i++) {
            float lineY = y + i * 2f;
            if (lineY > cy + half) {
                break;
            }
            GL11.glLineWidth(i == 0 ? 2f : 1f);
            Misc.setColor(coreColor, (i == 0 ? 0.9f : 0.35f / i) * alpha);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2f(cx - half, lineY);
            GL11.glVertex2f(cx + half, lineY);
            GL11.glEnd();
        }
        GL11.glLineWidth(1f);
    }

    private static void additive() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
    }

    private static void normalBlend() {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private static void circle(float cx, float cy, float radius, Color color, float width, float alpha) {
        GL11.glLineWidth(width);
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < CIRCLE_STEPS; i++) {
            float angle = TWO_PI * i / CIRCLE_STEPS;
            GL11.glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
        }
        GL11.glEnd();
        GL11.glLineWidth(1f);
    }

    private static void arcSegments(float cx, float cy, float radius, int segments, float fill, float rotationDeg, float width, Color color,
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

    private static void ticks(float cx, float cy, float radius, float rotationDeg, Color color, float alpha) {
        int count = 90;
        float rotation = (float) Math.toRadians(rotationDeg);
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < count; i++) {
            float angle = rotation + TWO_PI * i / count;
            float length = i % 9 == 0 ? 9f : 4f;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            GL11.glVertex2f(cx + cos * radius, cy + sin * radius);
            GL11.glVertex2f(cx + cos * (radius - length), cy + sin * (radius - length));
        }
        GL11.glEnd();
    }

    private static void radialGlow(float cx, float cy, float inner, float outer, Color color, float alpha) {
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= CIRCLE_STEPS; i++) {
            float angle = TWO_PI * i / CIRCLE_STEPS;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            Misc.setColor(color, alpha);
            GL11.glVertex2f(cx + cos * inner, cy + sin * inner);
            Misc.setColor(color, 0f);
            GL11.glVertex2f(cx + cos * outer, cy + sin * outer);
        }
        GL11.glEnd();
    }

    private static void brackets(float cx, float cy, float half, float arm, Color color, float alpha) {
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

    private static void dot(float x, float y, float size, Color color, float alpha) {
        Misc.setColor(color, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x - size, y);
        GL11.glVertex2f(x, y - size);
        GL11.glVertex2f(x + size, y);
        GL11.glVertex2f(x, y + size);
        GL11.glEnd();
    }
}
