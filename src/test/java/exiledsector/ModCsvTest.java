package exiledsector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModCsvTest {

    private static final String PATH = "data/config/exiledSector/example.csv";

    private MockedStatic<Global> globalMock;
    private SettingsAPI settings;

    @BeforeEach
    void setUp() {
        settings = mock(SettingsAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    @Test
    void readsTheMergedSheetOfThisModByItsIdColumn() throws Exception {
        JSONArray rows = new JSONArray().put(new JSONObject().put("id", "a"));
        when(settings.getMergedSpreadsheetDataForMod("id", PATH, "exiledSector")).thenReturn(rows);
        List<JSONArray> handled = new ArrayList<>();

        ModCsv.load("id", PATH, mock(Logger.class), handled::add);

        assertSame(rows, ModCsv.rows("id", PATH));
        assertEquals(List.of(rows), handled);
    }

    @Test
    void anUnreadableSheetIsLoggedWithItsPathInsteadOfThrown() throws Exception {
        IOException missing = new IOException("missing");
        when(settings.getMergedSpreadsheetDataForMod("id", PATH, "exiledSector")).thenThrow(missing);
        Logger log = mock(Logger.class);

        ModCsv.load("id", PATH, log, rows -> {
            throw new AssertionError("no rows to handle");
        });

        verify(log).error("Failed to load " + PATH, missing);
    }

    @Test
    void aHandlerThatRejectsTheRowsIsLoggedToo() throws Exception {
        when(settings.getMergedSpreadsheetDataForMod("id", PATH, "exiledSector")).thenReturn(new JSONArray());
        Logger log = mock(Logger.class);

        ModCsv.load("id", PATH, log, rows -> {
            throw new JSONException("bad row");
        });

        verify(log).error(eq("Failed to load " + PATH), any(JSONException.class));
    }

    @Test
    void visitsEveryRowInOrderWithItsIndex() throws Exception {
        JSONArray rows = new JSONArray().put(new JSONObject().put("id", "a")).put(new JSONObject().put("id", "b"));
        List<String> visited = new ArrayList<>();

        ModCsv.forEach(rows, (index, row) -> visited.add(index + ":" + row.getString("id")));

        assertEquals(List.of("0:a", "1:b"), visited);
        assertThrows(JSONException.class, () -> ModCsv.forEach(new JSONArray().put("not a row"), (index, row) -> {
        }));
    }

    @Test
    void cellTextIsTrimmedAndMissingCellsAreEmpty() throws Exception {
        JSONObject row = new JSONObject().put("id", "  padded \t").put("blank", "   ");

        assertEquals("padded", ModCsv.text(row, "id"));
        assertTrue(ModCsv.text(row, "blank").isEmpty());
        assertTrue(ModCsv.text(row, "missing").isEmpty());
    }
}
