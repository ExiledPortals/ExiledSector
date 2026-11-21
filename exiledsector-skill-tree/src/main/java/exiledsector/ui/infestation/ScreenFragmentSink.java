package exiledsector.ui.infestation;

import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;

final class ScreenFragmentSink implements FragmentSink {

    static final Color FRAGMENT_TINT = Color.WHITE;
    static final float FRAGMENT_GLOW_SIZE_MULT = 3f;
    static final float FRAGMENT_GLOW_ALPHA = 0.35f;

    private final Color glowTint;
    private TreeViewport viewport;
    private TexturedQuadBatch quads;
    private float sceneAlpha;
    private boolean drawingGlows;

    ScreenFragmentSink(Color glowTint) {
        this.glowTint = glowTint;
    }

    void drawGlows(TreeViewport frameViewport, TexturedQuadBatch glowQuads, float frameAlpha) {
        bind(frameViewport, glowQuads, frameAlpha, true);
    }

    void drawFragments(TreeViewport frameViewport, TexturedQuadBatch fragmentQuads, float frameAlpha) {
        bind(frameViewport, fragmentQuads, frameAlpha, false);
    }

    private void bind(TreeViewport frameViewport, TexturedQuadBatch frameQuads, float frameAlpha, boolean glows) {
        viewport = frameViewport;
        quads = frameQuads;
        sceneAlpha = frameAlpha;
        drawingGlows = glows;
    }

    @Override
    public void fragment(float worldX, float worldY, float worldSize, float angleDeg, float brightness) {
        if (drawingGlows) {
            drawQuad(worldX, worldY, worldSize * FRAGMENT_GLOW_SIZE_MULT, 0f, glowTint, brightness * FRAGMENT_GLOW_ALPHA);
        } else {
            drawQuad(worldX, worldY, worldSize, angleDeg, FRAGMENT_TINT, brightness);
        }
    }

    @Override
    public void glow(float worldX, float worldY, float worldSize, float brightness) {
        if (drawingGlows) {
            drawQuad(worldX, worldY, worldSize, 0f, glowTint, brightness * FRAGMENT_GLOW_ALPHA);
        }
    }

    private void drawQuad(float worldX, float worldY, float worldSize, float angleDeg, Color tint, float alpha) {
        float drawnAlpha = sceneAlpha * alpha;
        if (drawnAlpha <= 0f) {
            return;
        }
        float screenX = viewport.screenX(worldX);
        float screenY = viewport.screenY(worldY);
        float screenSize = worldSize * viewport.zoom();
        if (!viewport.isVisible(screenX, screenY, screenSize)) {
            return;
        }
        quads.quadAtCenter(screenX, screenY, screenSize, screenSize, angleDeg, tint, Math.min(1f, drawnAlpha));
    }
}
