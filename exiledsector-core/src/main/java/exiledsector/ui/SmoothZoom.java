package exiledsector.ui;

public final class SmoothZoom {

    public static final float MAX_ZOOM = 2.5f;
    public static final float MIN_ZOOM = 0.1f;
    static final float STEP = 1.1f;
    static final float RESPONSE_PER_SECOND = 12f;
    private static final float SNAP_LOG_DISTANCE = 0.0005f;

    private final float minZoom;
    private final float maxZoom;
    private float currentZoom;
    private float zoomTarget;

    public SmoothZoom(float zoom) {
        this(zoom, MAX_ZOOM);
    }

    public SmoothZoom(float zoom, float maxZoom) {
        this(zoom, MIN_ZOOM, maxZoom);
    }

    public SmoothZoom(float zoom, float minZoom, float maxZoom) {
        this.minZoom = minZoom;
        this.maxZoom = maxZoom;
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
        zoomTarget = in ? Math.min(maxZoom, zoomTarget * STEP) : Math.max(minZoom, zoomTarget / STEP);
    }

    public static float panAbout(float pan, float pivot, float zoomRatio) {
        return pivot + (pan - pivot) * zoomRatio;
    }

    public boolean isSettledAtMinimum() {
        return currentZoom == minZoom && zoomTarget == minZoom;
    }

    public void jumpTo(float zoom) {
        currentZoom = zoom;
        zoomTarget = zoom;
    }

    public void aimAt(float zoom) {
        zoomTarget = Math.max(minZoom, Math.min(maxZoom, zoom));
    }

    public void advance(float amount) {
        if (currentZoom == zoomTarget) {
            return;
        }
        float remaining = (float) (Math.log(currentZoom / zoomTarget) * Math.exp(-RESPONSE_PER_SECOND * amount));
        currentZoom = Math.abs(remaining) < SNAP_LOG_DISTANCE ? zoomTarget : zoomTarget * (float) Math.exp(remaining);
    }
}
