package exiledsector.ui.socket;

import exiledsector.ui.util.Fade;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.SpriteCache;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.util.SpriteDraw;

import java.awt.Color;

final class SocketWorkbenchFlair {

    private static final float LOAD_SECONDS = 0.45f;
    private static final float FLASH_SECONDS = 0.7f;
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
        loadProgress = Fade.step(loadProgress, loaded, step);
        flashProgress = Math.max(0f, flashProgress - amount / FLASH_SECONDS);
    }

    void restartLoad() {
        loadProgress = 0f;
    }

    void flash(Color color) {
        flashProgress = 1f;
        flashColor = color;
    }

    void render(float cx, float cy, float radius, Color accent, Color subjectColor, String iconPath, CargoIconLook iconLook, float alpha) {
        if (alpha <= 0f) {
            return;
        }
        try (GlScope scope = GlScope.save(GlScope.DRAW_ATTRIBS)) {
            renderLayers(scope, cx, cy, radius, accent, subjectColor, iconPath, iconLook, alpha);
        }
    }

    private void renderLayers(GlScope scope, float cx, float cy, float radius, Color accent, Color subjectColor, String iconPath,
                              CargoIconLook iconLook, float alpha) {
        float easedLoad = 1f - (1f - loadProgress) * (1f - loadProgress);
        float pulse = 0.5f + 0.5f * (float) Math.sin(elapsedSeconds * 2.2f);
        float half = radius * 0.42f;
        Color coreColor = subjectColor == null ? accent : subjectColor;

        scope.untextured().additive();
        HoloShapes.radialGlow(cx, cy, half * 0.8f, radius * 1.05f, coreColor, (0.12f + 0.18f * easedLoad) * (0.7f + 0.3f * pulse) * alpha);
        HoloShapes.circle(cx, cy, radius, accent, 1f, 0.25f * alpha);
        HoloShapes.arcSegments(cx, cy, radius - 5f, 12, 0.62f, elapsedSeconds * OUTER_RING_SPEED, 3f, accent, 0.55f * alpha);
        HoloShapes.ticks(cx, cy, radius * 0.8f, 90, 9, 9f, 4f, elapsedSeconds * TICK_RING_SPEED, accent, 0.45f * alpha);
        HoloShapes.arcSegments(cx, cy, radius * 0.66f, 3, 0.2f, elapsedSeconds * SCANNER_SPEED, 2f, coreColor, (0.35f + 0.5f * easedLoad) * alpha);
        arms(cx, cy, half, radius * 0.62f, accent, coreColor, easedLoad, alpha);
        scope.normalBlend();

        GLDraw.fillQuad(cx - half, cy - half, half * 2f, half * 2f, SOCKET_FILL, alpha);
        GLDraw.innerGlow(cx - half, cy - half, half * 2f, half * 2f, half * 0.5f, coreColor, (0.15f + 0.35f * easedLoad * pulse) * alpha);
        GLDraw.strokeQuad(cx - half, cy - half, half * 2f, half * 2f, coreColor, 1.5f, (0.5f + 0.4f * easedLoad) * alpha);
        if (iconPath != null) {
            float size = half * 2f * (1f - ICON_INSET * 2f);
            if (iconLook == null) {
                SpriteDraw.drawAtCenter(ICONS, iconPath, cx, cy, size, size, Color.WHITE, easedLoad * alpha);
            } else {
                CargoIconFx.render(ICONS, iconPath, iconLook.unframed(), cx - size / 2f, cy - size / 2f, size, size, easedLoad * alpha, 0f,
                        System.identityHashCode(this), -1f);
            }
        }
        scope.untextured().additive();
        HoloShapes.brackets(cx, cy, half + 6f + (1f - easedLoad) * BRACKET_TRAVEL, half * 0.35f, accent, (0.6f + 0.4f * easedLoad) * alpha);
        if (iconPath != null) {
            scanLine(cx, cy, half, coreColor, easedLoad * alpha);
        }
        if (flashProgress > 0f) {
            float spread = 1f - flashProgress;
            HoloShapes.circle(cx, cy, half + (radius + 4f - half) * spread, flashColor, 3f, flashProgress * alpha);
            HoloShapes.circle(cx, cy, half + (radius - half) * spread * 0.6f, flashColor, 1.5f, flashProgress * 0.6f * alpha);
            GLDraw.fillQuad(cx - half, cy - half, half * 2f, half * 2f, flashColor, flashProgress * flashProgress * 0.5f * alpha);
            scope.untextured().additive();
        }
        scope.normalBlend();
    }

    private void arms(float cx, float cy, float half, float reach, Color accent, Color coreColor, float easedLoad, float alpha) {
        float inner = half * 1.45f;
        for (int i = 0; i < 4; i++) {
            float angle = HoloShapes.TWO_PI * (i / 4f + 0.125f);
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);
            HoloShapes.line(cx + dx * inner, cy + dy * inner, cx + dx * reach, cy + dy * reach, 1.5f, accent, 0.4f * alpha);
            float travel = (elapsedSeconds * PULSE_SPEED + i * 0.25f) % 1f;
            float distance = reach - (reach - inner) * travel;
            HoloShapes.diamond(cx + dx * distance, cy + dy * distance, 2.5f, coreColor, (0.3f + 0.7f * easedLoad) * (1f - travel * 0.5f) * alpha);
            HoloShapes.diamond(cx + dx * reach, cy + dy * reach, 3f, accent, 0.8f * alpha);
        }
    }

    private void scanLine(float cx, float cy, float half, Color coreColor, float alpha) {
        float travel = (elapsedSeconds * SCAN_SPEED) % 1f;
        float y = cy + half - travel * half * 2f;
        for (int i = 0; i < 4; i++) {
            float lineY = y + i * 2f;
            if (lineY > cy + half) {
                break;
            }
            HoloShapes.line(cx - half, lineY, cx + half, lineY, i == 0 ? 2f : 1f, coreColor, (i == 0 ? 0.9f : 0.35f / i) * alpha);
        }
    }
}
