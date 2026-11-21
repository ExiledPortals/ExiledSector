package exiledsector.ui.atlas;

import exiledsector.charts.atlas.AtlasCell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasSearchTest {

    private static final AtlasCell HEGEMONY_NEBULA = new AtlasCell(0, 0);
    private static final AtlasCell PIRATE_RIFT = new AtlasCell(1, 0);
    private static final AtlasCell HEGEMONY_RIFT = new AtlasCell(2, 0);

    private final AtlasSearch search = new AtlasSearch();

    @BeforeEach
    void index() {
        Map<AtlasCell, List<String>> terms = new LinkedHashMap<>();
        terms.put(HEGEMONY_NEBULA, List.of("Corvus Reach", "Hegemony", "Ion Storm"));
        terms.put(PIRATE_RIFT, List.of("Abyssal Rift", "Pirates"));
        terms.put(HEGEMONY_RIFT, List.of("Abyssal Rift", "Hegemony", "Ion Storm"));
        search.index(terms);
    }

    @Test
    void anEmptyOrBlankQueryHighlightsNothingAndDimsNothing() {
        for (String query : new String[] {"", "   ", null}) {
            search.setQuery(query);

            assertFalse(search.isActive());
            assertFalse(search.matches(HEGEMONY_NEBULA));
            assertEquals(1f, search.alpha(PIRATE_RIFT));
            assertEquals(1f, search.laneAlpha(HEGEMONY_NEBULA, PIRATE_RIFT));
            assertEquals(1f, search.backgroundAlpha());
            assertNull(search.nextMatch());
        }
    }

    @Test
    void theQueryMatchesAnyPartOfAnyTermIgnoringCaseAndOuterSpaces() {
        search.setQuery("  ION st ");

        assertTrue(search.isActive());
        assertTrue(search.matches(HEGEMONY_NEBULA));
        assertFalse(search.matches(PIRATE_RIFT));
        assertTrue(search.matches(HEGEMONY_RIFT));
        assertEquals("  ION st ", search.getQuery());
    }

    @Test
    void aQueryCannotMatchAcrossTwoTerms() {
        search.setQuery("rift pirates");

        assertFalse(search.matches(PIRATE_RIFT));
    }

    @Test
    void unmatchedSystemsLanesAndTheBackdropDimWhileMatchesStayLit() {
        search.setQuery("rift");

        assertEquals(1f, search.alpha(PIRATE_RIFT));
        assertEquals(AtlasSearch.DIM_ALPHA, search.alpha(HEGEMONY_NEBULA));
        assertEquals(1f, search.laneAlpha(PIRATE_RIFT, HEGEMONY_RIFT));
        assertEquals(AtlasSearch.DIM_ALPHA, search.laneAlpha(HEGEMONY_NEBULA, PIRATE_RIFT));
        assertEquals(AtlasSearch.DIM_ALPHA, search.backgroundAlpha());
    }

    @Test
    void jumpingCyclesThroughTheMatchesInOrderAndRestartsWhenTheQueryChanges() {
        search.setQuery("hegemony");

        assertEquals(HEGEMONY_NEBULA, search.nextMatch());
        assertEquals(HEGEMONY_RIFT, search.nextMatch());
        assertEquals(HEGEMONY_NEBULA, search.nextMatch());

        search.setQuery("hegemony");
        assertEquals(HEGEMONY_RIFT, search.nextMatch());

        search.setQuery("abyssal");
        assertEquals(PIRATE_RIFT, search.nextMatch());
    }

    @Test
    void aQueryWithNoMatchHasNothingToJumpTo() {
        search.setQuery("tri-tachyon");

        assertTrue(search.isActive());
        assertNull(search.nextMatch());
        assertEquals(AtlasSearch.DIM_ALPHA, search.alpha(HEGEMONY_RIFT));
    }

    @Test
    void reindexingKeepsTheQueryAndRefreshesItsMatches() {
        search.setQuery("pirates");
        assertTrue(search.matches(PIRATE_RIFT));

        search.index(Map.of(HEGEMONY_NEBULA, List.of("Pirates"), PIRATE_RIFT, List.of("Abyssal Rift")));

        assertTrue(search.matches(HEGEMONY_NEBULA));
        assertFalse(search.matches(PIRATE_RIFT));
        assertEquals("pirates", search.getQuery());
    }
}
