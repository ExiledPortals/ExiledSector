package exiledsector.ui;

public final class SmoothZoom {

    public static final float MAX_ZOOM = 2.5f;
    static final float MIN_ZOOM = 0.2f;
    static final float STEP = 1.1f;
    static final float RESPONSE_PER_SECOND = 12f;
    private static final float SNAP_LOG_DISTANCE = 0.0005f;

    private float currentZoom;
    private float zoomTarget;

    SmoothZoom(float zoom) {
        currentZoom = zoom;
        zoomTarget = zoom;
    }

    float current() {
        return currentZoom;
    }

    float target() {
        return zoomTarget;
    }

    void scroll(boolean in) {
        zoomTarget = in ? Math.min(MAX_ZOOM, zoomTarget * STEP) : Math.max(MIN_ZOOM, zoomTarget / STEP);
    }

    static float panAbout(float pan, float pivot, float zoomRatio) {
        return pivot + (pan - pivot) * zoomRatio;
    }

    boolean isSettledAtMinimum() {
        return currentZoom == MIN_ZOOM && zoomTarget == MIN_ZOOM;
    }

    void jumpTo(float zoom) {
        currentZoom = zoom;
        zoomTarget = zoom;
    }

    void advance(float amount) {
        if (currentZoom == zoomTarget) {
            return;
        }
        float remaining = (float) (Math.log(currentZoom / zoomTarget) * Math.exp(-RESPONSE_PER_SECOND * amount));
        currentZoom = Math.abs(remaining) < SNAP_LOG_DISTANCE ? zoomTarget : zoomTarget * (float) Math.exp(remaining);
    }
}
