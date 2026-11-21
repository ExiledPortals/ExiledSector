package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalvageRunTest {

    private static final FlagshipFormation.Slot HOME = new FlagshipFormation.Slot(30f, 60f);
    private static final float FIELD_RADIUS = 100f;
    private static final float STEP_SECONDS = 0.05f;
    private static final float DELTA = 0.01f;

    private static SalvageRun run(int seed) {
        return SalvageRun.from(HOME, FIELD_RADIUS, new Random(seed));
    }

    private static float cycleStart(SalvageRun run, float after) {
        float seconds = after;
        int cycle = run.cycleAt(seconds);
        while (run.cycleAt(seconds) == cycle) {
            seconds += 0.001f;
        }
        return seconds;
    }

    @Test
    void eachCycleDartsOutWorksTheDebrisComesBackAndRests() {
        SalvageRun run = run(4);
        float start = cycleStart(run, 0f);

        assertEquals(SalvageRun.Leg.OUT, run.legAt(start + 0.1f));
        assertEquals(SalvageRun.Leg.WORKING, run.legAt(start + SalvageRun.OUT_SECONDS + 0.1f));
        assertEquals(SalvageRun.Leg.BACK, run.legAt(start + SalvageRun.OUT_SECONDS + SalvageRun.WORK_SECONDS + 0.1f));
        assertEquals(SalvageRun.Leg.RESTING, run.legAt(start + run.cycleSeconds() - 0.1f));
        assertEquals(0f, run.shareAt(start + run.cycleSeconds() - 0.1f), DELTA);
        assertEquals(1f, run.shareAt(start + SalvageRun.OUT_SECONDS + 0.1f), DELTA);
    }

    @Test
    void theShipLeavesHomeSmoothlyAndOnlyEverMovesOutwardOnTheWayOut() {
        SalvageRun run = run(9);
        float start = cycleStart(run, 0f);
        float previous = run.shareAt(start + 0.001f);

        assertEquals(0f, previous, DELTA);
        for (float into = STEP_SECONDS; into < SalvageRun.OUT_SECONDS; into += STEP_SECONDS) {
            float share = run.shareAt(start + into);
            assertTrue(share >= previous, "at " + into);
            previous = share;
        }
        assertEquals(HOME.forward(), run.forwardAt(start + 0.001f), 0.1f);
        assertEquals(HOME.lateral(), run.lateralAt(start + 0.001f), 0.1f);
    }

    @Test
    void everyTargetLiesFartherOutThanHomeAndOnHomesSide() {
        SalvageRun run = run(13);
        double homeAngle = Math.toDegrees(Math.atan2(HOME.lateral(), HOME.forward()));
        for (float seconds = 0f; seconds < 300f; seconds += 1.3f) {
            float forward = run.targetForwardAt(seconds);
            float lateral = run.targetLateralAt(seconds);
            float reach = (float) Math.hypot(forward, lateral);
            double angle = Math.toDegrees(Math.atan2(lateral, forward));

            assertTrue(reach >= HOME.reach() + FIELD_RADIUS * SalvageRun.MIN_EXTRA_REACH_SHARE - DELTA, "reach " + reach);
            assertTrue(reach <= HOME.reach() + FIELD_RADIUS * (SalvageRun.MIN_EXTRA_REACH_SHARE + SalvageRun.EXTRA_REACH_SPREAD) + DELTA);
            assertTrue(Math.abs(angle - homeAngle) <= SalvageRun.SPREAD_DEGREES + DELTA, "angle " + angle);
        }
    }

    @Test
    void aNewCycleUsuallyPicksANewPieceOfDebris() {
        SalvageRun run = run(21);
        float first = cycleStart(run, 0f);
        float second = cycleStart(run, first + 0.01f);

        assertNotEquals(run.targetForwardAt(first + 0.1f), run.targetForwardAt(second + 0.1f));
    }

    @Test
    void itFacesTheDebrisOnTheWayOutTheFlagshipWhileRestingAndBurnsHardOnlyWhileDarting() {
        SalvageRun run = run(5);
        float start = cycleStart(run, 0f);
        float heading = 30f;
        float out = start + 1f;
        float outward = (float) Math.toDegrees(Math.atan2(run.targetLateralAt(out) - HOME.lateral(), run.targetForwardAt(out) - HOME.forward()));

        assertEquals(heading + outward, run.facingAt(out, heading), DELTA);
        assertEquals(heading + outward + 180f, run.facingAt(start + SalvageRun.OUT_SECONDS + SalvageRun.WORK_SECONDS + 1f, heading), DELTA);
        assertEquals(heading, run.facingAt(start + run.cycleSeconds() - 0.1f, heading), DELTA);
        assertEquals(SalvageRun.DARTING_ENGINE_LEVEL, run.engineLevelAt(out), DELTA);
        assertEquals(SalvageRun.IDLE_ENGINE_LEVEL, run.engineLevelAt(start + SalvageRun.OUT_SECONDS + 0.5f), DELTA);
    }

    @Test
    void itsPositionTurnsWithTheFlagship() {
        SalvageRun run = run(2);
        float seconds = 17f;
        float reach = (float) Math.hypot(run.forwardAt(seconds), run.lateralAt(seconds));

        assertEquals(reach, (float) Math.hypot(run.xAt(seconds, 75f), run.yAt(seconds, 75f)), DELTA);
        assertEquals(run.forwardAt(seconds), run.xAt(seconds, 0f), DELTA);
        assertEquals(run.lateralAt(seconds), run.yAt(seconds, 0f), DELTA);
    }
}
