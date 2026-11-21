package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErraticOrbitTest {

    private static final float PLANET_RADIUS = 150f;
    private static final float STEP_SECONDS = 0.05f;

    @Test
    void aShipStaysOutsideThePlanetAndWithinItsTrackWhileItsRadiusWobbles() {
        ErraticOrbit orbit = ErraticOrbit.around(PLANET_RADIUS, new Random(7), false);
        float nearest = Float.MAX_VALUE;
        float farthest = 0f;
        for (float seconds = 0f; seconds < 120f; seconds += STEP_SECONDS) {
            float radius = orbit.radiusAt(seconds);
            nearest = Math.min(nearest, radius);
            farthest = Math.max(farthest, radius);
            assertEquals(radius, (float) Math.hypot(orbit.xAt(seconds), orbit.yAt(seconds)), 0.01f);
        }

        assertTrue(nearest >= orbit.minRadius() - 0.01f && farthest <= orbit.maxRadius() + 0.01f);
        assertTrue(orbit.minRadius() > PLANET_RADIUS, "min " + orbit.minRadius());
        assertTrue(farthest - nearest > PLANET_RADIUS * 0.1f, "wobble " + (farthest - nearest));
    }

    @Test
    void aShipAlwaysMovesOnInItsOwnDirectionButAtAChangingSpeed() {
        for (boolean clockwise : new boolean[] {false, true}) {
            ErraticOrbit orbit = ErraticOrbit.around(PLANET_RADIUS, new Random(11), clockwise);
            float slowest = Float.MAX_VALUE;
            float fastest = 0f;
            for (float seconds = 0f; seconds < 60f; seconds += STEP_SECONDS) {
                float speed = orbit.angularSpeedAt(seconds);
                assertEquals(clockwise, speed < 0f, "at " + seconds);
                slowest = Math.min(slowest, Math.abs(speed));
                fastest = Math.max(fastest, Math.abs(speed));
            }
            assertTrue(fastest > slowest * 1.5f, slowest + " to " + fastest);
            assertEquals(clockwise, orbit.clockwise());
        }
    }

    @Test
    void theAngleIsTheRunningTotalOfTheSpeed() {
        ErraticOrbit orbit = ErraticOrbit.around(PLANET_RADIUS, new Random(3), true);
        float step = 0.001f;
        for (float seconds = 0.5f; seconds < 30f; seconds += 1.7f) {
            float measured = (orbit.angleAt(seconds + step) - orbit.angleAt(seconds - step)) / (2f * step);
            assertEquals(orbit.angularSpeedAt(seconds), measured, 0.01f);
        }
    }

    @Test
    void theShipFacesTheWayItIsMoving() {
        ErraticOrbit orbit = ErraticOrbit.around(PLANET_RADIUS, new Random(5), false);
        float step = 0.001f;
        for (float seconds = 0.5f; seconds < 40f; seconds += 2.3f) {
            double moveX = orbit.xAt(seconds + step) - orbit.xAt(seconds - step);
            double moveY = orbit.yAt(seconds + step) - orbit.yAt(seconds - step);
            float travel = (float) Math.toDegrees(Math.atan2(moveY, moveX));
            float gap = Math.abs(((orbit.headingDegreesAt(seconds) - travel) % 360f + 540f) % 360f - 180f);
            assertTrue(gap < 2f, "at " + seconds + " off by " + gap);
        }
    }

    @Test
    void theSameSeedFliesTheSamePathAndShipsGoingOppositeWaysCrossPaths() {
        ErraticOrbit first = ErraticOrbit.around(PLANET_RADIUS, new Random(42), false);
        ErraticOrbit again = ErraticOrbit.around(PLANET_RADIUS, new Random(42), false);
        ErraticOrbit opposite = ErraticOrbit.around(PLANET_RADIUS, new Random(43), true);
        boolean crossed = false;
        for (float seconds = 0f; seconds < 120f; seconds += STEP_SECONDS) {
            assertEquals(first.xAt(seconds), again.xAt(seconds));
            assertEquals(first.yAt(seconds), again.yAt(seconds));
            if (Math.hypot(first.xAt(seconds) - opposite.xAt(seconds), first.yAt(seconds) - opposite.yAt(seconds)) < PLANET_RADIUS * 0.3f) {
                crossed = true;
            }
        }

        assertTrue(crossed);
        assertFalse(first.clockwise());
    }
}
