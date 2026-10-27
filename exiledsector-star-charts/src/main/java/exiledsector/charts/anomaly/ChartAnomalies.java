package exiledsector.charts.anomaly;

import exiledlib.ModCsv;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.debris.AnomalousDebris;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.table.BuiltInTable;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ChartAnomalies {

    public static final String PATH = "data/config/exiledSector/chart_anomalies.csv";
    static final String ID_COLUMN = "id";
    static final List<ChartAnomaly> BUILT_IN = List.of(StarAnomalies.BLACK_HOLE, StarAnomalies.NEUTRON_STAR, StarAnomalies.DENSE_NEBULA,
            AsteroidStorm.INSTANCE, MagneticStorm.INSTANCE, StellarFlares.INSTANCE, SpatialBreach.INSTANCE, FactionSkirmish.INSTANCE,
            AnomalousDebris.INSTANCE);
    private static final Logger LOG = Logger.getLogger(ChartAnomalies.class);
    private static volatile List<ChartAnomaly> anomalies = BUILT_IN;
    private static volatile Map<String, Definition> definitions = Map.of();

    public record Definition(String id, float weight, int minTier, int maxTier, String requiredMod, String icon, boolean insertable,
                             Set<String> excludes, boolean replacesStar, List<ChartGenContext.AppliedEffect> effects) {

        public Definition {
            requiredMod = requiredMod == null ? "" : requiredMod;
            icon = icon == null ? "" : icon;
            weight = Math.max(0f, weight);
            excludes = excludes == null ? Set.of() : Set.copyOf(excludes);
            insertable = insertable && !replacesStar;
            effects = effects == null ? List.of() : List.copyOf(effects);
        }

        public Definition(String id, float weight, int minTier, int maxTier, String requiredMod, String icon, boolean insertable,
                          Set<String> excludes, boolean replacesStar) {
            this(id, weight, minTier, maxTier, requiredMod, icon, insertable, excludes, replacesStar, List.of());
        }

        public boolean allowedAt(int tier) {
            return tier >= minTier && tier <= maxTier;
        }

        public boolean conflictsWith(Definition other) {
            return other != null && !id.equals(other.id) && (replacesStar && other.replacesStar || excludes.contains(other.id) || other.excludes.contains(id));
        }
    }

    private ChartAnomalies() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, PATH, LOG, ChartAnomalies::register);
    }

    public static List<String> register(JSONArray rows) throws JSONException {
        CsvRows csv = new CsvRows(PATH);
        Map<String, Definition> loaded = BuiltInTable.rows(rows, csv, "anomaly", id -> byId(id) != null, ChartAnomalies::parse);
        csv.report(LOG);
        definitions = Map.copyOf(loaded);
        LOG.info("[ExiledSector] Star Charts can roll " + loaded.size() + " anomaly/anomalies");
        return csv.problems();
    }

    private static Definition parse(CsvRows csv, JSONObject row, String id) {
        BuiltInTable.TierRange tiers = BuiltInTable.TierRange.parse(csv, row, id);
        boolean replacesStar = byId(id).replacesStar();
        boolean insertable = csv.flag(row, "insertable", false, id);
        if (insertable && replacesStar) {
            csv.problem(id, "insertable", "yes (an anomaly that replaces the star can't be inserted)");
        }
        Set<String> excludes = new LinkedHashSet<>();
        for (String excluded : CsvRows.entries(ModCsv.text(row, "excludes"))) {
            if (excluded.equals(id) || byId(excluded) == null) {
                csv.problem(id, "excludes", excluded);
            } else {
                excludes.add(excluded);
            }
        }
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
        csv.weights(row, "effects", id, null, true).forEach((name, value) -> {
            ChartEffect effect = ChartEffect.byNameOrNull(name);
            if (effect == null) {
                csv.problem(id, "effects", name + " (not a chart effect)");
            } else {
                effects.add(new ChartGenContext.AppliedEffect(effect, value));
            }
        });
        return new Definition(id, csv.number(row, "weight", 1f, id), tiers.minTier(), tiers.maxTier(), ModCsv.text(row, "requires_mod"),
                ModCsv.text(row, "icon"), insertable, excludes, replacesStar, effects);
    }

    public static ChartAnomaly byId(String id) {
        return BuiltInTable.find(anomalies, ChartAnomaly::id, id);
    }

    public static Collection<Definition> definitions() {
        return definitions.values();
    }

    public static Definition definition(String id) {
        return id == null ? null : definitions.get(id);
    }

    public static boolean usable(String id) {
        Definition definition = definition(id);
        return definition != null && byId(id) != null && ChartPois.modRequirementMet(definition.requiredMod());
    }

    public static boolean conflicts(String first, String second) {
        Definition definition = definition(first);
        return definition != null && definition.conflictsWith(definition(second));
    }

    public static boolean accepts(List<String> accepted, String id) {
        if (!usable(id) || accepted.contains(id)) {
            return false;
        }
        for (String existing : accepted) {
            if (conflicts(existing, id)) {
                return false;
            }
        }
        return true;
    }

    public static ChartAnomaly starOf(Collection<String> ids) {
        for (String id : ids) {
            ChartAnomaly anomaly = byId(id);
            if (anomaly != null && anomaly.replacesStar() && usable(id)) {
                return anomaly;
            }
        }
        return null;
    }

    public static List<ChartAnomaly> anomaliesOf(Collection<String> ids) {
        return ids.stream().map(ChartAnomalies::byId).filter(Objects::nonNull).toList();
    }

    public static List<ChartGenContext.AppliedEffect> effectsOf(Collection<String> ids) {
        List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
        for (String id : ids) {
            Definition definition = definition(id);
            if (definition != null) {
                effects.addAll(definition.effects());
            }
        }
        return effects;
    }

    public static List<String> planned(ChartGenContext context) {
        if (context == null) {
            return List.of();
        }
        List<String> accepted = new ArrayList<>();
        for (String id : context.forcedAnomalies()) {
            if (accepts(accepted, id)) {
                accepted.add(id);
            }
        }
        return List.copyOf(accepted);
    }

    public static void useForTests(List<ChartAnomaly> testAnomalies, List<Definition> testDefinitions) {
        anomalies = List.copyOf(testAnomalies);
        Map<String, Definition> byId = new LinkedHashMap<>();
        testDefinitions.forEach(definition -> byId.put(definition.id(), definition));
        definitions = Map.copyOf(byId);
    }

    public static void useForTests(List<Definition> testDefinitions) {
        useForTests(BUILT_IN, testDefinitions);
    }

    public static void resetForTests() {
        anomalies = BUILT_IN;
        definitions = Map.of();
    }
}
