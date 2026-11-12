package exiledsector.charts.officer;

import exiledsector.charts.ChartTables;
import exiledsector.i18n.Translation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OfficerRanksTest {

    @AfterEach
    void restoreDefaults() {
        OfficerRanks.useForTests(List.of());
    }

    private static JSONObject row(String rankId, int places) throws JSONException {
        return new JSONObject().put("id", rankId).put("boss_size", "10").put("fleet_strength", "5").put("actions", "1")
                .put("places", Integer.toString(places));
    }

    @Test
    void theShippedRankTableLoadsCleanlyAndMatchesTheDefaults() throws Exception {
        assertEquals(List.of(), OfficerRanks.register(ChartTables.csvRows(OfficerRanks.PATH)));
        assertEquals(OfficerRanks.DEFAULTS, OfficerRanks.all());
    }

    @Test
    void everyRankHasATranslatedName() {
        for (OfficerRank rank : OfficerRanks.DEFAULTS) {
            assertEquals(Translation.text("chart.officer.rank." + rank.id()), rank.displayName());
        }
    }

    @Test
    void aRepeatedRankIsSkippedAndReported() throws Exception {
        JSONArray rows = new JSONArray().put(row("ensign", 4)).put(row("ensign", 9)).put(row("marshal", 1));

        List<String> problems = OfficerRanks.register(rows);

        assertFalse(problems.isEmpty());
        assertEquals(List.of("ensign", "marshal"), OfficerRanks.all().stream().map(OfficerRank::id).toList());
        assertEquals(4, OfficerRanks.rank("ensign").places());
    }

    @Test
    void ranksAreOrderedLowestFirstAndAnUnknownRankFallsBackToTheLowest() {
        assertEquals("lieutenant", OfficerRanks.lowest().id());
        assertEquals(4, OfficerRanks.order("admiral"));
        assertEquals(0, OfficerRanks.order("grand_marshal"));
        assertEquals(OfficerRanks.lowest(), OfficerRanks.rank("grand_marshal"));
    }
}
