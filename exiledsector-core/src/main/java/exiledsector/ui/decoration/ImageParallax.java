package exiledsector.ui.decoration;

final class ImageParallax {

    static final float REACH_PER_WIDTH = 0.25f;

    private ImageParallax() {
    }

    static float driftFraction(float distanceFromViewCenter, float parallax, float reach) {
        if (Float.compare(parallax, 1f) == 0 || distanceFromViewCenter <= 0f || reach <= 0f) return 0f;
        float drift = distanceFromViewCenter * (1f - parallax);
        return reach * (float) Math.tanh(drift / reach) / distanceFromViewCenter;
    }
}
