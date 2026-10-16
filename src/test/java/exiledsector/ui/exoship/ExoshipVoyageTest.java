package exiledsector.ui.exoship;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExoshipVoyageTest {

    private static final float FRAME = 1f / 60f;
    private static final List<ExoshipVoyage.Anchor> FAR_APART = List.of(new ExoshipVoyage.Anchor(0f, 0f),
            new ExoshipVoyage.Anchor(6000f, 0f), new ExoshipVoyage.Anchor(0f, 8000f));

    private static final class RecordedEvents implements ExoshipVoyage.Events {
        final List<String> names = new ArrayList<>();
        float departedX;
        float departedY;
        float arrivedX;
        float arrivedY;

        @Override
        public void chargingUp(float shipX, float shipY) {
            names.add("chargingUp");
        }

        @Override
        public void departed(float shipX, float shipY, float headingX, float headingY) {
            names.add("departed");
            departedX = shipX;
            departedY = shipY;
        }

        @Override
        public void arrived(float shipX, float shipY, float headingX, float headingY) {
            names.add("arrived");
            arrivedX = shipX;
            arrivedY = shipY;
        }
    }

    private static float distanceToAnchor(ExoshipVoyage voyage) {
        ExoshipVoyage.Anchor anchor = voyage.currentAnchor();
        return (float) Math.hypot(voyage.shipX() - anchor.x(), voyage.shipY() - anchor.y());
    }

    private static void runUntil(ExoshipVoyage voyage, ExoshipVoyage.Phase phase, float maxSeconds) {
        for (float elapsed = 0f; elapsed < maxSeconds; elapsed += FRAME) {
            voyage.advance(FRAME);
            if (voyage.phase() == phase) {
                return;
            }
        }
    }

    @Test
    void theShipStartsInOrbitAroundOneOfTheNotables() {
        ExoshipVoyage voyage = new ExoshipVoyage(FAR_APART, new Random(1), new RecordedEvents());

        assertTrue(voyage.hasRoute());
        assertEquals(ExoshipVoyage.Phase.ORBITING, voyage.phase());
        assertEquals(ExoshipVoyage.ORBIT_RADIUS, distanceToAnchor(voyage), 0.01f);
        assertTrue(voyage.isVisible());
    }

    @Test
    void withinThirtySecondsTheShipTurnsBurnsWarpsAndSettlesIntoOrbitAroundAnotherNotable() {
        RecordedEvents events = new RecordedEvents();
        ExoshipVoyage voyage = new ExoshipVoyage(FAR_APART, new Random(2), events);
        ExoshipVoyage.Anchor start = voyage.currentAnchor();

        runUntil(voyage, ExoshipVoyage.Phase.TURNING, ExoshipVoyage.JUMP_INTERVAL_SECONDS + 1f);
        assertEquals(ExoshipVoyage.Phase.TURNING, voyage.phase());
        runUntil(voyage, ExoshipVoyage.Phase.ORBITING, 20f);

        assertEquals(List.of("chargingUp", "departed", "arrived"), events.names);
        assertNotEquals(start, voyage.currentAnchor());
        assertEquals(ExoshipVoyage.ORBIT_RADIUS, distanceToAnchor(voyage), 1f);
    }

    @Test
    void theShipIsOnlyHiddenWhileWarping() {
        ExoshipVoyage voyage = new ExoshipVoyage(FAR_APART, new Random(3), new RecordedEvents());

        runUntil(voyage, ExoshipVoyage.Phase.WARPING, ExoshipVoyage.JUMP_INTERVAL_SECONDS + 10f);
        assertFalse(voyage.isVisible());
        assertEquals(1f, voyage.warpLevel());
        runUntil(voyage, ExoshipVoyage.Phase.ARRIVING, 5f);
        assertTrue(voyage.isVisible());
    }

    @Test
    void outsideTheWarpTheShipNeverJumpsMoreThanItsTopSpeedInOneFrame() {
        ExoshipVoyage voyage = new ExoshipVoyage(FAR_APART, new Random(4), new RecordedEvents());
        float previousX = voyage.shipX();
        float previousY = voyage.shipY();
        float topSpeed = ExoshipVoyage.ORBIT_SPEED + ExoshipVoyage.BURN_ACCELERATION * ExoshipVoyage.BURN_SECONDS
                + 2f * ExoshipVoyage.APPROACH_DISTANCE / ExoshipVoyage.ARRIVAL_SECONDS;
        for (float elapsed = 0f; elapsed < 80f; elapsed += FRAME) {
            ExoshipVoyage.Phase before = voyage.phase();
            voyage.advance(FRAME);
            boolean warpInvolved = before == ExoshipVoyage.Phase.WARPING || voyage.phase() == ExoshipVoyage.Phase.WARPING;
            float step = (float) Math.hypot(voyage.shipX() - previousX, voyage.shipY() - previousY);
            assertTrue(warpInvolved || step <= topSpeed * FRAME * 1.05f, "jumped " + step + " in " + before);
            previousX = voyage.shipX();
            previousY = voyage.shipY();
        }
    }

    @Test
    void theWarpCarriesTheShipAcrossTheTreeFasterThanItCouldFly() {
        RecordedEvents events = new RecordedEvents();
        ExoshipVoyage voyage = new ExoshipVoyage(FAR_APART, new Random(5), events);

        runUntil(voyage, ExoshipVoyage.Phase.ARRIVING, ExoshipVoyage.JUMP_INTERVAL_SECONDS + 10f);

        float warpDistance = (float) Math.hypot(events.arrivedX - events.departedX, events.arrivedY - events.departedY);
        assertTrue(warpDistance > ExoshipVoyage.MIN_JUMP_DISTANCE - ExoshipVoyage.APPROACH_DISTANCE * 3f, "warped " + warpDistance);
    }

    @Test
    void destinationsAtLeastTheMinimumJumpAwayAreChosenWhenThereAreAny() {
        List<ExoshipVoyage.Anchor> anchors = List.of(new ExoshipVoyage.Anchor(0f, 0f), new ExoshipVoyage.Anchor(300f, 0f),
                new ExoshipVoyage.Anchor(9000f, 0f));
        for (int seed = 0; seed < 20; seed++) {
            ExoshipVoyage voyage = new ExoshipVoyage(anchors, new Random(seed), new RecordedEvents());
            ExoshipVoyage.Anchor start = voyage.currentAnchor();
            runUntil(voyage, ExoshipVoyage.Phase.ARRIVING, ExoshipVoyage.JUMP_INTERVAL_SECONDS + 10f);
            runUntil(voyage, ExoshipVoyage.Phase.ORBITING, 10f);
            ExoshipVoyage.Anchor end = voyage.currentAnchor();
            float jump = (float) Math.hypot(end.x() - start.x(), end.y() - start.y());
            assertTrue(jump >= ExoshipVoyage.MIN_JUMP_DISTANCE, "seed " + seed + " jumped only " + jump);
        }
    }

    @Test
    void aLoneNotableKeepsTheShipInOrbitForever() {
        RecordedEvents events = new RecordedEvents();
        ExoshipVoyage voyage = new ExoshipVoyage(List.of(new ExoshipVoyage.Anchor(50f, 50f)), new Random(6), events);

        for (int frame = 0; frame < 60 * 90; frame++) {
            voyage.advance(FRAME);
        }

        assertEquals(ExoshipVoyage.Phase.ORBITING, voyage.phase());
        assertTrue(events.names.isEmpty());
    }

    @Test
    void withoutNotablesThereIsNoRoute() {
        ExoshipVoyage voyage = new ExoshipVoyage(List.of(), new Random(7), new RecordedEvents());

        voyage.advance(1f);

        assertFalse(voyage.hasRoute());
    }

    @Test
    void anglesWrapTheShortWayRound() {
        assertEquals(20f, ExoshipVoyage.angleBetween(350f, 10f), 1e-4f);
        assertEquals(-20f, ExoshipVoyage.angleBetween(10f, 350f), 1e-4f);
    }
}
