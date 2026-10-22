package exiledlib.ui.util;

public final class Fade {

    private Fade() {
    }

    public static float step(float level, boolean rising, float stepSize) {
        return Math.max(0f, Math.min(1f, level + (rising ? stepSize : -stepSize)));
    }

    public static float smooth(float level) {
        return level * level * (3f - 2f * level);
    }
}
