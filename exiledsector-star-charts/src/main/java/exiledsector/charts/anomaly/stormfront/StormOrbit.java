package exiledsector.charts.anomaly.stormfront;

import org.lwjgl.util.vector.Vector2f;

record StormOrbit(float holeX, float holeY, float innerRadius, float outerRadius, float bandWidth, float startDegrees, float orbitDays)
        implements StormCourse {

    @Override
    public float bandLength() {
        return Math.max(0f, outerRadius - innerRadius);
    }

    float midRadius() {
        return (innerRadius + outerRadius) / 2f;
    }

    float spokeDegrees(float travelledDays) {
        if (!(orbitDays > 0f)) {
            return startDegrees;
        }
        return startDegrees + 360f * (travelledDays / orbitDays);
    }

    @Override
    public Vector2f centre(float travelledDays) {
        double spoke = Math.toRadians(spokeDegrees(travelledDays));
        return new Vector2f(holeX + (float) Math.cos(spoke) * midRadius(), holeY + (float) Math.sin(spoke) * midRadius());
    }

    @Override
    public float facingDegrees(float travelledDays) {
        return spokeDegrees(travelledDays) + 90f;
    }

    @Override
    public boolean isDone(float travelledDays) {
        return false;
    }

    @Override
    public boolean contains(Vector2f point, float pointRadius, float travelledDays) {
        if (point == null) {
            return false;
        }
        double spoke = Math.toRadians(spokeDegrees(travelledDays));
        float radialX = (float) Math.cos(spoke);
        float radialY = (float) Math.sin(spoke);
        float offsetX = point.x - holeX;
        float offsetY = point.y - holeY;
        float outward = offsetX * radialX + offsetY * radialY;
        float sideways = -offsetX * radialY + offsetY * radialX;
        return Math.abs(outward - midRadius()) <= bandLength() / 2f + pointRadius && Math.abs(sideways) <= bandWidth / 2f + pointRadius;
    }

    @Override
    public float daysUntilReaches(Vector2f point, float travelledDays) {
        if (point == null || !(orbitDays > 0f) || contains(point, 0f, travelledDays)) {
            return -1f;
        }
        float pointRadius = (float) Math.hypot(point.x - holeX, point.y - holeY);
        if (pointRadius < innerRadius || pointRadius > outerRadius) {
            return -1f;
        }
        float pointDegrees = (float) Math.toDegrees(Math.atan2(point.y - holeY, point.x - holeX));
        float leadingDegrees = spokeDegrees(travelledDays) + (float) Math.toDegrees(bandWidth / 2f / pointRadius);
        float gapDegrees = ((pointDegrees - leadingDegrees) % 360f + 360f) % 360f;
        return gapDegrees / 360f * orbitDays;
    }
}
