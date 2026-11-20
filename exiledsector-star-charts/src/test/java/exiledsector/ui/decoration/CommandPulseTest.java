package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPulseTest {

    private static final float DELTA = 0.001f;

    @Test
    void aPulseSweepsOutOverItsSweepThenWaitsForTheNextOne() {
        CommandPulse pulse = new CommandPulse(8f, 2f, 0f, 1, 100f, 0.6f);

        assertEquals(0f, pulse.ringProgressAt(0, 0f), DELTA);
        assertEquals(0.5f, pulse.ringProgressAt(0, 1f), DELTA);
        assertEquals(-1f, pulse.ringProgressAt(0, 2.5f), DELTA);
        assertEquals(0.25f, pulse.ringProgressAt(0, 8.5f), DELTA);
    }

    @Test
    void itsPhaseShiftsWhenItFiresEvenBeforeTheClockStarts() {
        CommandPulse pulse = new CommandPulse(8f, 2f, 3f, 1, 100f, 0.6f);

        assertEquals(0f, pulse.ringProgressAt(0, 5f), DELTA);
        assertEquals(-1f, pulse.ringProgressAt(0, 0f), DELTA);
        assertEquals(0.5f, pulse.ringProgressAt(0, -2f), DELTA);
    }

    @Test
    void aLaterRingFollowsTheFirstByTheRingGap() {
        CommandPulse pulse = new CommandPulse(10f, 4f, 0f, 2, 100f, 0.85f);

        assertEquals(-1f, pulse.ringProgressAt(1, 0.5f), DELTA);
        assertEquals(pulse.ringProgressAt(0, 1f), pulse.ringProgressAt(1, 1f + CommandPulse.RING_GAP_SECONDS), DELTA);
    }

    @Test
    void aHeavyPulseIsSlowerRarerStrongerAndDoubleRinged() {
        for (int seed = 0; seed < 20; seed++) {
            CommandPulse steady = CommandPulse.steady(100f, new Random(seed));
            CommandPulse heavy = CommandPulse.heavy(100f, new Random(seed));

            assertTrue(heavy.sweepSeconds() > steady.sweepSeconds());
            assertTrue(heavy.intervalSeconds() > steady.intervalSeconds());
            assertTrue(heavy.strength() > steady.strength());
            assertEquals(1, steady.rings());
            assertEquals(CommandPulse.HEAVY_RINGS, heavy.rings());
            assertTrue(steady.phaseSeconds() >= 0f && steady.phaseSeconds() < steady.intervalSeconds());
            assertTrue(steady.sweepSeconds() < steady.intervalSeconds());
        }
    }

    @Test
    void twoFacingPulsesClashBrightestWhenTheirRingsMeetHalfwayAndNotAtAllWhileIdle() {
        CommandPulse pulse = new CommandPulse(8f, 2f, 0f, 1, 100f, 0.6f);
        float distance = 50f;

        assertEquals(0.5f, pulse.meetingProgress(distance), DELTA);
        assertEquals(1f, pulse.clashAt(1f, distance), DELTA);
        assertTrue(pulse.clashAt(0.2f, distance) == 0f);
        assertTrue(pulse.clashAt(1.2f, distance) > 0f && pulse.clashAt(1.2f, distance) < 1f);
        assertEquals(0f, pulse.clashAt(5f, distance), DELTA);
        assertEquals(0f, pulse.clashAt(1f, 150f), DELTA);
    }
}
