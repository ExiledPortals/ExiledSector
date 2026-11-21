package exiledsector.ui.decoration;

import java.util.Random;

record GroupPose(float x, float y, float headingOffsetDeg, float swayDeg, float swayPhase) {

    static final float SWAY_RADIANS_PER_SECOND = (float) (Math.PI * 2.0 / 40.0);
    private static final float FULL_TURN = (float) (Math.PI * 2.0);

    static GroupPose centred(float swayDeg, Random random) {
        return new GroupPose(0f, 0f, 0f, swayDeg, random.nextFloat() * FULL_TURN);
    }

    static GroupPose at(FlagshipFormation.Slot spot, float baseHeadingDeg, float headingOffsetDeg, float swayDeg, Random random) {
        return new GroupPose(spot.xAt(0f, baseHeadingDeg), spot.yAt(0f, baseHeadingDeg), headingOffsetDeg, swayDeg, random.nextFloat() * FULL_TURN);
    }

    float headingAt(float baseHeadingDeg, float seconds) {
        return baseHeadingDeg + headingOffsetDeg + swayDeg * (float) Math.sin(SWAY_RADIANS_PER_SECOND * seconds + swayPhase);
    }
}
