package exiledsector.ui;

import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyboardPanTest {

    private static final float STEP = KeyboardPan.SCREEN_PIXELS_PER_SECOND;

    private static CanvasCamera pannedWith(Integer... heldKeys) {
        Set<Integer> held = Set.of(heldKeys);
        CanvasCamera camera = new CanvasCamera();
        KeyboardPan.advance(camera, 1f, held::contains);
        return camera;
    }

    @Test
    void eachKeyMovesTheViewTowardsItsSide() {
        assertEquals(-STEP, pannedWith(Keyboard.KEY_W).centreTreeY(), 1e-3f);
        assertEquals(STEP, pannedWith(Keyboard.KEY_S).centreTreeY(), 1e-3f);
        assertEquals(-STEP, pannedWith(Keyboard.KEY_A).centreTreeX(), 1e-3f);
        assertEquals(STEP, pannedWith(Keyboard.KEY_D).centreTreeX(), 1e-3f);
    }

    @Test
    void arrowKeysPanLikeTheirLetters() {
        assertEquals(-STEP, pannedWith(Keyboard.KEY_UP).centreTreeY(), 1e-3f);
        assertEquals(STEP, pannedWith(Keyboard.KEY_DOWN).centreTreeY(), 1e-3f);
        assertEquals(-STEP, pannedWith(Keyboard.KEY_LEFT).centreTreeX(), 1e-3f);
        assertEquals(STEP, pannedWith(Keyboard.KEY_RIGHT).centreTreeX(), 1e-3f);
    }

    @Test
    void anArrowAndItsLetterTogetherDoNotDoubleTheSpeed() {
        assertEquals(STEP, pannedWith(Keyboard.KEY_D, Keyboard.KEY_RIGHT).centreTreeX(), 1e-3f);
    }

    @Test
    void opposingKeysCancelOut() {
        CanvasCamera camera = pannedWith(Keyboard.KEY_W, Keyboard.KEY_S, Keyboard.KEY_A, Keyboard.KEY_D);

        assertEquals(0f, camera.centreTreeX(), 1e-3f);
        assertEquals(0f, camera.centreTreeY(), 1e-3f);
    }

    @Test
    void diagonalsMoveAtTheSameSpeedAsStraightLines() {
        CanvasCamera camera = pannedWith(Keyboard.KEY_W, Keyboard.KEY_D);

        float distance = (float) Math.hypot(camera.centreTreeX(), camera.centreTreeY());
        assertEquals(STEP, distance, 1e-2f);
    }

    @Test
    void panSpeedIsInScreenPixelsSoItCoversMoreTreeWhenZoomedOut() {
        CanvasCamera camera = new CanvasCamera();
        camera.jumpZoomTo(0.5f);

        KeyboardPan.advance(camera, 1f, Set.of(Keyboard.KEY_D)::contains);

        assertEquals(STEP * 2f, camera.centreTreeX(), 1e-2f);
    }
}
