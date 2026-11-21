package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.ui.util.HoloBeams;

import java.awt.Color;

final class AtlasSpotlight {

    static final int RING_COUNT = 3;
    static final float RING_SECONDS = 0.8f;
    static final float RING_STAGGER_SECONDS = 0.35f;
    static final float START_RADIUS = 260f;
    static final float MIN_END_RADIUS = 22f;
    static final float END_RADIUS_SHARE = 0.75f;
    static final float FADE_IN_SHARE = 0.25f;
    static final float FADE_OUT_SHARE = 0.15f;
    static final float GLOW_WIDTH = 16f;
    static final float CORE_WIDTH = 6f;
    static final float GLOW_ALPHA = 0.7f;
    static final Color CORE_COLOUR = Color.WHITE;

    private AtlasCell cell;
    private float elapsedSeconds;

    void start(AtlasCell target) {
        cell = target;
        elapsedSeconds = 0f;
    }

    AtlasCell cell() {
        return cell;
    }

    void advance(float amount, boolean waiting) {
        if (cell == null || waiting) {
            return;
        }
        elapsedSeconds += amount;
        if (elapsedSeconds >= totalSeconds()) {
            cell = null;
        }
    }

    static float totalSeconds() {
        return RING_STAGGER_SECONDS * (RING_COUNT - 1) + RING_SECONDS;
    }

    void render(float centreX, float centreY, float nodeFootprint, Color accent, float alpha) {
        if (cell == null || alpha <= 0f) {
            return;
        }
        float endRadius = Math.max(MIN_END_RADIUS, nodeFootprint * END_RADIUS_SHARE);
        float startRadius = Math.max(START_RADIUS, endRadius * 3f);
        for (int ring = 0; ring < RING_COUNT; ring++) {
            float progress = (elapsedSeconds - ring * RING_STAGGER_SECONDS) / RING_SECONDS;
            if (progress <= 0f || progress >= 1f) {
                continue;
            }
            float eased = 1f - (1f - progress) * (1f - progress);
            float radius = startRadius + (endRadius - startRadius) * eased;
            float ringAlpha = ringAlpha(progress) * alpha;
            HoloBeams.circle(centreX, centreY, radius, GLOW_WIDTH, accent, GLOW_ALPHA * ringAlpha);
            HoloBeams.circle(centreX, centreY, radius, CORE_WIDTH, CORE_COLOUR, ringAlpha);
        }
    }

    static float ringAlpha(float progress) {
        float fadeIn = Math.min(1f, progress / FADE_IN_SHARE);
        float fadeOut = Math.min(1f, (1f - progress) / FADE_OUT_SHARE);
        return Math.min(fadeIn, fadeOut);
    }
}
