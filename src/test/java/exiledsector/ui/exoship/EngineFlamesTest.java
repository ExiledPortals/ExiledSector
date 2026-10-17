package exiledsector.ui.exoship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineFlamesTest {

    private static final float EPSILON = 0.0001f;

    @Test
    void theFlameTailPointsStraightBackFromTheShip() {
        float facingDeg = 30f;
        double tailRad = Math.toRadians(EngineFlames.flameAngle(facingDeg));
        double forwardRad = Math.toRadians(-facingDeg);

        assertEquals(-Math.cos(forwardRad), Math.cos(tailRad), EPSILON);
        assertEquals(-Math.sin(forwardRad), Math.sin(tailRad), EPSILON);
    }

    @Test
    void aFlameIsNeverStretchedPastItsLengthToWidthLimit() {
        assertEquals(10f * EngineFlames.MAX_LENGTH_PER_WIDTH, EngineFlames.cappedLength(500f, 10f), EPSILON);
        assertEquals(20f, EngineFlames.cappedLength(20f, 10f), EPSILON);
    }

    @Test
    void theFlickerStaysWithinAFewPercentAndKeepsChanging() {
        float lowest = Float.MAX_VALUE;
        float highest = -Float.MAX_VALUE;
        for (int frame = 0; frame < 600; frame++) {
            float flicker = EngineFlames.flicker(frame / 60f, 2);
            lowest = Math.min(lowest, flicker);
            highest = Math.max(highest, flicker);
        }

        assertTrue(lowest >= 0.89f && highest <= 1.11f, lowest + ".." + highest);
        assertTrue(highest - lowest > 0.1f, lowest + ".." + highest);
    }

    @Test
    void differentEnginesFlickerOutOfStep() {
        assertTrue(Math.abs(EngineFlames.flicker(1.3f, 0) - EngineFlames.flicker(1.3f, 1)) > EPSILON);
    }
}
