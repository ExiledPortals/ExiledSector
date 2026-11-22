package exiledsector.charts.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import exiledsector.charts.ChartTables;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TerritoryExclusionsTest {

    @AfterEach
    void tearDown() {
        TerritoryExclusions.useForTests(Set.of());
    }

    @Test
    void theShippedListKeepsFactionsWithoutTerritoryOffTheBoard() throws Exception {
        Set<String> shipped = TerritoryExclusions.factionIds(ChartTables.csvRows(TerritoryExclusions.PATH));

        assertEquals(Set.of("remnant", "derelict", "threat", "dweller", "rat_abyssals"), shipped);
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

    @Test
    void loadingJoinsTheNewListWithFactionsModsStillMergeIntoTheOldOne() throws Exception {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.getMergedSpreadsheetDataForMod(TerritoryExclusions.FACTION_COLUMN, TerritoryExclusions.PATH, "exiledSector"))
                .thenReturn(rows("remnant"));
        when(settings.getMergedSpreadsheetDataForMod(TerritoryExclusions.FACTION_COLUMN, TerritoryExclusions.OLD_PATH, "exiledSector"))
                .thenReturn(rows("some_mod_faction"));
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);

            TerritoryExclusions.load();
        }

        assertTrue(TerritoryExclusions.excludes("remnant"));
        assertTrue(TerritoryExclusions.excludes("some_mod_faction"));
        assertFalse(TerritoryExclusions.excludes("hegemony"));
    }

    private static JSONArray rows(String factionId) throws Exception {
        return new JSONArray().put(new JSONObject().put(TerritoryExclusions.FACTION_COLUMN, factionId));
    }
}
