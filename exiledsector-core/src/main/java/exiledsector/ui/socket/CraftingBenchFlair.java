package exiledsector.ui.socket;

import exiledsector.skills.skilleffect.render.Fade;
import exiledsector.skills.skilleffect.render.SpriteCache;
import exiledsector.ui.cargo.AnimatedItemIcons;
import exiledsector.ui.cargo.CargoIconFx;
import exiledsector.ui.cargo.CargoIconLook;
import exiledsector.ui.util.GLDraw;
import exiledsector.ui.util.GlScope;
import exiledsector.ui.util.HoloBeams;
import exiledsector.ui.util.HoloShapes;
import exiledsector.ui.util.HoloSocketFrame;

import java.awt.Color;

final class CraftingBenchFlair {

    private static final float LOAD_SECONDS = 0.45f;
    private static final float FLASH_SECONDS = 0.7f;
    private static final HoloSocketFrame.Spec FRAME = new HoloSocketFrame.Spec(1.05f, 5f,
            new HoloSocketFrame.OuterArcs(5f, 12, 0.62f, 10f, 3f),
            new HoloSocketFrame.Ticks(0.8f, 90, 9, 9f, 4f, false, -6f),
            new HoloSocketFrame.Scanner(0.66f, 0.2f, -40f),
            new Color(0, 0, 0, 170),
            new HoloSocketFrame.ScanLines(0.55f, 4, 0.9f, 0.35f));
    private static final float PULSE_SPEED = 0.7f;
    private static final float BRACKET_TRAVEL = 14f;
    private static final float ICON_INSET = 0.12f;
    private static final SpriteCache ICONS = new SpriteCache(CraftingBenchFlair.class);

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
        float half = HoloSocketFrame.socketHalf(radius);
        Color coreColor = subjectColor == null ? accent : subjectColor;

        scope.untextured().additive();
        HoloSocketFrame.drawRings(FRAME, cx, cy, radius, elapsedSeconds, new HoloSocketFrame.RingShading(accent, coreColor,
                (0.12f + 0.18f * easedLoad) * (0.7f + 0.3f * pulse) * alpha, 0.5f * alpha, 0.55f * alpha, 0.45f * alpha, coreColor,
                (0.35f + 0.5f * easedLoad) * alpha));
        arms(cx, cy, half, radius * 0.62f, accent, coreColor, easedLoad, alpha);
        scope.normalBlend();

        HoloSocketFrame.drawSocket(FRAME, cx, cy, half, coreColor, alpha, (0.15f + 0.35f * easedLoad * pulse) * alpha,
                (0.5f + 0.4f * easedLoad) * alpha);
        if (iconPath != null) {
            float size = half * 2f * (1f - ICON_INSET * 2f);
            if (iconLook == null) {
                AnimatedItemIcons.drawAtCenter(ICONS, iconPath, cx, cy, size, size, Color.WHITE, easedLoad * alpha);
            } else {
                CargoIconLook shownLook = iconLook.ownArt() ? iconLook : iconLook.unframed();
                CargoIconFx.render(ICONS, iconPath, shownLook, cx - size / 2f, cy - size / 2f, size, size, easedLoad * alpha, 0f,
                        System.identityHashCode(this), -1f);
            }
        }
        scope.untextured().additive();
        HoloSocketFrame.drawBrackets(cx, cy, half + 6f + (1f - easedLoad) * BRACKET_TRAVEL, half, accent, (0.6f + 0.4f * easedLoad) * alpha);
        if (iconPath != null) {
            HoloSocketFrame.drawScanLines(FRAME, cx, cy, half, coreColor, elapsedSeconds, easedLoad * alpha);
        }
        if (flashProgress > 0f) {
            float spread = 1f - flashProgress;
            HoloBeams.circle(cx, cy, half + (radius + 4f - half) * spread, 3f, flashColor, flashProgress * alpha);
            HoloBeams.ring(cx, cy, half + (radius - half) * spread * 0.6f, spread * 90f, flashColor, flashProgress * 0.8f * alpha);
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
            HoloBeams.line(cx + dx * inner, cy + dy * inner, cx + dx * reach, cy + dy * reach, 1.5f, accent, 0.4f * alpha);
            float travel = (elapsedSeconds * PULSE_SPEED + i * 0.25f) % 1f;
            float distance = reach - (reach - inner) * travel;
            HoloShapes.diamond(cx + dx * distance, cy + dy * distance, 2.5f, coreColor, (0.3f + 0.7f * easedLoad) * (1f - travel * 0.5f) * alpha);
            HoloShapes.diamond(cx + dx * reach, cy + dy * reach, 3f, accent, 0.8f * alpha);
        }
    }
}
