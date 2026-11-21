package exiledsector.charts.officer.scar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScarChallengePickTest {

    @BeforeEach
    void loadTables() throws Exception {
        ScarFixtures.loadShipped();
    }

    @Test
    void aChallengeLossPicksAnEligibleScarAtRandom() {
        Random random = new Random(7L);
        Set<String> picked = new HashSet<>();
        for (int roll = 0; roll < 200; roll++) {
            String scarId = ScarChallengePick.pick(Map.of(), ScarFixtures.SHIELDED_WARSHIP, random);
            assertNotEquals(ScarTable.FALLBACK_SCAR_ID, scarId);
            assertTrue(ScarScoring.eligible(ScarTable.row(scarId), Map.of(), ScarFixtures.SHIELDED_WARSHIP), scarId);
            picked.add(scarId);
        }
        assertTrue(picked.size() > 10, picked.toString());
    }

    @Test
    void aCappedScarIsNeverPicked() {
        List<ScarRow> rows = List.of(ScarFixtures.row("overloaded", ScarDetector.OVERLOADS, 1f, "flux"),
                ScarFixtures.row("flux_pinned", ScarDetector.HIGH_FLUX_SECONDS, 20f, "flux"), ScarFixtures.fallback());
        Map<String, Integer> capped = Map.of("overloaded", ScarTable.STACK_CAP);
        Random random = new Random(3L);
        for (int roll = 0; roll < 20; roll++) {
            assertEquals("flux_pinned", ScarChallengePick.pick(rows, capped, ScarFixtures.SHIELDED_WARSHIP, random));
        }
    }

    @Test
    void withNoEligibleScarOrNoFlagshipItTakesTheFallback() {
        List<ScarRow> rows = List.of(ScarFixtures.row("overloaded", ScarDetector.OVERLOADS, 1f, "flux"), ScarFixtures.fallback());
        assertEquals(ScarTable.FALLBACK_SCAR_ID, ScarChallengePick.pick(rows, Map.of("overloaded", ScarTable.STACK_CAP),
                ScarFixtures.SHIELDED_WARSHIP, new Random(1L)));
        assertEquals(ScarTable.FALLBACK_SCAR_ID, ScarChallengePick.pick(Map.of(), null, new Random(1L)));
    }
}
