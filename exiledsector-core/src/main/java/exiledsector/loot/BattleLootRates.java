package exiledsector.loot;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class BattleLootRates {

    public static final String DATA_PATH = ExiledSector.configPath("battle_loot.csv");
    public static final float DIFFICULTY_CAP = 3f;
    static final String DROP_COLUMN = "drop";
    static final float DEPLOYMENT_POINTS_PER_RATE_STEP = 100f;
    private static final Logger LOG = Logger.getLogger(BattleLootRates.class);
    private static final AtomicReference<Map<String, Rate>> RATES = new AtomicReference<>(Map.of());

    public record Rate(String drop, float per100DeploymentPoints, LootCurve curve, float regularShare) {

        public Rate {
            per100DeploymentPoints = Math.max(0f, per100DeploymentPoints);
            curve = curve == null ? LootCurve.LINEAR : curve;
            regularShare = Math.max(0f, regularShare);
        }

        public float expected(float lootDeploymentPoints, float battleDeploymentPoints) {
            return per100DeploymentPoints * Math.max(0f, lootDeploymentPoints) / DEPLOYMENT_POINTS_PER_RATE_STEP
                    * curve.factor(battleDeploymentPoints);
        }
    }

    private BattleLootRates() {
    }

    public static void load() {
        ModCsv.load(DROP_COLUMN, DATA_PATH, LOG, BattleLootRates::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, Rate> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(DATA_PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String drop = ModCsv.text(row, DROP_COLUMN);
                if (drop.isEmpty()) {
                    return;
                }
                String curveText = ModCsv.text(row, "curve").toLowerCase(Locale.ROOT);
                LootCurve curve = curveText.isEmpty() ? LootCurve.LINEAR : LootCurve.byIdOrNull(curveText);
                if (curve == null) {
                    csv.skipRow(drop, "unknown curve " + curveText + " (linear or diminishing)");
                    return;
                }
                loaded.put(drop, new Rate(drop, csv.number(row, "per_100_dp", 0f, drop), curve, csv.number(row, "regular_share", 0f, drop)));
            });
        }
        csv.report(LOG);
        RATES.set(Collections.unmodifiableMap(loaded));
    }

    public static void clear() {
        RATES.set(Map.of());
    }

    public static Rate rate(String drop) {
        return RATES.get().get(drop);
    }

    public static List<Rate> all() {
        return List.copyOf(RATES.get().values());
    }

    public static float difficulty(float difficultyMultiplier) {
        return Float.isFinite(difficultyMultiplier) ? Math.max(1f, Math.min(DIFFICULTY_CAP, difficultyMultiplier)) : 1f;
    }
}
