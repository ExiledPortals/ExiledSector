package exiledsector.ui.decoration;

import exiledsector.ui.TreeViewport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepthLensTest {

    private static final float ZOOM = 0.5f;
    private static final TreeViewport VIEW = new TreeViewport(640f, 360f, ZOOM, 0f, 0f, 1280f, 720f);
    private static final float REACH = 400f;

    private static float[] place(DepthLens lens, float worldX, float worldY, float depth) {
        float[] out = new float[3];
        lens.place(worldX, worldY, depth, out);
        return out;
    }

    @Test
    void aFlatLensDrawsWhereTheViewportDoes() {
        float[] placed = place(DepthLens.flat(VIEW), 300f, -200f, 1f);
        assertEquals(VIEW.screenX(300f), placed[DepthLens.X], 1e-4f);
        assertEquals(VIEW.screenY(-200f), placed[DepthLens.Y], 1e-4f);
        assertEquals(1f, placed[DepthLens.SCALE]);
    }

    @Test
    void thePointUnderTheEyeGrowsButNeverMoves() {
        DepthLens lens = new DepthLens(VIEW, (x, y) -> 0f, 0.4f, REACH);
        float[] placed = place(lens, 0f, 0f, 1f);
        assertEquals(640f, placed[DepthLens.X], 1e-4f);
        assertEquals(360f, placed[DepthLens.Y], 1e-4f);
        assertEquals(1f / 0.6f, placed[DepthLens.SCALE], 1e-4f);
    }

    @Test
    void nearPointsSpreadAwayFromTheEyeAndFarPointsGatherTowardIt() {
        DepthLens lens = new DepthLens(VIEW, (x, y) -> 0f, 0.4f, REACH);
        float flatX = VIEW.screenX(600f);
        assertTrue(place(lens, 600f, 0f, 1f)[DepthLens.X] > flatX);
        assertTrue(place(lens, 600f, 0f, -1f)[DepthLens.X] < flatX);
    }

    @Test
    void theParallaxNeverPassesItsReach() {
        DepthLens lens = new DepthLens(VIEW, (x, y) -> 0f, 0.4f, REACH);
        float[] placed = place(lens, 100000f, 0f, 1f);
        assertTrue(placed[DepthLens.X] - VIEW.screenX(100000f) <= REACH * ZOOM + 1e-2f);
    }

    @Test
    void theScaleStopsShortOfTheCamera() {
        DepthLens lens = new DepthLens(VIEW, (x, y) -> 0f, 0.4f, REACH);
        assertEquals(1f / (1f - DepthLens.MOST_LIFT), lens.scale(10f), 1e-4f);
        assertEquals(1f / (1f + DepthLens.MOST_SINK), lens.scale(-10f), 1e-4f);
    }

    @Test
    void theFieldGivesEachPointItsDepth() {
        DepthLens lens = new DepthLens(VIEW, (x, y) -> x > 0f ? 1f : -1f, 0.4f, REACH);
        assertEquals(lens.scale(1f), place(lens, 50f, 0f, lens.depthAt(50f, 0f))[DepthLens.SCALE], 1e-4f);
        float[] fromField = new float[3];
        lens.place(-50f, 0f, fromField);
        assertEquals(lens.scale(-1f), fromField[DepthLens.SCALE], 1e-4f);
    }

    @Test
    void aRescaledLensDrawsTheSameSpotFromSmallerWorldUnits() {
        DepthLens lens = new DepthLens(VIEW, (x, y) -> x / 1000f, 0.4f, REACH);
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
