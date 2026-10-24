package exiledsector.charts;

import exiledsector.data.CsvRows;
import exiledlib.ModCsv;
import exiledsector.charts.expansion.ChartExpansion;
import exiledsector.charts.expansion.SpatialBreach;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ChartExpansions {

    static final String PATH = "data/config/exiledSector/chart_expansions.csv";
    static final String ID_COLUMN = "id";
    static final long ROLL_SALT = 0x43484558504e44L;
    static final List<ChartExpansion> BUILT_IN = List.of(SpatialBreach.INSTANCE);
    private static final Logger LOG = Logger.getLogger(ChartExpansions.class);
    private static volatile List<ChartExpansion> expansions = BUILT_IN;
    private static volatile Map<String, Definition> definitions = Map.of();

    public record Definition(String id, float weight, int minTier, int maxTier, String requiredMod, String icon) {

        public Definition {
            requiredMod = requiredMod == null ? "" : requiredMod;
            icon = icon == null ? "" : icon;
            weight = Math.max(0f, weight);
        }

        public boolean allowedAt(int tier) {
            return tier >= minTier && tier <= maxTier;
        }
    }

    private ChartExpansions() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, ChartExpansions::register);
    }

    static List<String> register(JSONArray rows) throws JSONException {
        Map<String, Definition> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String id = ModCsv.text(row, ID_COLUMN);
                if (id.isEmpty()) {
                    return;
                }
                if (byId(id) == null) {
                    csv.skipRow(id, "no expansion has this id");
                    return;
                }
                loaded.put(id, parse(csv, row, id));
            });
        }
        csv.report(LOG);
        definitions = Map.copyOf(loaded);
        LOG.info("[ExiledSector] Star Charts can roll " + loaded.size() + " expansion(s)");
        return csv.problems();
    }

    private static Definition parse(CsvRows csv, JSONObject row, String id) {
        int minTier = ChartTiers.clamp(csv.integer(row, "min_tier", ChartTiers.LOWEST_TIER, id));
        int maxTier = ChartTiers.clamp(csv.integer(row, "max_tier", ChartTiers.HIGHEST_TIER, id));
        return new Definition(id, csv.number(row, "weight", 1f, id), Math.min(minTier, maxTier), Math.max(minTier, maxTier),
                ModCsv.text(row, "requires_mod"), ModCsv.text(row, "icon"));
    }

    public static ChartExpansion byId(String id) {
        if (id == null) {
            return null;
        }
        for (ChartExpansion expansion : expansions) {
            if (expansion.id().equals(id)) {
                return expansion;
            }
        }
        return null;
    }

    public static Definition definition(String id) {
        return id == null ? null : definitions.get(id);
    }

    static boolean available(String id, int tier) {
        Definition definition = definition(id);
        return definition != null && definition.allowedAt(tier) && ChartPois.modRequirementMet(definition.requiredMod());
    }

    static List<String> planned(long seed, int tier, ChartGenContext context) {
        if (context == null) {
            return List.of();
        }
        List<String> planned = new ArrayList<>();
        for (ChartExpansion expansion : expansions) {
            float chance = context.expansionChance(expansion.id()) / ChartGenContext.PERCENT;
            if (chance > 0f && available(expansion.id(), tier) && rollFor(seed, expansion.id()).nextFloat() < chance) {
                planned.add(expansion.id());
            }
        }
        return List.copyOf(planned);
    }

    static Random rollFor(long seed, String expansionId) {
        return ChartLayoutGenerator.derivedRandom(seed ^ ((long) expansionId.hashCode() << 32), ROLL_SALT);
    }

    static void useForTests(List<ChartExpansion> testExpansions, List<Definition> testDefinitions) {
        expansions = List.copyOf(testExpansions);
        Map<String, Definition> byId = new LinkedHashMap<>();
        testDefinitions.forEach(definition -> byId.put(definition.id(), definition));
        definitions = Map.copyOf(byId);
    }

    static void resetForTests() {
        expansions = BUILT_IN;
        definitions = Map.of();
    }
}
