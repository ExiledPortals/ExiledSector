package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvoyRunTest {

    private static final float LENGTH = 550f;
    private static final float DELTA = 0.01f;

    private static ConvoyRun eastward(int order, int count) {
        return new ConvoyRun(0f, 0f, LENGTH, 0f, order, count);
    }

    private static float travelSeconds() {
        return LENGTH / ConvoyRun.SPEED;
    }

    @Test
    void aConvoyLeavesItsPortHeadingForTheWorldOnTheRightHandLane() {
        ConvoyRun run = eastward(0, 1);

        ConvoyRun.Position halfway = run.at(travelSeconds() / 2f);

        assertEquals(LENGTH / 2f, halfway.x(), DELTA);
        assertEquals(-ConvoyRun.LANE_OFFSET, halfway.y(), DELTA);
        assertEquals(0f, halfway.headingDegrees(), DELTA);
        assertEquals(1f, halfway.visibility(), DELTA);
    }

    @Test
    void itDocksUnseenAtTheWorldThenComesBackOnTheOtherLane() {
        ConvoyRun run = eastward(0, 1);

        ConvoyRun.Position docked = run.at(travelSeconds() + ConvoyRun.DOCK_SECONDS / 2f);
        ConvoyRun.Position returning = run.at(travelSeconds() * 1.5f + ConvoyRun.DOCK_SECONDS);

        assertEquals(0f, docked.visibility(), DELTA);
        assertEquals(LENGTH, docked.x(), DELTA);
        assertEquals(LENGTH / 2f, returning.x(), DELTA);
        assertEquals(ConvoyRun.LANE_OFFSET, returning.y(), DELTA);
        assertEquals(180f, Math.abs(returning.headingDegrees()), DELTA);
    }

    @Test
    void itFadesInAsItLeavesThePortAndTheCycleRepeats() {
        ConvoyRun run = eastward(0, 1);
        float justOut = ConvoyRun.FADE_DISTANCE / 2f / ConvoyRun.SPEED;

        assertEquals(0.5f, run.at(justOut).visibility(), DELTA);
        assertEquals(run.at(justOut).x(), run.at(justOut + run.cycleSeconds()).x(), DELTA);
        assertEquals(2f * (travelSeconds() + ConvoyRun.DOCK_SECONDS), run.cycleSeconds(), DELTA);
    }

    @Test
    void convoysFromOnePortAreSpreadEvenlyAroundTheCycle() {
        ConvoyRun first = eastward(0, 2);
        ConvoyRun second = eastward(1, 2);
        float seconds = travelSeconds() / 4f;

        assertEquals(first.at(seconds + first.cycleSeconds() / 2f).x(), second.at(seconds).x(), DELTA);
        assertTrue(second.at(seconds).headingDegrees() != first.at(seconds).headingDegrees());
    }

    @Test
    void headingsFollowTheTreeFacingConventionForAnyDirection() {
        ConvoyRun northward = new ConvoyRun(0f, 0f, 0f, LENGTH, 0, 1);

        assertEquals(-90f, northward.at(travelSeconds() / 2f).headingDegrees(), DELTA);
    }
}
