package exiledsector.ui.cargo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheIconFxTest {

    @Test
    void theRockStaysShutUntilTheCrackThenSnapsOpenPastFullAndShutsAgain() {
        assertEquals(0f, CacheIconFx.openness(0f));
        assertEquals(0f, CacheIconFx.openness(CacheIconFx.CRACK_KEY - 0.01f));
        assertEquals(0f, CacheIconFx.openness(CacheIconFx.CRACK_KEY), 1e-5f);
        assertTrue(CacheIconFx.openness(CacheIconFx.CRACK_KEY + CacheIconFx.OPEN_SPAN * 0.7f) > 1f, "the halves overshoot as they pop open");
        assertEquals(1f, CacheIconFx.openness(CacheIconFx.CRACK_KEY + CacheIconFx.OPEN_SPAN), 1e-5f);
        assertEquals(0f, CacheIconFx.openness(CacheIconFx.CLOSE_END), 1e-5f);
        assertEquals(0f, CacheIconFx.openness(0.95f), 1e-5f);
        assertEquals(CacheIconFx.cycleOf(1f), CacheIconFx.cycleOf(1f + CacheIconFx.CYCLE_SECONDS), 1e-4f);
    }

    @Test
    void theCrackIsTheKeyMomentForTheFrameLightsAndTheFlash() {
        assertEquals(2f, DoctrineIconFx.wave(CacheIconFx.CRACK_KEY, CacheIconFx.CRACK_KEY), 1e-4f);
        assertEquals(1f, DoctrineIconFx.flash(CacheIconFx.CRACK_KEY, CacheIconFx.CRACK_KEY), 1e-5f);
        assertTrue(CacheIconFx.crackleAlpha(CacheIconFx.CRACK_KEY) > 0.5f);
        assertEquals(0f, CacheIconFx.crackleAlpha(CacheIconFx.CRACKLE_END));
        assertEquals(0f, CacheIconFx.crackleAlpha(CacheIconFx.HEAT_START));
        assertTrue(CacheIconFx.seamHeat(CacheIconFx.CRACK_KEY - 0.001f, 0f) > 0.9f, "the seam glows white hot just before it gives");
        assertEquals(0f, CacheIconFx.dustAge(CacheIconFx.CRACK_KEY, CacheIconFx.CRACK_KEY));
        assertEquals(-1f, CacheIconFx.dustAge(CacheIconFx.CRACK_KEY + CacheIconFx.DUST_SPAN * 1.01f,CacheIconFx.CRACK_KEY));
    }

    @Test
    void theBeaconRunsDarkThroughTheScanThenBlinksAllClearTwiceBeforeTheCrack() {
        assertTrue(CacheIconFx.SCAN_END < CacheIconFx.ALL_CLEAR_BLINKS[0]);
        assertTrue(CacheIconFx.ALL_CLEAR_BLINKS[1] + CacheIconFx.BLINK_HALF < CacheIconFx.HEAT_START);
        for (float cycle = CacheIconFx.SCAN_START; cycle < CacheIconFx.SCAN_END; cycle += 0.01f) {
            for (float seconds = 0f; seconds < 2f; seconds += 0.05f) {
                assertEquals(0f, CacheIconFx.beaconLevel(cycle, seconds), "cycle " + cycle);
            }
        }
        for (float blink : CacheIconFx.ALL_CLEAR_BLINKS) {
            assertEquals(1f, CacheIconFx.beaconLevel(blink, 0.5f));
        }
        assertEquals(0f, CacheIconFx.beaconLevel((CacheIconFx.ALL_CLEAR_BLINKS[0] + CacheIconFx.ALL_CLEAR_BLINKS[1]) / 2f, 0f));
        assertEquals(0f, CacheIconFx.beaconLevel(0.6f, 0f), "dark while open");
        assertTrue(CacheIconFx.beaconLevel(0.95f, 0f) > 0f, "the idle beacon blinks again once the cache is shut");
    }

    @Test
    void chipsRiseOneAfterAnotherAfterTheCrackAndSinkBeforeTheRockShuts() {
        float shutting = (CacheIconFx.CLOSE_START + CacheIconFx.CLOSE_END) / 2f;
        for (int count = 1; count <= CacheIconFx.MAX_CHIPS; count++) {
            assertEquals(count, CacheIconFx.CHIP_LAYOUTS[count - 1].length);
            for (int index = 0; index < count; index++) {
                assertEquals(0f, CacheIconFx.chipPresence(CacheIconFx.CRACK_KEY, index, count));
                assertEquals(1f, CacheIconFx.chipPresence(0.65f, index, count), 1e-5f);
                assertEquals(0f, CacheIconFx.chipPresence(shutting, index, count), 1e-5f);
            }
        }
        float early = CacheIconFx.CHIP_RISE_START + CacheIconFx.CHIP_STAGGER * 0.5f;
        assertTrue(CacheIconFx.chipPresence(early, 0, 3) > CacheIconFx.chipPresence(early, 1, 3));
    }

    @Test
    void aCacheShowsOneChipPerRolledNotableUpToThree() {
        assertEquals(0, CacheIconFx.chipCount(List.of()));
        assertEquals(2, CacheIconFx.chipCount(List.of("a.png", "b.png")));
        assertEquals(3, CacheIconFx.chipCount(List.of("a.png", "b.png", "c.png", "d.png")));
    }

    @Test
    void theSeamRunsTopToBottomSoEachHalfIsAStripOfConvexQuads() {
        float[][] seam = CacheIconFx.SEAM;
        assertEquals(0f, seam[0][1]);
        assertEquals(1f, seam[seam.length - 1][1]);
        for (int point = 1; point < seam.length; point++) {
            assertTrue(seam[point][1] > seam[point - 1][1]);
            assertTrue(seam[point][0] > 0f && seam[point][0] < 1f);
        }
    }
}
