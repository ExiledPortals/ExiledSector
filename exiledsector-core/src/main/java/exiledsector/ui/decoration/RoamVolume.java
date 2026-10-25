package exiledsector.ui.decoration;

import java.util.Random;

record RoamVolume(float centerX, float centerY, float innerRadius, float outerRadius) {

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
}
