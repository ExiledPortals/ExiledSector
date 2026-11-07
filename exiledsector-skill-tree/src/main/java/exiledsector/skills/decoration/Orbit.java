package exiledsector.skills.decoration;

public record Orbit(float radius, float seconds, boolean clockwise, float startAngleDegrees) {

    public static final float MIN_SECONDS = 1f;

    public float direction() {
        return clockwise ? 1f : -1f;
    }

    public float secondsPerOrbit() {
        return Math.max(MIN_SECONDS, seconds);
    }

    public float angleDegAt(float atSeconds) {
        return startAngleDegrees + direction() * 360f * atSeconds / secondsPerOrbit();
    }
}
