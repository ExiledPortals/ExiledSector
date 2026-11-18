package exiledsector.ui.infestation;

import exiledsector.ui.TreeViewport;
import exiledsector.ui.util.TexturedQuadBatch;

import java.awt.Color;

final class ScreenPuffSink implements PuffSink {

    private TreeViewport viewport;
    private TexturedQuadBatch quads;
    private float sceneAlpha;

    void draw(TreeViewport frameViewport, TexturedQuadBatch sheetQuads, float frameAlpha) {
        viewport = frameViewport;
        quads = sheetQuads;
        sceneAlpha = frameAlpha;
    }

    @Override
    public void puff(float worldX, float worldY, float worldSize, float angleDeg, int cell, Color tint, float alpha) {
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
        quads.cellAtCenter(InfestationSprites.SHROUD_SHEET_SIDE, InfestationSprites.SHROUD_SHEET_SIDE, cell, screenX, screenY, screenSize, screenSize,
                angleDeg, tint, Math.min(1f, drawnAlpha));
    }
}
