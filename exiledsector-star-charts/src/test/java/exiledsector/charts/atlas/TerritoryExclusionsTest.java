package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerritoryExclusionsTest {

    @AfterEach
    void tearDown() {
        TerritoryExclusions.useForTests(Set.of());
    }

    @Test
    void theShippedListKeepsFactionsWithoutTerritoryOffTheBoard() throws Exception {
        Set<String> shipped = TerritoryExclusions.factionIds(ChartTables.csvRows(TerritoryExclusions.PATH));

        assertEquals(Set.of("remnant", "derelict", "threat", "rat_abyssals"), shipped);
        TerritoryExclusions.useForTests(shipped);
        assertTrue(TerritoryExclusions.excludes("remnant"));
        assertTrue(TerritoryExclusions.excludes(null));
        assertFalse(TerritoryExclusions.excludes("hegemony"));
    }

    @Test
    void theOldListStaysReadableForModsThatStillMergeIntoIt() throws Exception {
        assertEquals(Set.of(), TerritoryExclusions.factionIds(ChartTables.csvRows(TerritoryExclusions.OLD_PATH)));
        assertEquals(Set.of(), TerritoryExclusions.factionIds(null));
    }
}
