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
    void whatTheCameraLooksAtStaysOnItsMapSpotAtEveryDepth() {
        for (float cameraX : new float[] {-3000f, 0f, 2500f}) {
            TreeViewport view = lookingAt(cameraX, 700f);
            DepthLens lens = new DepthLens(view, (x, y) -> 0f, 0.4f);
            for (float depth : new float[] {-1f, 0f, 1f}) {
                float[] placed = place(lens, cameraX, 700f, depth);
                assertEquals(view.screenX(cameraX), placed[DepthLens.X], 1e-3f);
                assertEquals(view.screenY(700f), placed[DepthLens.Y], 1e-3f);
            }
        }
    }

    @Test
    void nearPointsSlideFasterThanTheMapAndFarPointsSlowerAsTheCameraPans() {
        float[] slides = new float[3];
        float[] depths = {-1f, 0f, 1f};
        for (int index = 0; index < depths.length; index++) {
            float before = place(new DepthLens(lookingAt(0f, 0f), (x, y) -> 0f, 0.4f), 300f, 0f, depths[index])[DepthLens.X];
            float after = place(new DepthLens(lookingAt(200f, 0f), (x, y) -> 0f, 0.4f), 300f, 0f, depths[index])[DepthLens.X];
            slides[index] = before - after;
        }
        assertEquals(200f * ZOOM, slides[1], 1e-3f);
        assertTrue(slides[2] > slides[1] && slides[1] > slides[0]);
        DepthLens lens = new DepthLens(VIEW, (x, y) -> 0f, 0.4f);
        assertEquals(200f * ZOOM * lens.speed(1f), slides[2], 1e-3f);
        assertEquals(200f * ZOOM * lens.speed(-1f), slides[0], 1e-3f);
    }

    @Test
    void theWorldWindowReachesFarEnoughForTheFarthestGroundToFillTheScreen() {
        DepthLens lens = new DepthLens(lookingAt(-2000f, 900f), (x, y) -> 0f, 0.4f);
        float[] corner = new float[3];
        lens.place(lens.worldMinX(DepthLens.FARTHEST), lens.worldMinY(DepthLens.FARTHEST), DepthLens.FARTHEST, corner);
        assertEquals(0f, corner[DepthLens.X], 1e-2f);
        assertEquals(720f, corner[DepthLens.Y], 1e-2f);
        lens.place(lens.worldMaxX(DepthLens.FARTHEST), lens.worldMaxY(DepthLens.FARTHEST), DepthLens.FARTHEST, corner);
        assertEquals(1280f, corner[DepthLens.X], 1e-2f);
        assertEquals(0f, corner[DepthLens.Y], 1e-2f);
        assertTrue(lens.worldMaxX(DepthLens.FARTHEST) - lens.worldMinX(DepthLens.FARTHEST) > 1280f / ZOOM);
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
