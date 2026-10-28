package exiledsector.charts.anomaly.storm;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StormPathTest {

    private static final StormPath EASTWARD = new StormPath(0f, 10000f, 3000f, 30f);

    @Test
    void theBandStartsBeyondOneEdgeAndLeavesPastTheOther() {
        assertEquals(-11500f, EASTWARD.centre(0f).x, 0.01f);
        assertEquals(0f, EASTWARD.centre(15f).x, 0.01f);
        assertFalse(EASTWARD.contains(new Vector2f(-9900f, 0f), 0f, 0f));
        assertTrue(EASTWARD.contains(new Vector2f(-10100f, 0f), 0f, 0f));
        assertFalse(EASTWARD.isDone(29f));
        assertTrue(EASTWARD.isDone(30f));
        assertFalse(EASTWARD.contains(new Vector2f(11500f, 0f), 0f, 30f));
    }

    @Test
    void containsPointsWithinItsWidthAndLengthOnly() {
        assertTrue(EASTWARD.contains(new Vector2f(1400f, 9000f), 0f, 15f));
        assertFalse(EASTWARD.contains(new Vector2f(1600f, 0f), 0f, 15f));
        assertTrue(EASTWARD.contains(new Vector2f(1600f, 0f), 200f, 15f));
        assertFalse(EASTWARD.contains(new Vector2f(0f, 10500f), 0f, 15f));
    }

    @Test
    void aTurnedPathSweepsAlongItsOwnDirection() {
        StormPath northward = new StormPath(90f, 10000f, 3000f, 30f);

        assertEquals(-11500f, northward.centre(0f).y, 0.01f);
        assertTrue(northward.contains(new Vector2f(9000f, 0f), 0f, 15f));
        assertFalse(northward.contains(new Vector2f(0f, 2000f), 0f, 15f));
    }

    @Test
    void daysUntilReachesCountsToTheLeadingEdgeAndSkipsWhatItMisses() {
        float days = EASTWARD.daysUntilReaches(new Vector2f(1500f, 0f), 0f);

        assertEquals(15f, days, 0.01f);
        assertEquals(5f, EASTWARD.daysUntilReaches(new Vector2f(1500f, 0f), 10f), 0.01f);
        assertEquals(-1f, EASTWARD.daysUntilReaches(new Vector2f(1500f, 0f), 20f));
        assertEquals(-1f, EASTWARD.daysUntilReaches(new Vector2f(0f, 12000f), 0f));
    }

    @Test
    void daysLeftCountsDownToZero() {
        assertEquals(30f, EASTWARD.daysLeft(0f));
        assertEquals(0f, EASTWARD.daysLeft(45f));
    }
}
