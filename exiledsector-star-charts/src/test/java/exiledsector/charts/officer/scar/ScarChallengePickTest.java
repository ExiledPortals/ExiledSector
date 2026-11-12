package exiledsector.charts.officer.scar;

import exiledsector.charts.ChartTables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScarChallengePickTest {

    @BeforeEach
    void loadTables() throws Exception {
        ScarFixtures.loadShipped();
        assertEquals(List.of(), ScarChallengePick.register(ChartTables.csvRows(ScarChallengePick.FOCUS_PATH)));
    }

    @AfterEach
    void clearFocus() {
        ScarChallengePick.clear();
    }

    @Test
    void focusTagsAreOrderedStrongestFirstWithTiesInListedOrder() {
        Map<String, Float> weights = new LinkedHashMap<>();
        weights.put("shield", 1.5f);
        weights.put("flux", 2f);
        weights.put("phase", 1.5f);
        weights.put("energy", 2f);
        assertEquals(List.of("flux", "energy", "shield", "phase"), ScarChallengePick.strongestFirst(weights));
    }

    @Test
    void theWinnersStrongestFocusPicksTheFirstScarWithThatTag() {
        assertEquals("overloaded", ScarChallengePick.pick("tritachyon", Map.of(), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals("ironbreaker", ScarChallengePick.pick("hegemony", Map.of(), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void aCappedScarPassesToTheNextOneWithTheTag() {
        Map<String, Integer> capped = Map.of("overloaded", ScarTable.STACK_CAP);
        assertEquals("flux_pinned", ScarChallengePick.pick("tritachyon", capped, ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void aSpentTagPassesToTheNextFocusTag() {
        List<ScarRow> rows = List.of(ScarFixtures.row("overloaded", ScarDetector.OVERLOADS, 1f, "flux"),
                ScarFixtures.row("ghost_haunted", ScarDetector.PHASE_ATTACKER_SHARE, 0.3f, "phase"), ScarFixtures.fallback());
        assertEquals("ghost_haunted", ScarChallengePick.pick(List.of("flux", "phase"), rows,
                Map.of("overloaded", ScarTable.STACK_CAP), ScarFixtures.SHIELDED_WARSHIP));
    }

    @Test
    void aFactionWithoutFocusOrScarsToGiveTakesTheFallback() {
        assertEquals(ScarTable.FALLBACK_SCAR_ID, ScarChallengePick.pick("no_such_faction", Map.of(), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals(ScarTable.FALLBACK_SCAR_ID, ScarChallengePick.pick(null, Map.of(), ScarFixtures.SHIELDED_WARSHIP));
        assertEquals(ScarTable.FALLBACK_SCAR_ID, ScarChallengePick.pick(List.of("ballistic"), ScarTable.rows(), Map.of(),
                ScarFixtures.SHIELDED_WARSHIP));
    }
}
