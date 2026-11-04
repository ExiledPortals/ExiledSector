package exiledsector.ui;

import org.lwjgl.input.Keyboard;

import java.util.function.IntPredicate;

final class KeyboardPan {

    static final float SCREEN_PIXELS_PER_SECOND = 1200f;
    private static final float DIAGONAL_SCALE = (float) (1.0 / Math.sqrt(2.0));
    private static final IntPredicate LIVE_KEYBOARD = keyCode -> Keyboard.isCreated() && Keyboard.isKeyDown(keyCode);

    private KeyboardPan() {
    }

    static void advance(CanvasCamera camera, float amount) {
        advance(camera, amount, LIVE_KEYBOARD);
    }

    static void advance(CanvasCamera camera, float amount, IntPredicate keyHeld) {
        float rightward = axis(keyHeld, Keyboard.KEY_D, Keyboard.KEY_RIGHT, Keyboard.KEY_A, Keyboard.KEY_LEFT);
        float upward = axis(keyHeld, Keyboard.KEY_W, Keyboard.KEY_UP, Keyboard.KEY_S, Keyboard.KEY_DOWN);
        if (rightward == 0f && upward == 0f) {
            return;
        }
        float step = SCREEN_PIXELS_PER_SECOND * amount * (rightward != 0f && upward != 0f ? DIAGONAL_SCALE : 1f);
        camera.panBy(-rightward * step, -upward * step);
    }

    private static float axis(IntPredicate keyHeld, int positiveLetter, int positiveArrow, int negativeLetter, int negativeArrow) {
        boolean positive = keyHeld.test(positiveLetter) || keyHeld.test(positiveArrow);
        boolean negative = keyHeld.test(negativeLetter) || keyHeld.test(negativeArrow);
        return (positive ? 1f : 0f) - (negative ? 1f : 0f);
    }
}
