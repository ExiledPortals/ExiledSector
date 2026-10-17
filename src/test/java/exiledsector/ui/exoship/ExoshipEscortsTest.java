package exiledsector.ui.exoship;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.ui.decoration.HullEngines;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExoshipEscortsTest {

    private static final float SHIP_LENGTH = 700f;
    private static final float FRAME = 1f / 60f;

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

    private static ExoshipEscorts runOrder(ExoshipEscorts escorts, boolean launched, float seconds) {
        for (float elapsed = 0f; elapsed < seconds; elapsed += FRAME) {
            escorts.advance(FRAME, launched);
        }
        return escorts;
    }

    private static ExoshipEscorts withPlayerShip(HullSize hullSize) {
        ExoshipEscorts escorts = new ExoshipEscorts();
        escorts.setPlayerEscort(ExoshipEscorts.playerEscort("graphics/ships/onslaught.png", "onslaught", 160f, 280f, hullSize),
                HullEngines.NONE);
        return escorts;
    }

    @Test
    void everyEscortIsAboardBeforeTheShortestPossibleRunUpToAJump() {
        assertTrue(ExoshipEscorts.RECALL_SECONDS < ExoshipVoyage.BURN_SECONDS);
        ExoshipEscorts escorts = runOrder(withPlayerShip(HullSize.CAPITAL_SHIP), false, ExoshipEscorts.RECALL_SECONDS + FRAME);
        for (int escortIndex = 0; escortIndex < ExoshipEscorts.MAX_ESCORTS; escortIndex++) {
            assertEquals(0f, escorts.escortLevel(escortIndex), 0.0001f);
        }
    }

    @Test
    void capitalsLeaveTheHangarAtTheSlowestPaceAndSmallerHullsFaster() {
        assertEquals(4.68f, ExoshipEscorts.departSeconds(HullSize.CAPITAL_SHIP), 0.0001f);
        assertTrue(ExoshipEscorts.departSeconds(HullSize.CRUISER) < ExoshipEscorts.departSeconds(HullSize.CAPITAL_SHIP));
        assertTrue(ExoshipEscorts.departSeconds(HullSize.DESTROYER) < ExoshipEscorts.departSeconds(HullSize.CRUISER));
        assertTrue(ExoshipEscorts.departSeconds(HullSize.FRIGATE) < ExoshipEscorts.departSeconds(HullSize.DESTROYER));
        assertTrue(ExoshipEscorts.departSeconds(HullSize.FIGHTER) < ExoshipEscorts.departSeconds(HullSize.FRIGATE));

        int playerIndex = ExoshipEscorts.ESCORTS.length;
        float released = ExoshipEscorts.DEPART_GAP_SECONDS * playerIndex;
        ExoshipEscorts capital = runOrder(runOrder(withPlayerShip(HullSize.CAPITAL_SHIP), false, ExoshipEscorts.RECALL_SECONDS + FRAME),
                true, released + ExoshipEscorts.departSeconds(HullSize.FRIGATE) + FRAME);
        ExoshipEscorts frigate = runOrder(runOrder(withPlayerShip(HullSize.FRIGATE), false, ExoshipEscorts.RECALL_SECONDS + FRAME),
                true, released + ExoshipEscorts.departSeconds(HullSize.FRIGATE) + FRAME);

        assertEquals(1f, frigate.escortLevel(playerIndex));
        assertTrue(capital.escortLevel(playerIndex) < 0.6f);
    }

    @Test
    void thePlayersShipJoinsTheEscortsAtASizeMatchingItsHullClass() {
        ExoshipEscorts escorts = new ExoshipEscorts();
        assertEquals(ExoshipEscorts.ESCORTS.length, escorts.escortCount());

        ExoshipEscorts.Escort playerShip = ExoshipEscorts.playerEscort("graphics/ships/onslaught.png", "onslaught", 160f, 280f,
                HullSize.CAPITAL_SHIP);
        escorts.setPlayerEscort(playerShip, HullEngines.NONE);

        assertEquals(ExoshipEscorts.MAX_ESCORTS, escorts.escortCount());
        assertEquals(playerShip, escorts.escort(ExoshipEscorts.ESCORTS.length));
        assertEquals("graphics/ships/onslaught.png", playerShip.spritePath());
        assertTrue(ExoshipEscorts.playerLengthShare(HullSize.FRIGATE) < ExoshipEscorts.playerLengthShare(HullSize.DESTROYER));
        assertTrue(ExoshipEscorts.playerLengthShare(HullSize.DESTROYER) < ExoshipEscorts.playerLengthShare(HullSize.CRUISER));
        assertTrue(ExoshipEscorts.playerLengthShare(HullSize.CRUISER) < playerShip.lengthShare());
        assertEquals(ExoshipEscorts.ESCORTS[0].lengthShare(), ExoshipEscorts.playerLengthShare(HullSize.DESTROYER), 0.0001f);
        assertEquals(HullSize.CAPITAL_SHIP, playerShip.hullSize());
    }

    @Test
    void escortsLaunchOneAfterAnotherAndTheLastOutIsTheFirstBackIn() {
        ExoshipEscorts aboard = runOrder(withPlayerShip(HullSize.CAPITAL_SHIP), false, ExoshipEscorts.RECALL_SECONDS + FRAME);
        ExoshipEscorts launching = runOrder(aboard, true, ExoshipEscorts.DEPART_GAP_SECONDS * 1.5f);
        assertTrue(launching.escortLevel(0) > launching.escortLevel(1));
        assertTrue(launching.escortLevel(1) > 0f);
        assertEquals(0f, launching.escortLevel(2));

        ExoshipEscorts returning = runOrder(runOrder(withPlayerShip(HullSize.CAPITAL_SHIP), true, 1f), false, 0.5f);
        assertTrue(returning.escortLevel(ExoshipEscorts.MAX_ESCORTS - 1) < returning.escortLevel(1));
        assertEquals(1f, returning.escortLevel(0));
    }

    @Test
    void anEscortFliesBetweenItsLoopAndTheHangarAtTheShipCentre() {
        ExoshipEscorts.Escort escort = ExoshipEscorts.ESCORTS[1];
        ExoshipEscorts.Pose orbit = ExoshipEscorts.pose(escort, 100f, -50f, SHIP_LENGTH, 4f);
        ExoshipEscorts.Pose out = ExoshipEscorts.travelPose(escort, 100f, -50f, SHIP_LENGTH, 4f, 1f, false);
        ExoshipEscorts.Pose aboard = ExoshipEscorts.travelPose(escort, 100f, -50f, SHIP_LENGTH, 4f, 0f, true);

        assertEquals(orbit, out);
        assertEquals(100f, aboard.treeX(), 0.001f);
        assertEquals(-50f, aboard.treeY(), 0.001f);
        assertFalse(aboard.inFront());
    }

    @Test
    void returningEscortsHeadForTheShipAndLaunchingOnesHeadAway() {
        ExoshipEscorts.Escort escort = ExoshipEscorts.ESCORTS[2];
        ExoshipEscorts.Pose returning = ExoshipEscorts.travelPose(escort, 0f, 0f, SHIP_LENGTH, 2f, 0.05f, true);
        ExoshipEscorts.Pose launching = ExoshipEscorts.travelPose(escort, 0f, 0f, SHIP_LENGTH, 2f, 0.05f, false);
        double towardShipDeg = Math.toDegrees(Math.atan2(-returning.treeY(), -returning.treeX()));

        assertTrue(Math.abs(angleBetween(returning.facingDeg(), (float) towardShipDeg)) < 15f);
        assertTrue(Math.abs(angleBetween(launching.facingDeg(), (float) towardShipDeg + 180f)) < 15f);
    }

    private static float angleBetween(float fromDeg, float toDeg) {
        return (toDeg - fromDeg + 540f) % 360f - 180f;
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
