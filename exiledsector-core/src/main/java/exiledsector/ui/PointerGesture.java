package exiledsector.ui;

import java.util.Objects;

public final class PointerGesture<M> {

    public enum Kind { NONE, TARGET, PAN }

    public static final float DRAG_THRESHOLD = 6f;

    private Kind gestureKind = Kind.NONE;
    private M pressMode;
    private Object pressedTarget;
    private boolean targetMayPan;
    private float pressX;
    private float pressY;
    private boolean pressCtrlDown;
    private boolean pressShiftDown;

    public void pressTarget(M mode, Object target, float x, float y, boolean ctrlDown, boolean shiftDown, boolean mayPan) {
        start(Kind.TARGET, mode, x, y);
        pressedTarget = target;
        pressCtrlDown = ctrlDown;
        pressShiftDown = shiftDown;
        targetMayPan = mayPan;
    }

    public void pressPan(M mode, float x, float y) {
        start(Kind.PAN, mode, x, y);
    }

    private void start(Kind kind, M mode, float x, float y) {
        clear();
        gestureKind = kind;
        pressMode = mode;
        pressX = x;
        pressY = y;
    }

    public boolean moveTo(float x, float y) {
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

    public boolean isStaleIn(M currentMode) {
        return gestureKind != Kind.NONE && currentMode != pressMode;
    }

    public boolean releasedOn(Object releasedTarget) {
        return gestureKind == Kind.TARGET && releasedTarget != null && Objects.equals(pressedTarget, releasedTarget);
    }

    public void clear() {
        gestureKind = Kind.NONE;
        pressMode = null;
        pressedTarget = null;
        targetMayPan = false;
        pressCtrlDown = false;
        pressShiftDown = false;
    }

    public Kind kind() {
        return gestureKind;
    }

    public boolean isActive() {
        return gestureKind != Kind.NONE;
    }

    public boolean isPanning() {
        return gestureKind == Kind.PAN;
    }

    public float pressX() {
        return pressX;
    }

    public float pressY() {
        return pressY;
    }

    public boolean ctrlDown() {
        return pressCtrlDown;
    }

    public boolean shiftDown() {
        return pressShiftDown;
    }
}
