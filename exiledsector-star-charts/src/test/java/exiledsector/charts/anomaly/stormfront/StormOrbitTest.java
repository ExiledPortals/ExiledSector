package exiledsector.charts.anomaly.stormfront;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StormOrbitTest {

    private static final StormOrbit AROUND_ORIGIN = new StormOrbit(0f, 0f, 1000f, 8000f, 2000f, 0f, 40f);

    @Test
    void theBandIsASpokeFromTheInnerToTheOuterRadius() {
        assertEquals(7000f, AROUND_ORIGIN.bandLength());
        assertEquals(4500f, AROUND_ORIGIN.centre(0f).x, 0.01f);
        assertEquals(0f, AROUND_ORIGIN.centre(0f).y, 0.01f);
        assertTrue(AROUND_ORIGIN.contains(new Vector2f(1500f, 0f), 0f, 0f));
        assertTrue(AROUND_ORIGIN.contains(new Vector2f(7500f, 900f), 0f, 0f));
        assertFalse(AROUND_ORIGIN.contains(new Vector2f(500f, 0f), 0f, 0f));
        assertFalse(AROUND_ORIGIN.contains(new Vector2f(4000f, 1200f), 0f, 0f));
        assertFalse(AROUND_ORIGIN.contains(new Vector2f(-4000f, 0f), 0f, 0f));
    }

    @Test
    void theSpokeCirclesCounterClockwiseAndNeverLeaves() {
        assertEquals(4500f, AROUND_ORIGIN.centre(10f).y, 0.01f);
        assertEquals(0f, AROUND_ORIGIN.centre(10f).x, 0.01f);
        assertTrue(AROUND_ORIGIN.contains(new Vector2f(0f, 5000f), 0f, 10f));
        assertEquals(4500f, AROUND_ORIGIN.centre(40f).x, 0.01f);
        assertFalse(AROUND_ORIGIN.isDone(10000f));
        assertEquals(90f, AROUND_ORIGIN.facingDegrees(0f));
    }

    @Test
    void intelDaysCountTheTurnUntilTheLeadingEdgeArrives() {
        Vector2f north = new Vector2f(0f, 5000f);
        float leadingDegrees = (float) Math.toDegrees(1000f / 5000f);
        float expectedDays = (90f - leadingDegrees) / 360f * 40f;

        assertEquals(expectedDays, AROUND_ORIGIN.daysUntilReaches(north, 0f), 0.01f);
        assertEquals(-1f, AROUND_ORIGIN.daysUntilReaches(new Vector2f(3000f, 0f), 0f));
        assertEquals(-1f, AROUND_ORIGIN.daysUntilReaches(new Vector2f(0f, 9000f), 0f));
        assertEquals(-1f, AROUND_ORIGIN.daysUntilReaches(new Vector2f(0f, 500f), 0f));
    }
}
