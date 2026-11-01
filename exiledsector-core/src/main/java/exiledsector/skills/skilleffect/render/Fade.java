package exiledsector.skills.skilleffect.render;

public final class Fade {

    private Fade() {
    }

    public static float step(float level, boolean rising, float stepSize) {
        return Math.max(0f, Math.min(1f, level + (rising ? stepSize : -stepSize)));
    }

    public static float smooth(float level) {
        return level * level * (3f - 2f * level);
    }

    public static float window(float from, float to, float value) {
        return smooth(Math.max(0f, Math.min(1f, (value - from) / (to - from))));
    }

    public static float cycle(float seconds, float period) {
        float cycle = seconds % period / period;
        return cycle < 0f ? cycle + 1f : cycle;
    }
}
