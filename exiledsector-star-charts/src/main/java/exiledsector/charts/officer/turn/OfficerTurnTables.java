package exiledsector.charts.officer.turn;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OfficerTurnTables {

    static final String RULES_PATH = ExiledSector.configPath("atlas_officer_turns.csv");
    static final String INTENTS_PATH = ExiledSector.configPath("atlas_officer_intents.csv");
    static final String ID_COLUMN = "id";
    static final String VALUE_COLUMN = "value";
    static final TurnRules DEFAULTS = new TurnRules(4, 6f, 50f, 6.25f, 10f, 1, 3, 2, 4, 50, 6, Map.of());
    static final List<String> RULE_IDS = List.of("other_boss_kills_per_turn", "expand_brake_size", "even_chance", "chance_per_rank",
            "defend_bonus", "renown_per_claim", "renown_per_challenge_won", "renown_per_defend_held", "systems_per_renown", "renown_kept_on_loss",
            "max_conflicts_per_turn");
    private static final Logger LOG = Logger.getLogger(OfficerTurnTables.class);
    private static volatile TurnRules rules = DEFAULTS;

    private OfficerTurnTables() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, RULES_PATH, LOG, OfficerTurnTables::registerRules);
        ModCsv.load(ID_COLUMN, INTENTS_PATH, LOG, OfficerTurnTables::registerIntents);
    }

    static List<String> registerRules(JSONArray rows) throws JSONException {
        Map<String, Float> values = new HashMap<>();
        CsvRows csv = new CsvRows(RULES_PATH);
        CsvRows.forEachIdRow(rows, ID_COLUMN, (ruleId, row) -> {
            if (!RULE_IDS.contains(ruleId)) {
                csv.skipRow(ruleId, "no turn rule has this id");
                return;
            }
            values.put(ruleId, csv.number(row, VALUE_COLUMN, Float.NaN, ruleId));
        });
        TurnRules loaded = new TurnRules(
                whole(values, "other_boss_kills_per_turn", DEFAULTS.otherBossKillsPerTurn()),
                number(values, "expand_brake_size", DEFAULTS.expandBrakeSize()),
                number(values, "even_chance", DEFAULTS.evenChancePercent()),
                number(values, "chance_per_rank", DEFAULTS.chancePerRankPercent()),
                number(values, "defend_bonus", DEFAULTS.defendBonusPercent()),
                whole(values, "renown_per_claim", DEFAULTS.renownPerClaim()),
                whole(values, "renown_per_challenge_won", DEFAULTS.renownPerChallengeWon()),
                whole(values, "renown_per_defend_held", DEFAULTS.renownPerDefendHeld()),
                whole(values, "systems_per_renown", DEFAULTS.systemsPerRenown()),
                whole(values, "renown_kept_on_loss", DEFAULTS.renownKeptOnLossPercent()),
                whole(values, "max_conflicts_per_turn", DEFAULTS.maxConflictsPerTurn()),
                rules.intentWeights());
        csv.report(LOG);
        rules = loaded;
        return csv.problems();
    }

    static List<String> registerIntents(JSONArray rows) throws JSONException {
        Map<String, IntentWeights> weights = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(INTENTS_PATH);
        CsvRows.forEachIdRow(rows, ID_COLUMN, (rowId, row) -> {
            if (weights.containsKey(rowId)) {
                csv.skipRow(rowId, "a row with this id is already listed");
                return;
            }
            weights.put(rowId, new IntentWeights(weight(csv, row, "expand", rowId), weight(csv, row, "push", rowId),
                    weight(csv, row, "challenge", rowId), weight(csv, row, "defend", rowId), weight(csv, row, "hold", rowId)));
        });
        csv.report(LOG);
        rules = rules.withIntentWeights(weights);
        return csv.problems();
    }

    private static float weight(CsvRows csv, JSONObject row, String column, String rowId) {
        return Math.max(0f, csv.number(row, column, 1f, rowId));
    }

    private static int whole(Map<String, Float> values, String ruleId, int fallback) {
        Float value = values.get(ruleId);
        return value == null || value.isNaN() ? fallback : Math.round(value);
    }

    private static float number(Map<String, Float> values, String ruleId, float fallback) {
        Float value = values.get(ruleId);
        return value == null || value.isNaN() ? fallback : value;
    }

    public static TurnRules rules() {
        return rules;
    }

    public static void useForTests(TurnRules testRules) {
        rules = testRules == null ? DEFAULTS : testRules;
    }
}
