package exiledsector.ui;

public final class SmoothZoom {

    public static final float MAX_ZOOM = 2.5f;
    public static final float MIN_ZOOM = 0.1f;
    static final float STEP = 1.1f;
    static final float RESPONSE_PER_SECOND = 12f;
    private static final float SNAP_LOG_DISTANCE = 0.0005f;

    private float currentZoom;
    private float zoomTarget;

    public SmoothZoom(float zoom) {
        currentZoom = zoom;
        zoomTarget = zoom;
    }

    public float current() {
        return currentZoom;
    }

    public float target() {
        return zoomTarget;
    }

    public void scroll(boolean in) {
        zoomTarget = in ? Math.min(MAX_ZOOM, zoomTarget * STEP) : Math.max(MIN_ZOOM, zoomTarget / STEP);
    }

    public static float panAbout(float pan, float pivot, float zoomRatio) {
        return pivot + (pan - pivot) * zoomRatio;
    }

    public boolean isSettledAtMinimum() {
        return currentZoom == MIN_ZOOM && zoomTarget == MIN_ZOOM;
    }

    public void jumpTo(float zoom) {
        currentZoom = zoom;
        zoomTarget = zoom;
    }

    public void aimAt(float zoom) {
        zoomTarget = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    public void advance(float amount) {
        if (currentZoom == zoomTarget) {
            return;
        }
        float remaining = (float) (Math.log(currentZoom / zoomTarget) * Math.exp(-RESPONSE_PER_SECOND * amount));
        currentZoom = Math.abs(remaining) < SNAP_LOG_DISTANCE ? zoomTarget : zoomTarget * (float) Math.exp(remaining);
    }
}
