package exiledsector.ui.decoration;

public final class ConvoyCrossing {

    public static final float LANE_SHARE = 1.45f;
    public static final float LANE_SIDE_DEGREES = 90f;
    static final float TRACK_HALF_SHARE = 2.8f;
    static final float FADE_SHARE = 0.7f;
    static final float SPEED_SHARE = 0.16f;
    static final float WEAVE_SHARE = 0.05f;
    static final float WEAVE_RADIANS_PER_SECOND = 0.35f;
    private static final float WEAVE_PHASE_PER_SHIP = 1.9f;

    private final float starRadius;
    private final int shipCount;

    ConvoyCrossing(float starRadius, int shipCount) {
        this.starRadius = starRadius;
        this.shipCount = Math.max(shipCount, 1);
    }

    float trackHalfLength() {
        return starRadius * TRACK_HALF_SHARE;
    }

    float alongAt(int ship, float seconds) {
        float trackLength = trackHalfLength() * 2f;
        float travelled = seconds * starRadius * SPEED_SHARE - ship * trackLength / shipCount;
        float onTrack = travelled % trackLength;
        return (onTrack < 0f ? onTrack + trackLength : onTrack) - trackHalfLength();
    }

    float laneOffsetAt(int ship, float seconds) {
        return starRadius * (LANE_SHARE + WEAVE_SHARE * (float) Math.sin(seconds * WEAVE_RADIANS_PER_SECOND + ship * WEAVE_PHASE_PER_SHIP));
    }

    float visibilityAt(int ship, float seconds) {
        float fromEnd = trackHalfLength() - Math.abs(alongAt(ship, seconds));
        return Math.max(0f, Math.min(1f, fromEnd / (starRadius * FADE_SHARE)));
    }

    float xAt(int ship, float seconds, float headingDeg) {
        double heading = Math.toRadians(headingDeg);
        double laneSide = Math.toRadians(headingDeg + LANE_SIDE_DEGREES);
        return alongAt(ship, seconds) * (float) Math.cos(heading) + laneOffsetAt(ship, seconds) * (float) Math.cos(laneSide);
    }

    float yAt(int ship, float seconds, float headingDeg) {
        double heading = Math.toRadians(headingDeg);
        double laneSide = Math.toRadians(headingDeg + LANE_SIDE_DEGREES);
        return alongAt(ship, seconds) * (float) Math.sin(heading) + laneOffsetAt(ship, seconds) * (float) Math.sin(laneSide);
    }
}
