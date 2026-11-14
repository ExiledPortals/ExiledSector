package exiledsector.ui;

public final class CanvasDrag<M> {

    public enum Move { NONE, STARTED_PANNING, PANNED }

    private final PointerGesture<M> gesture = new PointerGesture<>();
    private final CanvasCamera camera;

    public CanvasDrag(CanvasCamera camera) {
        this.camera = camera;
    }

    public PointerGesture<M> gesture() {
        return gesture;
    }

    public void pressPan(M mode, float x, float y) {
        gesture.pressPan(mode, x, y);
        camera.startDrag();
    }

    public Move pointerMoved(float x, float y, float dx, float dy) {
        if (gesture.moveTo(x, y)) {
            camera.startDrag();
            camera.dragBy(x - gesture.pressX(), y - gesture.pressY());
            return Move.STARTED_PANNING;
        }
        return camera.dragBy(dx, dy) ? Move.PANNED : Move.NONE;
    }

    public void release() {
        gesture.clear();
        camera.stopDrag();
    }

    public boolean dropIfStaleIn(M currentMode) {
        if (!gesture.isStaleIn(currentMode)) {
            return false;
        }
        if (gesture.isPanning()) {
            camera.stopDrag();
        }
        gesture.clear();
        return true;
    }
}
