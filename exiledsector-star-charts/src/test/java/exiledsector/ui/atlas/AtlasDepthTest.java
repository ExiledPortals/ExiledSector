package exiledsector.ui.atlas;

import exiledsector.ui.TreeViewport;
import exiledsector.ui.decoration.DepthLens;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasDepthTest {

    private static final float CELL = AtlasMapCanvas.CELL_PIXELS;
    private static final float CLOSEST_SYSTEMS_CELLS = 0.65f;

    @Test
    void depthStaysInRangeAndReachesBothNearAndFar() {
        AtlasDepth depth = new AtlasDepth(42L);
        float nearest = 0f;
        float farthest = 0f;
        for (int gridX = -40; gridX <= 40; gridX++) {
            for (int gridY = -40; gridY <= 40; gridY++) {
                float sample = depth.depth(gridX * CELL, gridY * CELL);
                assertTrue(sample >= -1f && sample <= 1f);
                nearest = Math.max(nearest, sample);
                farthest = Math.min(farthest, sample);
            }
        }
        assertTrue(nearest > 0.6f);
        assertTrue(farthest < -0.6f);
    }

    @Test
    void theSameSeedGivesTheSameSky() {
        assertEquals(new AtlasDepth(7L).depth(1234f, -5678f), new AtlasDepth(7L).depth(1234f, -5678f));
    }

    @Test
    void depthGrowsStrongerAsTheCameraZoomsIn() {
        assertEquals(AtlasDepth.STRENGTH_ZOOMED_OUT, AtlasDepth.strength(AtlasMapCanvas.MIN_ZOOM), 1e-5f);
        assertEquals(AtlasDepth.STRENGTH_ZOOMED_IN, AtlasDepth.strength(AtlasMapCanvas.MAX_ZOOM), 1e-5f);
        assertTrue(AtlasDepth.strength(0.2f) > AtlasDepth.strength(0.1f));
    }

    @Test
    void farSystemsFadeAndNearOnesDoNot() {
        assertEquals(1f, AtlasDepth.fade(0.8f));
        assertEquals(1f - AtlasDepth.FAR_FADE, AtlasDepth.fade(-1f), 1e-5f);
    }

    @Test
    void neighbouringSystemsNeverCrowdTogetherWhereverTheCameraLooks() {
        AtlasDepth depth = new AtlasDepth(42L);
        float step = CLOSEST_SYSTEMS_CELLS * CELL;
        float[] first = new float[3];
        float[] second = new float[3];
        for (float zoom : new float[] {AtlasMapCanvas.MIN_ZOOM, 0.2f, AtlasMapCanvas.MAX_ZOOM}) {
            float halfWidth = 640f / zoom;
            for (int camera = -6; camera <= 6; camera++) {
                float cameraX = camera * 2.3f * CELL;
                TreeViewport view = new TreeViewport(640f - cameraX * zoom, 360f, zoom, 0f, 0f, 1280f, 720f);
                DepthLens lens = depth.lens(view);
                for (float worldX = cameraX - halfWidth; worldX < cameraX + halfWidth; worldX += step) {
                    for (float worldY = -3f * CELL; worldY <= 3f * CELL; worldY += step) {
                        lens.place(worldX, worldY, first);
                        lens.place(worldX + step, worldY, second);
                        float gap = (float) Math.hypot(second[DepthLens.X] - first[DepthLens.X], second[DepthLens.Y] - first[DepthLens.Y]);
                        assertTrue(gap > step * zoom * 0.5f, "systems crowded at zoom " + zoom + " x " + worldX + " y " + worldY);
                    }
                }
            }
        }
    }
}
