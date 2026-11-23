package exiledsector.ui.decoration;

import java.util.Objects;
import java.util.Random;

final class RoamVolume {

    private final float innerRadius;
    private final float outerRadius;
    private float centerX;
    private float centerY;

    RoamVolume(float centerX, float centerY, float innerRadius, float outerRadius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
    }

    float centerX() {
        return centerX;
    }

    float centerY() {
        return centerY;
    }

    float innerRadius() {
        return innerRadius;
    }

    float outerRadius() {
        return outerRadius;
    }

    void moveTo(float newCenterX, float newCenterY) {
        centerX = newCenterX;
        centerY = newCenterY;
    }

    void randomPoint(Random random, float[] pointOut) {
        float radiusSquared = innerRadius * innerRadius + random.nextFloat() * (outerRadius * outerRadius - innerRadius * innerRadius);
        float radius = (float) Math.sqrt(radiusSquared);
        double angle = random.nextDouble() * Math.PI * 2;
        pointOut[0] = centerX + radius * (float) Math.cos(angle);
        pointOut[1] = centerY + radius * (float) Math.sin(angle);
    }

    boolean segmentClearsStar(float fromX, float fromY, float toX, float toY) {
        float segmentX = toX - fromX;
        float segmentY = toY - fromY;
        float lengthSquared = segmentX * segmentX + segmentY * segmentY;
        float along = lengthSquared <= 0f ? 0f
                : Math.max(0f, Math.min(1f, ((centerX - fromX) * segmentX + (centerY - fromY) * segmentY) / lengthSquared));
        float closestX = fromX + segmentX * along - centerX;
        float closestY = fromY + segmentY * along - centerY;
        return closestX * closestX + closestY * closestY >= innerRadius * innerRadius;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RoamVolume volume && Float.compare(centerX, volume.centerX) == 0 && Float.compare(centerY, volume.centerY) == 0
                && Float.compare(innerRadius, volume.innerRadius) == 0 && Float.compare(outerRadius, volume.outerRadius) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(centerX, centerY, innerRadius, outerRadius);
    }

    @Override
    public String toString() {
        return "RoamVolume[" + centerX + ", " + centerY + ", " + innerRadius + ", " + outerRadius + "]";
    }
}
