package exiledsector.charts.atlas;

import exiledlib.ModCsv;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.data.CsvRows;
import exiledsector.items.WeightedPick;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class AtlasCatalysts {

    public static final String PATH = "data/config/exiledSector/atlas_catalysts.csv";
    static final String ID_COLUMN = "id";
    private static final Logger LOG = Logger.getLogger(AtlasCatalysts.class);
    private static volatile Map<String, Catalyst> catalysts = Map.of();

    public enum Family {
        TERMINUS,
        SURVEY,
        ORDERS;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Family byKey(String key) {
            for (Family family : values()) {
                if (family.key().equals(key)) {
                    return family;
                }
            }
            return null;
        }
    }

    public enum Effect {
        ANOMALY,
        REDEPLOY;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Effect byKey(String key) {
            if (key.isEmpty()) {
                return ANOMALY;
            }
            for (Effect effect : values()) {
                if (effect.key().equals(key)) {
                    return effect;
                }
            }
            return null;
        }
    }

    public record Catalyst(String itemId, String anomalyId, Family family, float weight, int minTier, Effect effect, String icon) {

        public Catalyst(String itemId, String anomalyId, Family family, float weight, int minTier) {
            this(itemId, anomalyId, family, weight, minTier, Effect.ANOMALY, "");
        }

        public String iconPath() {
            if (!icon.isEmpty()) {
                return icon;
            }
            ChartAnomalies.Definition definition = ChartAnomalies.definition(anomalyId);
            return definition == null ? "" : definition.icon();
        }
    }

    private AtlasCatalysts() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, AtlasCatalysts::register);
    }

    public static List<String> register(JSONArray rows) throws JSONException {
        CsvRows csv = new CsvRows(PATH);
        Map<String, Catalyst> loaded = new LinkedHashMap<>();
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
            String anomalyId = ModCsv.text(row, "anomaly");
            String icon = ModCsv.text(row, "icon");
            Effect effect = Effect.byKey(ModCsv.text(row, "effect"));
            String skipReason = effect == null ? "effect '" + ModCsv.text(row, "effect") + "' is not anomaly or redeploy"
                    : skipReason(effect, anomalyId, icon);
            Family family = Family.byKey(ModCsv.text(row, "family"));
            if (family == null) {
                skipReason = "family '" + ModCsv.text(row, "family") + "' is not terminus, survey or orders";
            }
            if (skipReason != null) {
                csv.skipRow(id, skipReason);
                return;
            }
            int minTier = ChartTiers.clamp(csv.integer(row, "min_tier", ChartTiers.LOWEST_TIER, id));
            loaded.put(id, new Catalyst(id, effect == Effect.ANOMALY ? anomalyId : "", family, Math.max(0f, csv.number(row, "weight", 1f, id)),
                    minTier, effect, icon));
        });
        csv.report(LOG);
        catalysts = Collections.unmodifiableMap(loaded);
        LOG.info("[ExiledSector] The atlas has " + loaded.size() + " Atlas Catalyst(s)");
        return csv.problems();
    }

    private static String skipReason(Effect effect, String anomalyId, String icon) {
        if (effect == Effect.REDEPLOY) {
            return icon.isEmpty() ? "a redeploy catalyst needs an icon" : null;
        }
        ChartAnomalies.Definition definition = ChartAnomalies.definition(anomalyId);
        if (definition == null || ChartAnomalies.byId(anomalyId) == null) {
            return "anomaly '" + anomalyId + "' is unknown";
        }
        if (!definition.insertable()) {
            return "anomaly '" + anomalyId + "' is not insertable";
        }
        if (!ChartPois.modRequirementMet(definition.requiredMod())) {
            return "anomaly '" + anomalyId + "' needs the missing mod '" + definition.requiredMod() + "'";
        }
        return definition.icon().isEmpty() && icon.isEmpty() ? "anomaly '" + anomalyId + "' has no icon" : null;
    }

    public static Collection<Catalyst> all() {
        return catalysts.values();
    }

    public static Catalyst byItemId(String itemId) {
        return itemId == null ? null : catalysts.get(itemId);
    }

    public static Catalyst pickDrop(int tier, Random random) {
        List<Catalyst> droppable = catalysts.values().stream().filter(catalyst -> catalyst.weight() > 0f && catalyst.minTier() <= tier).toList();
        return WeightedPick.pick(droppable, Catalyst::weight, random);
    }

    public static void useForTests(List<Catalyst> testCatalysts) {
        Map<String, Catalyst> byItemId = new LinkedHashMap<>();
        testCatalysts.forEach(catalyst -> byItemId.put(catalyst.itemId(), catalyst));
        catalysts = Collections.unmodifiableMap(byItemId);
    }

    public static void resetForTests() {
        catalysts = Map.of();
    }
}
