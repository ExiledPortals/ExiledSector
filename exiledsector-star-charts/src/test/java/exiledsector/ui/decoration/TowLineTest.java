package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TowLineTest {

    private static final float DELTA = 0.01f;

    @Test
    void aStraightTowTrailsDirectlyBehindTheTug() {
        GroupPose tug = new GroupPose(10f, 5f, 0f, 0f, 0f);
        TowLine tow = new TowLine(40f, 1.5f);

        assertEquals(10f, tow.xAt(tug, 90f, 7f), DELTA);
        assertEquals(-35f, tow.yAt(tug, 90f, 7f), DELTA);
    }

    @Test
    void theTowedHullKeepsItsLineLengthAndLagsBehindTheTugsTurns() {
        GroupPose tug = new GroupPose(0f, 0f, 0f, 20f, 0f);
        TowLine tow = new TowLine(40f, 2f);
        for (float seconds = 0f; seconds < 60f; seconds += 0.7f) {
            float x = tow.xAt(tug, 0f, seconds);
            float y = tow.yAt(tug, 0f, seconds);
            assertEquals(40f, (float) Math.hypot(x, y), DELTA);
            double trailing = Math.toRadians(tug.headingAt(0f, seconds - 2f));
            assertEquals(-40f * (float) Math.cos(trailing), x, DELTA);
            assertTrue(x < 0f, "behind at " + seconds);
        }
    }
}
