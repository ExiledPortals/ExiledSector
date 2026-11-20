package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvoyOrbitTest {

    private static final float STAR_RADIUS = 45f;
    private static final float START_DEGREES = -30f;

    @Test
    void theConvoyCirclesOutsideTheStarAtOneDistance() {
        for (float seconds = 0f; seconds < 120f; seconds += 0.7f) {
            float distance = (float) Math.hypot(ConvoyOrbit.xAt(STAR_RADIUS, START_DEGREES, seconds), ConvoyOrbit.yAt(STAR_RADIUS, START_DEGREES, seconds));
            assertEquals(STAR_RADIUS * ConvoyOrbit.ORBIT_SHARE, distance, 0.01f);
            assertTrue(distance - STAR_RADIUS * ConvoyOrbit.FLEET_SPREAD_SHARE > STAR_RADIUS);
        }
    }

    @Test
    void theConvoyFacesTheWayItIsMoving() {
        float step = 0.001f;
        for (float seconds = 0.5f; seconds < 40f; seconds += 2.3f) {
            double moveX = ConvoyOrbit.xAt(STAR_RADIUS, START_DEGREES, seconds + step) - ConvoyOrbit.xAt(STAR_RADIUS, START_DEGREES, seconds - step);
            double moveY = ConvoyOrbit.yAt(STAR_RADIUS, START_DEGREES, seconds + step) - ConvoyOrbit.yAt(STAR_RADIUS, START_DEGREES, seconds - step);
            float travel = (float) Math.toDegrees(Math.atan2(moveY, moveX));
            float gap = Math.abs(((ConvoyOrbit.headingDegreesAt(START_DEGREES, seconds) - travel) % 360f + 540f) % 360f - 180f);
            assertTrue(gap < 1f, "at " + seconds + " off by " + gap);
        }
    }

    @Test
    void theAnimationClockWrappingAfterAnHourNeverMakesTheConvoyJump() {
        float hour = 3600f;
        assertEquals(0f, hour % ConvoyOrbit.SECONDS_PER_LAP, 0.0001f);
        assertEquals(ConvoyOrbit.xAt(STAR_RADIUS, START_DEGREES, 0f), ConvoyOrbit.xAt(STAR_RADIUS, START_DEGREES, hour), 0.01f);
        assertEquals(ConvoyOrbit.yAt(STAR_RADIUS, START_DEGREES, 0f), ConvoyOrbit.yAt(STAR_RADIUS, START_DEGREES, hour), 0.01f);
    }
}
