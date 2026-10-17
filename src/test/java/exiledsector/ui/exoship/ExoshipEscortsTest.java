package exiledsector.ui.exoship;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExoshipEscortsTest {

    private static final float SHIP_LENGTH = 700f;

    @Test
    void threeToFiveSmallEscortsFlyBothWaysAroundTheShip() {
        assertTrue(ExoshipEscorts.ESCORTS.length >= 3 && ExoshipEscorts.ESCORTS.length <= 5);
        assertTrue(Arrays.stream(ExoshipEscorts.ESCORTS).anyMatch(escort -> escort.direction() > 0));
        assertTrue(Arrays.stream(ExoshipEscorts.ESCORTS).anyMatch(escort -> escort.direction() < 0));
        assertTrue(Arrays.stream(ExoshipEscorts.ESCORTS).allMatch(escort -> escort.lengthShare() <= 0.1f));
    }

    @Test
    void eachEscortLoopsAroundTheShipPassingBothInFrontAndBehindIt() {
        for (ExoshipEscorts.Escort escort : ExoshipEscorts.ESCORTS) {
            boolean seenInFront = false;
            boolean seenBehind = false;
            float nearest = Float.MAX_VALUE;
            float farthest = 0f;
            for (float seconds = 0f; seconds < escort.secondsPerLap(); seconds += 0.25f) {
                ExoshipEscorts.Pose pose = ExoshipEscorts.pose(escort, 100f, -50f, SHIP_LENGTH, seconds);
                float distance = (float) Math.hypot(pose.treeX() - 100f, pose.treeY() + 50f);
                nearest = Math.min(nearest, distance);
                farthest = Math.max(farthest, distance);
                seenInFront |= pose.inFront();
                seenBehind |= !pose.inFront();
            }
            assertTrue(seenInFront && seenBehind, escort.spritePath());
            assertTrue(farthest <= escort.orbitShare() * SHIP_LENGTH * (1f + escort.wobbleShare()) + 1f, escort.spritePath());
            assertTrue(nearest > SHIP_LENGTH * 0.1f, escort.spritePath() + " flies through the hull centre");
        }
    }

    @Test
    void escortsFaceTheWayTheyFly() {
        for (ExoshipEscorts.Escort escort : ExoshipEscorts.ESCORTS) {
            ExoshipEscorts.Pose now = ExoshipEscorts.pose(escort, 0f, 0f, SHIP_LENGTH, 3f);
            ExoshipEscorts.Pose soon = ExoshipEscorts.pose(escort, 0f, 0f, SHIP_LENGTH, 3.02f);
            float travelDeg = (float) Math.toDegrees(Math.atan2(soon.treeY() - now.treeY(), soon.treeX() - now.treeX()));
            float difference = (travelDeg - now.facingDeg() + 540f) % 360f - 180f;
            assertTrue(Math.abs(difference) < 5f, escort.spritePath() + " off by " + difference);
        }
    }
}
