package exiledsector.ui.decoration;

import exiledsector.skills.layout.RingBelt;
import exiledsector.skills.layout.Star;
import exiledsector.skills.tags.SkillTags;

import java.util.List;
import java.util.Random;

record CoreVolume(float centerX, float centerY, float innerRadius, float outerRadius) {

    static final float DEFAULT_OUTER_RADIUS = 4000f;
    static final float DEFAULT_STAR_RADIUS = 150f;
    static final float STAR_CLEARANCE_MULT = 2.5f;

    static CoreVolume of(List<Star> stars, List<RingBelt> ringBelts) {
        Star coreStar = stars.stream().filter(star -> SkillTags.CORE_REGION.equals(star.getRegion())).findFirst().orElse(null);
        float centerX = coreStar == null ? 0f : coreStar.getX();
        float centerY = coreStar == null ? 0f : coreStar.getY();
        float starRadius = coreStar == null ? DEFAULT_STAR_RADIUS : coreStar.getRadius();
        float outerRadius = 0f;
        for (RingBelt belt : ringBelts) {
            if (SkillTags.CORE_REGION.equals(belt.getRegion())) {
                float beltCenterDistance = (float) Math.hypot(belt.getX() - centerX, belt.getY() - centerY);
                outerRadius = Math.max(outerRadius, beltCenterDistance + belt.getOuterRadius());
            }
        }
        float innerRadius = starRadius * STAR_CLEARANCE_MULT;
        if (outerRadius <= innerRadius) {
            outerRadius = Math.max(DEFAULT_OUTER_RADIUS, innerRadius * 2f);
        }
        return new CoreVolume(centerX, centerY, innerRadius, outerRadius);
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
}
