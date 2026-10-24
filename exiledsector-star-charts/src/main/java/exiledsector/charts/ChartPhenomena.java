package exiledsector.charts;

import exiledlib.ModCsv;
import exiledsector.charts.phenomenon.AsteroidStorm;
import exiledsector.charts.phenomenon.ChartPhenomenon;
import exiledsector.charts.phenomenon.MagneticStorm;
import exiledsector.charts.phenomenon.StarPhenomena;
import exiledsector.charts.phenomenon.StellarFlares;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChartPhenomena {

    static final String PATH = "data/config/exiledSector/chart_phenomena.csv";
    static final String ID_COLUMN = "id";
    static final long ROLL_SALT = 0x5048454e4f4dL;
    static final List<ChartPhenomenon> BUILT_IN = List.of(StarPhenomena.BLACK_HOLE, StarPhenomena.NEUTRON_STAR, StarPhenomena.DENSE_NEBULA,
            AsteroidStorm.INSTANCE, MagneticStorm.INSTANCE, StellarFlares.INSTANCE);
    private static final Logger LOG = Logger.getLogger(ChartPhenomena.class);
    private static volatile Map<String, Definition> definitions = Map.of();

    public record Definition(String id, float weight, int minTier, int maxTier, String requiredMod, String icon, ChartPhenomenon.Kind kind) {

        public Definition {
            requiredMod = requiredMod == null ? "" : requiredMod;
            icon = icon == null ? "" : icon;
            weight = Math.max(0f, weight);
        }

        public boolean allowedAt(int tier) {
            return tier >= minTier && tier <= maxTier;
        }
    }

    private ChartPhenomena() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, ChartPhenomena::register);
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
                ChartPhenomenon phenomenon = built(id);
                if (phenomenon == null) {
                    csv.skipRow(id, "no phenomenon has this id");
                    return;
                }
                Definition definition = parse(csv, row, id);
                if (definition.kind() != phenomenon.kind()) {
                    csv.problem(id, "kind", definition.kind() + " (this phenomenon is " + phenomenon.kind() + ")");
                    definition = new Definition(id, definition.weight(), definition.minTier(), definition.maxTier(), definition.requiredMod(),
                            definition.icon(), phenomenon.kind());
                }
                loaded.put(id, definition);
            });
        }
        csv.report(LOG);
        definitions = Map.copyOf(loaded);
        LOG.info("[ExiledSector] Star Charts can roll " + loaded.size() + " phenomenon/phenomena");
        return csv.problems();
    }

    private static Definition parse(CsvRows csv, JSONObject row, String id) {
        int minTier = ChartTiers.clamp(csv.integer(row, "min_tier", ChartTiers.LOWEST_TIER, id));
        int maxTier = ChartTiers.clamp(csv.integer(row, "max_tier", ChartTiers.HIGHEST_TIER, id));
        ChartPhenomenon.Kind kind = ChartPhenomenon.Kind.parse(ModCsv.text(row, "kind"));
        return new Definition(id, csv.number(row, "weight", 1f, id), Math.min(minTier, maxTier), Math.max(minTier, maxTier),
                ModCsv.text(row, "requires_mod"), ModCsv.text(row, "icon"), kind == null ? ChartPhenomenon.Kind.OVERLAY : kind);
    }

    public static Collection<Definition> definitions() {
        return definitions.values();
    }

    public static Definition definition(String id) {
        return id == null ? null : definitions.get(id);
    }

    public static ChartPhenomenon byId(String id) {
        return id == null || !definitions.containsKey(id) ? null : built(id);
    }

    public static ChartPhenomenon availableAt(String id, int tier) {
        Definition definition = definition(id);
        if (definition == null || !definition.allowedAt(tier) || !ChartPois.modRequirementMet(definition.requiredMod())) {
            return null;
        }
        return built(id);
    }

    public static String rolledOverlay(long seed, int tier, ChartGenContext context) {
        String winner = "";
        float winningRoll = Float.POSITIVE_INFINITY;
        for (Definition definition : definitions.values()) {
            float chance = context == null ? 0f : context.phenomenonChance(definition.id()) / ChartGenContext.PERCENT;
            if (chance <= 0f || definition.kind() != ChartPhenomenon.Kind.OVERLAY || availableAt(definition.id(), tier) == null) {
                continue;
            }
            float roll = ChartLayoutGenerator.derivedRandom(seed ^ ((long) definition.id().hashCode() << 32), ROLL_SALT).nextFloat();
            if (roll < chance && roll < winningRoll) {
                winner = definition.id();
                winningRoll = roll;
            }
        }
        return winner;
    }

    static ChartPhenomenon built(String id) {
        for (ChartPhenomenon phenomenon : BUILT_IN) {
            if (phenomenon.id().equals(id)) {
                return phenomenon;
            }
        }
        return null;
    }

    static void useForTests(List<Definition> testDefinitions) {
        Map<String, Definition> byId = new LinkedHashMap<>();
        testDefinitions.forEach(definition -> byId.put(definition.id(), definition));
        definitions = Map.copyOf(byId);
    }

    static void resetForTests() {
        definitions = Map.of();
    }
}
