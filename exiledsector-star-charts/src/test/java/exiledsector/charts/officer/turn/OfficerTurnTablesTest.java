package exiledsector.charts.officer.turn;

import exiledsector.charts.ChartTables;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OfficerTurnTablesTest {

    @AfterEach
    void restoreDefaults() {
        OfficerTurnTables.useForTests(null);
    }

    @Test
    void theShippedTablesLoadCleanlyAndMatchTheDefaults() throws Exception {
        assertEquals(List.of(), OfficerTurnTables.registerRules(ChartTables.csvRows(OfficerTurnTables.RULES_PATH)));
        assertEquals(List.of(), OfficerTurnTables.registerIntents(ChartTables.csvRows(OfficerTurnTables.INTENTS_PATH)));

        TurnRules rules = OfficerTurnTables.rules();
        assertEquals(OfficerTurnTables.DEFAULTS, rules.withIntentWeights(Map.of()));
        assertEquals(9f, rules.intentWeight("admiral", "reckless", IntentKind.CHALLENGE));
        assertEquals(2f, rules.intentWeight("lieutenant", "steady", IntentKind.DEFEND));
    }

    @Test
    void anUnknownRuleIsReportedAndAMissingRuleKeepsItsDefault() throws Exception {
        JSONArray rows = new JSONArray()
                .put(new JSONObject().put(OfficerTurnTables.ID_COLUMN, "max_conflicts_per_turn").put(OfficerTurnTables.VALUE_COLUMN, "2"))
                .put(new JSONObject().put(OfficerTurnTables.ID_COLUMN, "free_lunch").put(OfficerTurnTables.VALUE_COLUMN, "1"));

        assertFalse(OfficerTurnTables.registerRules(rows).isEmpty());
        assertEquals(2, OfficerTurnTables.rules().maxConflictsPerTurn());
        assertEquals(OfficerTurnTables.DEFAULTS.otherBossKillsPerTurn(), OfficerTurnTables.rules().otherBossKillsPerTurn());
    }

    @Test
    void aNegativeIntentWeightCountsAsNone() throws Exception {
        JSONArray rows = new JSONArray().put(new JSONObject().put(OfficerTurnTables.ID_COLUMN, "timid").put("expand", "1").put("push", "-2")
                .put("challenge", "1").put("defend", "1").put("hold", "1"));

        OfficerTurnTables.registerIntents(rows);

        assertEquals(0f, OfficerTurnTables.rules().intentWeight("lieutenant", "timid", IntentKind.PUSH));
    }
}
