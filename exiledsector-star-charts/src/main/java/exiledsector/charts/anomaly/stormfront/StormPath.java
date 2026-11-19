package exiledsector.charts.anomaly.stormfront;

import org.lwjgl.util.vector.Vector2f;

record StormPath(float angleDegrees, float edgeRadius, float bandWidth, float crossingDays) implements StormCourse {

    float directionX() {
        return (float) Math.cos(Math.toRadians(angleDegrees));
    }

    float directionY() {
        return (float) Math.sin(Math.toRadians(angleDegrees));
    }

    @Override
    public float bandLength() {
        return edgeRadius * 2f;
    }

    float startOffset() {
        return -(edgeRadius + bandWidth / 2f);
    }

    float endOffset() {
        return edgeRadius + bandWidth / 2f;
    }

    float progress(float elapsedDays) {
        if (!(crossingDays > 0f)) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, elapsedDays / crossingDays));
    }

    @Override
    public boolean isDone(float elapsedDays) {
        return progress(elapsedDays) >= 1f;
    }

    float offset(float elapsedDays) {
        return startOffset() + (endOffset() - startOffset()) * progress(elapsedDays);
    }

    @Override
    public Vector2f centre(float elapsedDays) {
        float offset = offset(elapsedDays);
        return new Vector2f(directionX() * offset, directionY() * offset);
    }

    float along(Vector2f point) {
        return point.x * directionX() + point.y * directionY();
    }

    float across(Vector2f point) {
        return -point.x * directionY() + point.y * directionX();
    }

    @Override
    public boolean contains(Vector2f point, float pointRadius, float elapsedDays) {
        if (point == null || isDone(elapsedDays)) {
            return false;
        }
        return Math.abs(along(point) - offset(elapsedDays)) <= bandWidth / 2f + pointRadius
                && Math.abs(across(point)) <= bandLength() / 2f + pointRadius;
    }

    @Override
    public float daysUntilReaches(Vector2f point, float elapsedDays) {
        if (point == null || Math.abs(across(point)) > bandLength() / 2f) {
            return -1f;
        }
        float leadingEdgeNeeded = along(point) - bandWidth / 2f;
        float travelled = (leadingEdgeNeeded - startOffset()) / (endOffset() - startOffset());
        float days = travelled * crossingDays - elapsedDays;
        return days > 0f ? days : -1f;
    }

    float daysLeft(float elapsedDays) {
        return Math.max(0f, crossingDays - elapsedDays);
    }

    @Override
    public float facingDegrees(float elapsedDays) {
        return angleDegrees;
    }
}
