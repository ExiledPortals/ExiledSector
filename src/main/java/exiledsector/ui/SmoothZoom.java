package exiledsector.ui;

public final class SmoothZoom {

    public static final float MAX_ZOOM = 2.5f;
    static final float MIN_ZOOM = 0.2f;
    static final float STEP = 1.1f;
    static final float RESPONSE_PER_SECOND = 12f;
    private static final float SNAP_LOG_DISTANCE = 0.0005f;

    private float current;
    private float target;

    SmoothZoom(float zoom) {
        current = zoom;
        target = zoom;
    }

    float current() {
        return current;
    }

    float target() {
        return target;
    }

    void scroll(boolean in) {
        target = in ? Math.min(MAX_ZOOM, target * STEP) : Math.max(MIN_ZOOM, target / STEP);
    }

    void jumpTo(float zoom) {
        current = zoom;
        target = zoom;
    }

    void advance(float amount) {
        if (current == target) {
            return;
        }
        float remaining = (float) (Math.log(current / target) * Math.exp(-RESPONSE_PER_SECOND * amount));
        current = Math.abs(remaining) < SNAP_LOG_DISTANCE ? target : target * (float) Math.exp(remaining);
    }
}
