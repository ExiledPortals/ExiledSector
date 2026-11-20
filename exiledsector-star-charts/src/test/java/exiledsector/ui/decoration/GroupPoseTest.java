package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupPoseTest {

    private static final float DELTA = 0.001f;

    @Test
    void aSwayingFlagshipTurnsBothWaysButNeverPastItsSway() {
        GroupPose pose = GroupPose.centred(12f, new Random(3));
        float lowest = Float.MAX_VALUE;
        float highest = -Float.MAX_VALUE;
        for (float seconds = 0f; seconds < 80f; seconds += 0.1f) {
            float heading = pose.headingAt(90f, seconds);
            lowest = Math.min(lowest, heading);
            highest = Math.max(highest, heading);
        }

        assertTrue(lowest >= 78f - DELTA && highest <= 102f + DELTA, lowest + " to " + highest);
        assertTrue(lowest < 80f && highest > 100f, lowest + " to " + highest);
        assertEquals(0f, pose.x(), DELTA);
        assertEquals(0f, pose.y(), DELTA);
    }

    @Test
    void aPoseWithoutSwayHoldsItsHeadingPlusItsOffset() {
        GroupPose pose = new GroupPose(0f, 0f, 180f, 0f, 1.2f);

        assertEquals(225f, pose.headingAt(45f, 0f), DELTA);
        assertEquals(225f, pose.headingAt(45f, 33f), DELTA);
    }

    @Test
    void aPlacedPoseSitsAtItsSpotTurnedToTheBaseHeading() {
        GroupPose ahead = GroupPose.at(new FlagshipFormation.Slot(50f, 0f), 90f, 0f, 0f, new Random(1));
        GroupPose leftward = GroupPose.at(new FlagshipFormation.Slot(0f, 20f), 0f, 0f, 0f, new Random(1));

        assertEquals(0f, ahead.x(), DELTA);
        assertEquals(50f, ahead.y(), DELTA);
        assertEquals(0f, leftward.x(), DELTA);
        assertEquals(20f, leftward.y(), DELTA);
    }
}
