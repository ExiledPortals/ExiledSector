package exiledsector.ui.decoration;

import exiledsector.ui.TreeViewport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepthLensTest {

    private static final float ZOOM = 0.5f;
    private static final TreeViewport VIEW = new TreeViewport(640f, 360f, ZOOM, 0f, 0f, 1280f, 720f);

    private static float[] place(DepthLens lens, float worldX, float worldY, float depth) {
        float[] out = new float[3];
        lens.place(worldX, worldY, depth, out);
        return out;
    }

    private static TreeViewport lookingAt(float cameraX, float cameraY) {
        return new TreeViewport(640f - cameraX * ZOOM, 360f + cameraY * ZOOM, ZOOM, 0f, 0f, 1280f, 720f);
    }

    @Test
    void aFlatLensDrawsWhereTheViewportDoes() {
        float[] placed = place(DepthLens.flat(VIEW), 300f, -200f, 1f);
        assertEquals(VIEW.screenX(300f), placed[DepthLens.X], 1e-4f);
        assertEquals(VIEW.screenY(-200f), placed[DepthLens.Y], 1e-4f);
        assertEquals(1f, placed[DepthLens.SCALE]);
    }

    @Test
    void nearAndFarPointsStayWhereTheViewportPutsThemWhereverTheCameraLooks() {
        for (float cameraX : new float[] {-3000f, 0f, 2500f}) {
            TreeViewport view = lookingAt(cameraX, 700f);
            DepthLens lens = new DepthLens(view, (x, y) -> 0f, 0.4f);
            for (float depth : new float[] {-1f, 0f, 1f}) {
                float[] placed = place(lens, 600f, -400f, depth);
                assertEquals(view.screenX(600f), placed[DepthLens.X], 1e-3f);
                assertEquals(view.screenY(-400f), placed[DepthLens.Y], 1e-3f);
            }
        }
    }

    @Test
    void nearPointsGrowAndFarPointsShrink() {
        DepthLens lens = new DepthLens(VIEW, (x, y) -> 0f, 0.4f);
        assertEquals(1f / 0.6f, place(lens, 0f, 0f, 1f)[DepthLens.SCALE], 1e-4f);
        assertTrue(place(lens, 0f, 0f, -1f)[DepthLens.SCALE] < 1f);
    }

    @Test
    void theScaleStopsShortOfTheCamera() {
        DepthLens lens = new DepthLens(VIEW, (x, y) -> 0f, 0.4f);
        assertEquals(1f / (1f - DepthLens.MOST_LIFT), lens.scale(10f), 1e-4f);
        assertEquals(1f / (1f + DepthLens.MOST_SINK), lens.scale(-10f), 1e-4f);
    }

    @Test
    void theFieldGivesEachPointItsDepth() {
        DepthLens lens = new DepthLens(VIEW, (x, y) -> x > 0f ? 1f : -1f, 0.4f);
        assertEquals(lens.scale(1f), place(lens, 50f, 0f, lens.depthAt(50f, 0f))[DepthLens.SCALE], 1e-4f);
        float[] fromField = new float[3];
        lens.place(-50f, 0f, fromField);
        assertEquals(lens.scale(-1f), fromField[DepthLens.SCALE], 1e-4f);
    }

    @Test
    void theLensKnowsWhereTheCameraLooks() {
        DepthLens lens = new DepthLens(lookingAt(-2000f, 900f), (x, y) -> 0f, 0.4f);
        assertEquals(-2000f, lens.cameraX(), 1e-2f);
        assertEquals(900f, lens.cameraY(), 1e-2f);
    }

    @Test
    void aRescaledLensDrawsTheSameSpotFromSmallerWorldUnits() {
        DepthLens lens = new DepthLens(VIEW, (x, y) -> x / 1000f, 0.4f);
        DepthLens halved = lens.rescaled(2f);
        float[] full = new float[3];
        float[] half = new float[3];
        lens.place(800f, 300f, full);
        halved.place(400f, 150f, half);
        assertEquals(full[DepthLens.X], half[DepthLens.X], 1e-3f);
        assertEquals(full[DepthLens.Y], half[DepthLens.Y], 1e-3f);
        assertEquals(full[DepthLens.SCALE], half[DepthLens.SCALE], 1e-5f);
    }
}
