package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledlib.ModCsv;
import exiledlib.compat.CompatTarget;
import exiledsector.charts.ChartExpansions;
import exiledsector.charts.ChartFaction;
import exiledsector.charts.ChartFactions;
import exiledsector.charts.expansion.ChartExpansion;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AtlasData {

    static final String REGIONS_PATH = "data/config/exiledSector/atlas_regions.csv";
    static final String LAYOUTS_PATH = "data/config/exiledSector/atlas_layouts.csv";
    static final String MARKERS_PATH = "data/config/exiledSector/atlas_markers.csv";
    static final String SPECIALS_PATH = "data/config/exiledSector/atlas_specials.csv";
    static final String ID_COLUMN = "id";
    static final String VOID_MARKER = "void";
    static final String POWERFUL_BOSS_MARKER = "powerful_boss";
    static final String EXPANSION_MARKER = "expansion";
    static final String PHENOMENON_MARKER = "phenomenon";
    static final String WEIGHT_SEPARATOR = ":";
    private static final Logger LOG = Logger.getLogger(AtlasData.class);
    private static volatile Map<String, Float> layoutWeights = Map.of();
    private static volatile List<AtlasRegion> regions = List.of();
    private static volatile Map<String, AtlasMarkerRule> markerRules = Map.of();
    private static volatile List<AtlasSpecial> specials = List.of();

    private AtlasData() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, LAYOUTS_PATH, LOG, AtlasData::registerLayouts);
        ModCsv.load(ID_COLUMN, REGIONS_PATH, LOG, rows -> registerRegions(rows, factionIds()));
        ModCsv.load(ID_COLUMN, MARKERS_PATH, LOG, AtlasData::registerMarkers);
        ModCsv.load(ID_COLUMN, SPECIALS_PATH, LOG, AtlasData::registerSpecials);
        LOG.info("[ExiledSector] The atlas has " + regions.size() + " region(s), " + layoutWeights.size() + " layout(s) and "
                + specials.size() + " special site(s)");
    }

    public static AtlasRules rules() {
        Map<String, Float> factionWeights = new LinkedHashMap<>();
        for (ChartFaction faction : ChartFactions.all()) {
            factionWeights.put(faction.factionId(), faction.weight());
        }
        List<AtlasMarkerCandidate> expansions = new ArrayList<>();
        for (ChartExpansions.Definition definition : ChartExpansions.definitions()) {
            ChartExpansion expansion = ChartExpansions.byId(definition.id());
            if (expansion != null) {
                expansions.add(new AtlasMarkerCandidate(definition.id(), definition.weight(), definition.requiredMod(), expansion::atlasWeight));
            }
        }
        Map<String, AtlasMarkerRule> rules = markerRules;
        return new AtlasRules(regions, layoutWeights, factionWeights, rule(rules, VOID_MARKER), rule(rules, POWERFUL_BOSS_MARKER),
                rule(rules, EXPANSION_MARKER), rule(rules, PHENOMENON_MARKER), specials, expansions, List.of());
    }

    public static AtlasUnlocks unlocksFor(SectorAPI sector, AtlasRules rules) {
        Set<String> keys = new HashSet<>();
        rules.regions().forEach(region -> keys.add(region.unlock()));
        rules.specials().forEach(special -> keys.add(special.unlock()));
        keys.remove("");
        Set<String> unlocked = new HashSet<>();
        if (sector != null) {
            for (String key : keys) {
                if (sector.getPlayerMemoryWithoutUpdate().getBoolean(key)) {
                    unlocked.add(key);
                }
            }
        }
        return new AtlasUnlocks(CompatTarget::isModEnabled, unlocked);
    }

    private static AtlasMarkerRule rule(Map<String, AtlasMarkerRule> rules, String id) {
        return rules.getOrDefault(id, AtlasMarkerRule.NEVER);
    }

    static Set<String> factionIds() {
        Set<String> ids = new HashSet<>();
        ChartFactions.all().forEach(faction -> ids.add(faction.factionId()));
        return ids;
    }

    static List<String> registerLayouts(JSONArray rows) throws JSONException {
        Map<String, Float> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(LAYOUTS_PATH);
        forEachRow(rows, (id, row) -> loaded.put(id, Math.max(0f, csv.number(row, "weight", 1f, id))));
        csv.report(LOG);
        layoutWeights = Map.copyOf(loaded);
        return csv.problems();
    }

    static List<String> registerRegions(JSONArray rows, Set<String> knownFactions) throws JSONException {
        Map<String, AtlasRegion> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(REGIONS_PATH);
        Map<String, Float> layouts = layoutWeights;
        forEachRow(rows, (id, row) -> loaded.put(id, new AtlasRegion(id, csv.number(row, "weight", 1f, id), csv.integer(row, "min_steps", 0, id),
                ModCsv.text(row, "requires_mod"), ModCsv.text(row, "unlock"), weights(csv, row, "layouts", id, layouts.keySet()),
                weights(csv, row, "factions", id, knownFactions), weights(csv, row, "markers", id, null))));
        AtlasRegion normal = loaded.get(AtlasRegion.NORMAL);
        if (normal == null) {
            csv.problem(AtlasRegion.NORMAL, ID_COLUMN, "missing (normal space uses no layouts, factions or leanings)");
        } else if (!normal.requiredMod().isEmpty() || !normal.unlock().isEmpty() || normal.minSteps() > 0) {
            csv.problem(AtlasRegion.NORMAL, "requires_mod/unlock/min_steps", "set (normal space is always open, so these are ignored)");
            loaded.put(AtlasRegion.NORMAL, new AtlasRegion(AtlasRegion.NORMAL, normal.weight(), 0, "", "", normal.layoutWeights(),
                    normal.factionWeights(), normal.markerWeights()));
        }
        csv.report(LOG);
        regions = List.copyOf(loaded.values());
        return csv.problems();
    }

    static List<String> registerMarkers(JSONArray rows) throws JSONException {
        Map<String, AtlasMarkerRule> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(MARKERS_PATH);
        forEachRow(rows, (id, row) -> {
            if (!List.of(VOID_MARKER, POWERFUL_BOSS_MARKER, EXPANSION_MARKER, PHENOMENON_MARKER).contains(id)) {
                csv.skipRow(id, "no atlas marker has this id");
                return;
            }
            float chance = csv.number(row, "chance", 0f, id);
            loaded.put(id, new AtlasMarkerRule(chance, csv.number(row, "chance_per_step", 0f, id), csv.number(row, "max_chance", 1f, id)));
        });
        csv.report(LOG);
        markerRules = Map.copyOf(loaded);
        return csv.problems();
    }

    static List<String> registerSpecials(JSONArray rows) throws JSONException {
        List<AtlasSpecial> loaded = new ArrayList<>();
        CsvRows csv = new CsvRows(SPECIALS_PATH);
        forEachRow(rows, (id, row) -> {
            String placementText = ModCsv.text(row, "placement").toUpperCase(Locale.ROOT);
            AtlasSpecial.Placement placement;
            try {
                placement = placementText.isEmpty() ? AtlasSpecial.Placement.CHUNK : AtlasSpecial.Placement.valueOf(placementText);
            } catch (IllegalArgumentException e) {
                csv.skipRow(id, "placement must be chunk or fixed");
                return;
            }
            float radius = csv.number(row, "radius", 0f, id);
            if (radius > AtlasSpecial.MOST_RADIUS) {
                csv.problem(id, "radius", radius + " (at most " + AtlasSpecial.MOST_RADIUS + ")");
            }
            loaded.add(new AtlasSpecial(id, ModCsv.text(row, "kind"), placement, csv.number(row, "chance", 0f, id), csv.number(row, "min_steps", 1f, id),
                    radius, csv.integer(row, "count", 1, id), csv.number(row, "steps", 1f, id), ModCsv.text(row, "requires_mod"),
                    ModCsv.text(row, "unlock")));
        });
        csv.report(LOG);
        specials = List.copyOf(loaded);
        return csv.problems();
    }

    static Map<String, Float> weights(CsvRows csv, JSONObject row, String column, String rowId, Set<String> knownIds) {
        Map<String, Float> weights = new LinkedHashMap<>();
        for (String entry : CsvRows.entries(ModCsv.text(row, column))) {
            int separator = entry.indexOf(WEIGHT_SEPARATOR);
            String id = separator < 0 ? entry : entry.substring(0, separator).trim();
            float weight = separator < 0 ? 1f : CsvRows.numberOrNaN(entry.substring(separator + 1).trim());
            if (id.isEmpty() || Float.isNaN(weight) || Float.isInfinite(weight) || weight < 0f) {
                csv.problem(rowId, column, entry);
            } else if (knownIds != null && !knownIds.contains(id)) {
                csv.problem(rowId, column, entry + " (unknown id)");
            } else {
                weights.put(id, weight);
            }
        }
        return weights;
    }

    private interface IdRowHandler {
        void accept(String id, JSONObject row) throws JSONException;
    }

    private static void forEachRow(JSONArray rows, IdRowHandler handler) throws JSONException {
        if (rows == null) {
            return;
        }
        ModCsv.forEach(rows, (index, row) -> {
            String id = ModCsv.text(row, ID_COLUMN);
            if (!id.isEmpty()) {
                handler.accept(id, row);
            }
        });
    }

    static void resetForTests() {
        layoutWeights = Map.of();
        regions = List.of();
        markerRules = Map.of();
        specials = List.of();
    }
}
