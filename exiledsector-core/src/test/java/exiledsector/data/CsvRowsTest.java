package exiledsector.data;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CsvRowsTest {

    @Test
    void aRowThatThrowsIsSkippedAndLoggedWhileTheOthersLoad() throws Exception {
        Logger log = mock(Logger.class);
        JSONArray rows = new JSONArray().put(new JSONObject().put("id", "a")).put(new JSONObject().put("id", "bad")).put(new JSONObject().put("id", "c"));
        List<String> loaded = new ArrayList<>();

        CsvRows.forEachRow(rows, "data/test.csv", log, (index, row) -> {
            if (row.getString("id").equals("bad")) {
                throw new IllegalArgumentException("bad is not allowed");
            }
            loaded.add(row.getString("id"));
        });

        assertEquals(List.of("a", "c"), loaded);
        verify(log).error(contains("Skipping row 2 of data/test.csv: bad is not allowed"));
    }

    @Test
    void missingRowsLoadNothing() throws Exception {
        List<String> loaded = new ArrayList<>();

        CsvRows.forEachRow(null, "data/test.csv", mock(Logger.class), (index, row) -> loaded.add("row"));

        assertEquals(List.of(), loaded);
    }

    @Test
    void entriesAreTrimmedAndBlanksDropped() {
        assertEquals(List.of("hegemony", "luddic_church"), CsvRows.entries(" hegemony ; ;luddic_church;"));
        assertEquals(List.of(), CsvRows.entries(""));
    }

    @Test
    void aChanceIsANumberOfZeroOrMoreAndAnythingElseCountsAsZero() {
        Logger log = mock(Logger.class);

        assertEquals(0.25f, CsvRows.chance("0.25", "anchor", "drop_chance", "data/test.csv", log));
        assertEquals(0f, CsvRows.chance("", "anchor", "drop_chance", "data/test.csv", log));
        assertEquals(0f, CsvRows.chance("6%", "anchor", "drop_chance", "data/test.csv", log));
        verify(log).error(contains("anchor in data/test.csv has the drop_chance \"6%\""));
        assertTrue(CsvRows.isValidChance(""));
        assertFalse(CsvRows.isValidChance("-1"));
        assertFalse(CsvRows.isValidChance("Infinity"));
    }
}
