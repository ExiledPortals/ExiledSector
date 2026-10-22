package exiledsector.ui;

import java.util.Objects;

final class PointerGesture {

    enum Kind { NONE, TARGET, PAN }

    static final float DRAG_THRESHOLD = 6f;

    private Kind gestureKind = Kind.NONE;
    private CanvasMode pressMode;
    private Object pressedTarget;
    private boolean targetMayPan;
    private float pressX;
    private float pressY;
    private boolean pressCtrlDown;
    private boolean pressShiftDown;

    void pressTarget(CanvasMode mode, Object target, float x, float y, boolean ctrlDown, boolean shiftDown, boolean mayPan) {
        start(Kind.TARGET, mode, x, y);
        pressedTarget = target;
        pressCtrlDown = ctrlDown;
        pressShiftDown = shiftDown;
        targetMayPan = mayPan;
    }

    void pressPan(CanvasMode mode, float x, float y) {
        start(Kind.PAN, mode, x, y);
    }

    private void start(Kind kind, CanvasMode mode, float x, float y) {
        clear();
        gestureKind = kind;
        pressMode = mode;
        pressX = x;
        pressY = y;
    }

    boolean moveTo(float x, float y) {
        if (gestureKind != Kind.TARGET || !beyondThreshold(x, y)) {
            return false;
        }
        if (!targetMayPan) {
            clear();
            return false;
        }
        gestureKind = Kind.PAN;
        pressedTarget = null;
        return true;
    }

    private boolean beyondThreshold(float x, float y) {
        float dx = x - pressX;
        float dy = y - pressY;
        return dx * dx + dy * dy > DRAG_THRESHOLD * DRAG_THRESHOLD;
    }

    boolean isStaleIn(CanvasMode currentMode) {
        return gestureKind != Kind.NONE && currentMode != pressMode;
    }

    boolean releasedOn(Object releasedTarget) {
        return gestureKind == Kind.TARGET && releasedTarget != null && Objects.equals(pressedTarget, releasedTarget);
    }

    void clear() {
        gestureKind = Kind.NONE;
        pressMode = null;
        pressedTarget = null;
        targetMayPan = false;
        pressCtrlDown = false;
        pressShiftDown = false;
    }

    Kind kind() {
        return gestureKind;
    }

    boolean isActive() {
        return gestureKind != Kind.NONE;
    }

    boolean isPanning() {
        return gestureKind == Kind.PAN;
    }

    float pressX() {
        return pressX;
    }

    float pressY() {
        return pressY;
    }

    boolean ctrlDown() {
        return pressCtrlDown;
    }

    boolean shiftDown() {
        return pressShiftDown;
    }
}
