package exiledsector.charts.anomaly.stormfront;

import org.lwjgl.util.vector.Vector2f;

sealed interface StormCourse permits StormPath, StormOrbit {

    float bandWidth();

    float bandLength();

    Vector2f centre(float travelledDays);

    float facingDegrees(float travelledDays);

    boolean isDone(float travelledDays);

    boolean contains(Vector2f point, float pointRadius, float travelledDays);

    float daysUntilReaches(Vector2f point, float travelledDays);

    default float halfDiagonal() {
        return (float) Math.hypot(bandWidth() / 2f, bandLength() / 2f);
    }
}
