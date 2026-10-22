package exiledlib.ui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WobblyOrbitTest {

    private static float tiltedX(float centerX, float radius, float squash, float tiltDeg, float angle) {
        float localX = radius * (float) Math.cos(angle);
        float localY = radius * squash * (float) Math.sin(angle);
        double tilt = Math.toRadians(tiltDeg);
        return centerX + localX * (float) Math.cos(tilt) - localY * (float) Math.sin(tilt);
    }

    private static float tiltedY(float centerY, float radius, float squash, float tiltDeg, float angle) {
        float localX = radius * (float) Math.cos(angle);
        float localY = radius * squash * (float) Math.sin(angle);
        double tilt = Math.toRadians(tiltDeg);
        return centerY + localX * (float) Math.sin(tilt) + localY * (float) Math.cos(tilt);
    }

    @Test
    void aTiltedSquashedOrbitMatchesTheEscortLoopItReplaced() {
        WobblyOrbit orbit = new WobblyOrbit(0.08f, 5, 0f, 0.45f, 60f);
        WobblyOrbit.Point point = new WobblyOrbit.Point();
        for (int step = 0; step < 200; step++) {
            float angle = step * 0.137f - 7f;
            float radius = 0.72f * 900f * (1f + 0.08f * (float) Math.sin(5 * angle));
            orbit.locate(100f, -50f, 0.72f * 900f, angle, point);
            assertEquals(tiltedX(100f, radius, 0.45f, 60f, angle), point.x(), 0f);
            assertEquals(tiltedY(-50f, radius, 0.45f, 60f, angle), point.y(), 0f);
        }
    }

    @Test
    void aRoundOrbitWithAPhaseMatchesTheSerpentPathItReplaced() {
        WobblyOrbit orbit = new WobblyOrbit(0.15f, 7, 1.3f, 1f, 0f);
        WobblyOrbit.Point point = new WobblyOrbit.Point();
        for (int step = 0; step < 200; step++) {
            float angle = step * 0.091f - 3f;
            float radius = 1550f * (1f + 0.15f * (float) Math.sin(7 * angle + 1.3f));
            orbit.locate(1000f, -500f, 1550f, angle, point);
            assertEquals(1000f + radius * (float) Math.cos(angle), point.x(), 0f);
            assertEquals(-500f + radius * (float) Math.sin(angle), point.y(), 0f);
        }
    }
}
