package exiledsector.ui.util;

import com.fs.starfarer.api.Global;

import java.awt.Color;

public final class HoloTransition {

    private static final float OPEN_SECONDS = 0.35f;
    private static final float CLOSE_SECONDS = 0.22f;
    private static final float LINE_PHASE_END = 0.3f;
    private static final float CONTENT_PHASE_START = 0.7f;
    private static final float FILL_ALPHA = 0.85f;
    private static final float EDGE_THICKNESS = 2f;
    private static final float EDGE_GLOW_WIDTH = 10f;
    private static final float EDGE_GLOW_ALPHA = 0.5f;
    private static final float SCANLINE_SPACING = 3f;
    private static final float SCANLINE_ALPHA = 0.08f;
    private static final Color FILL_COLOR = Color.BLACK;

    private float openProgress;
    private boolean opening;

    public void open() {
        opening = true;
    }

    public void close() {
        opening = false;
    }

    public void openInstantly() {
        opening = true;
        openProgress = 1f;
    }

    public void advance(float amount) {
        float step = amount / (opening ? OPEN_SECONDS : CLOSE_SECONDS);
        openProgress = clamp(openProgress + (opening ? step : -step));
    }

    public boolean isOpening() {
        return opening;
    }

    public boolean isVisible() {
        return opening || openProgress > 0f;
    }

    public boolean isAnimating() {
        return opening ? openProgress < 1f : openProgress > 0f;
    }

    public boolean isFullyClosed() {
        return !opening && openProgress <= 0f;
    }

    public float progress() {
        return openProgress;
    }

    public float contentAlpha() {
        return easeOut(phase(CONTENT_PHASE_START, 1f));
    }

    public float backdropAlpha() {
        return easeOut(openProgress);
    }

    public void drawProjection(float x, float y, float width, float height, Color accent, float alphaMult) {
        float alpha = (1f - contentAlpha()) * alphaMult;
        if (alpha <= 0f || openProgress <= 0f) {
            return;
        }
        float currentWidth = width * easeOut(phase(0f, LINE_PHASE_END));
        float currentHeight = Math.max(EDGE_THICKNESS, height * easeOut(phase(LINE_PHASE_END, CONTENT_PHASE_START)));
        float left = x + (width - currentWidth) / 2f;
        float bottom = y + (height - currentHeight) / 2f;

        GLDraw.fillQuad(left, bottom, currentWidth, currentHeight, FILL_COLOR, FILL_ALPHA * alpha);
        if (currentHeight > EDGE_THICKNESS * 2f) {
            GLDraw.horizontalLines(left, bottom, currentWidth, currentHeight, SCANLINE_SPACING, accent, SCANLINE_ALPHA * alpha);
            GLDraw.innerGlow(left, bottom, currentWidth, currentHeight, EDGE_GLOW_WIDTH, accent, EDGE_GLOW_ALPHA * alpha);
            GLDraw.strokeQuad(left, bottom, currentWidth, currentHeight, accent, 1f, alpha);
        }
        GLDraw.fillQuad(left, bottom + currentHeight - EDGE_THICKNESS, currentWidth, EDGE_THICKNESS, accent, alpha);
        GLDraw.fillQuad(left, bottom, currentWidth, EDGE_THICKNESS, accent, alpha);
    }

    public GlScope reveal(float x, float y, float width, float height) {
        float reveal = contentAlpha();
        if (reveal >= 1f) {
            return null;
        }
        float screenScale = Global.getSettings().getScreenScaleMult();
        float visibleHeight = height * reveal;
        return GlScope.clip((int) Math.floor(x * screenScale), (int) Math.floor((y + height - visibleHeight) * screenScale),
                (int) Math.ceil(width * screenScale), (int) Math.ceil(visibleHeight * screenScale));
    }

    public void drawRevealLine(float x, float y, float width, float height, Color accent, float alphaMult) {
        float reveal = contentAlpha();
        if (reveal <= 0f || reveal >= 1f) {
            return;
        }
        float lineY = y + height - height * reveal;
        GLDraw.fillQuad(x, lineY - EDGE_THICKNESS / 2f, width, EDGE_THICKNESS, accent, (1f - reveal) * alphaMult);
    }

    private float phase(float start, float end) {
        return clamp((openProgress - start) / (end - start));
    }

    private static float easeOut(float t) {
        float inverse = 1f - t;
        return 1f - inverse * inverse * inverse;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
