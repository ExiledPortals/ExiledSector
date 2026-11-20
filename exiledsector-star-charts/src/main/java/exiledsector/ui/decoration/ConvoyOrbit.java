package exiledsector.ui.decoration;

public final class ConvoyOrbit {

    public static final float ORBIT_SHARE = 2.2f;
    static final float FLEET_SPREAD_SHARE = 0.6f;
    static final float SECONDS_PER_LAP = 45f;
    private static final float TANGENT_DEGREES = 90f;

    private ConvoyOrbit() {
    }

    public static float angleDegreesAt(float startDegrees, float seconds) {
        float lapShare = (seconds % SECONDS_PER_LAP) / SECONDS_PER_LAP;
        return startDegrees + 360f * lapShare;
    }

    static float headingDegreesAt(float startDegrees, float seconds) {
        return angleDegreesAt(startDegrees, seconds) + TANGENT_DEGREES;
    }

    static float xAt(float starRadius, float startDegrees, float seconds) {
        return starRadius * ORBIT_SHARE * (float) Math.cos(Math.toRadians(angleDegreesAt(startDegrees, seconds)));
    }

    static float yAt(float starRadius, float startDegrees, float seconds) {
        return starRadius * ORBIT_SHARE * (float) Math.sin(Math.toRadians(angleDegreesAt(startDegrees, seconds)));
    }
}
