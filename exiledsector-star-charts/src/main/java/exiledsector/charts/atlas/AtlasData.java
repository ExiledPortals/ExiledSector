package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledlib.ModCsv;
import exiledlib.compat.CompatTarget;
import exiledsector.charts.ChartExpansions;
import exiledsector.charts.ChartFaction;
import exiledsector.charts.ChartFactions;
import exiledsector.charts.ChartEffect;
import exiledsector.charts.ChartFeatures;
import exiledsector.charts.ChartGenContext;
import exiledsector.charts.ChartLayoutGenerator;
import exiledsector.charts.ChartLayoutRecipe;
import exiledsector.charts.ChartPhenomena;
import exiledsector.charts.expansion.ChartExpansion;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
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
    static final String NODE_EFFECTS_PATH = "data/config/exiledSector/atlas_node_effects.csv";
    static final String ID_COLUMN = "id";
    static final String VOID_MARKER = "void";
    static final String POWERFUL_BOSS_MARKER = "powerful_boss";
    static final String EXPANSION_MARKER = "expansion";
    static final String PHENOMENON_MARKER = "phenomenon";
    static final String WEIGHT_SEPARATOR = ":";
    private static final Logger LOG = Logger.getLogger(AtlasData.class);
    private static volatile Map<String, AtlasLayout> layouts = Map.of();
    private static volatile Map<String, Float> layoutWeights = Map.of();
    private static volatile Map<String, AtlasRegionLook> looks = Map.of();
    private static volatile List<AtlasRegion> regions = List.of();
    private static volatile Map<String, AtlasMarkerRule> markerRules = Map.of();
    private static volatile List<AtlasSpecial> specials = List.of();
    private static volatile Map<String, List<ChartGenContext.AppliedEffect>> nodeEffects = Map.of();

    private AtlasData() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, LAYOUTS_PATH, LOG, AtlasData::registerLayouts);
        ModCsv.load(ID_COLUMN, REGIONS_PATH, LOG, rows -> registerRegions(rows, factionIds()));
        ModCsv.load(ID_COLUMN, MARKERS_PATH, LOG, AtlasData::registerMarkers);
        ModCsv.load(ID_COLUMN, SPECIALS_PATH, LOG, AtlasData::registerSpecials);
        ModCsv.load(ID_COLUMN, NODE_EFFECTS_PATH, LOG, AtlasData::registerNodeEffects);
        AtlasRuntime.invalidate();
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
        List<AtlasMarkerCandidate> phenomena = new ArrayList<>();
        for (ChartPhenomena.Definition definition : ChartPhenomena.definitions()) {
            phenomena.add(new AtlasMarkerCandidate(definition.id(), definition.weight(), definition.requiredMod()));
        }
        Map<String, AtlasMarkerRule> rules = markerRules;
        return new AtlasRules(regions, layoutWeights, factionWeights, rule(rules, VOID_MARKER), rule(rules, POWERFUL_BOSS_MARKER),
                rule(rules, EXPANSION_MARKER), rule(rules, PHENOMENON_MARKER), specials, expansions, phenomena);
    }

    public static Color regionTint(String regionId) {
        AtlasRegionLook look = regionId == null ? null : looks.get(regionId);
        return look == null ? null : look.lightTint();
    }

    public static boolean knowsRegion(String regionId) {
        return looks.containsKey(regionId);
    }

    public static boolean knowsLayout(String layoutId) {
        return layouts.containsKey(layoutId);
    }

    public static ChartLayoutRecipe recipeFor(String regionId, String layoutId) {
        AtlasRegionLook look = regionId == null ? AtlasRegionLook.PLAIN : looks.getOrDefault(regionId, AtlasRegionLook.PLAIN);
        return recipe(look, layoutId == null ? null : layouts.get(layoutId));
    }

    static ChartLayoutRecipe recipe(AtlasRegionLook look, AtlasLayout layout) {
        Map<String, Float> categories = new HashMap<>(look.planetWeights());
        if (layout == null) {
            return new ChartLayoutRecipe(look.starWeights(), categories, look.backgrounds(), look.nebulaBackgrounds(), look.nebulaChance(),
                    look.lightTint(), look.lightTintStrength(), Map.of(), -1, -1, Float.NaN, "", look.keepRuins(), look.musicSetId());
        }
        layout.terrainWeights().forEach((category, weight) -> categories.merge(category, weight, (first, second) -> first * second));
        return new ChartLayoutRecipe(look.starWeights(), categories, look.backgrounds(), look.nebulaBackgrounds(), look.nebulaChance(),
                look.lightTint(), look.lightTintStrength(), layout.systemTypeWeights(), layout.minOrbits(), layout.maxOrbits(),
                layout.maxOrbitRadius(), layout.featureId(), look.keepRuins(), look.musicSetId());
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
        Map<String, AtlasLayout> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(LAYOUTS_PATH);
        forEachRow(rows, (id, row) -> {
            int minOrbits = csv.integer(row, "min_orbits", -1, id);
            int maxOrbits = csv.integer(row, "max_orbits", minOrbits, id);
            if (minOrbits < 0 && maxOrbits >= 0) {
                minOrbits = maxOrbits;
            }
            if (minOrbits >= 0 && maxOrbits < minOrbits) {
                csv.problem(id, "max_orbits", maxOrbits + " (below min_orbits)");
            }
            String feature = ModCsv.text(row, "feature");
            if (!feature.isEmpty() && ChartFeatures.byId(feature) == null) {
                csv.problem(id, "feature", feature + " (no feature class, or no row in atlas_features.csv)");
                feature = "";
            }
            loaded.put(id, new AtlasLayout(id, csv.number(row, "weight", 1f, id), systemTypes(csv, row, id), minOrbits, maxOrbits,
                    csv.number(row, "max_orbit_radius", Float.NaN, id), categoryWeights(csv, row, "terrain", id), feature));
        });
        csv.report(LOG);
        Map<String, Float> weights = new LinkedHashMap<>();
        loaded.forEach((id, layout) -> weights.put(id, layout.weight()));
        layouts = Map.copyOf(loaded);
        layoutWeights = Map.copyOf(weights);
        return csv.problems();
    }

    private static Map<StarSystemType, Float> systemTypes(CsvRows csv, JSONObject row, String rowId) {
        Map<StarSystemType, Float> types = new EnumMap<>(StarSystemType.class);
        weights(csv, row, "system_types", rowId, null).forEach((name, weight) -> {
            StarSystemType type = ChartLayoutGenerator.parseType(name);
            if (type == null) {
                csv.problem(rowId, "system_types", name + " (not a chart system type)");
            } else {
                types.put(type, weight);
            }
        });
        return types;
    }

    private static Map<String, Float> categoryWeights(CsvRows csv, JSONObject row, String column, String rowId) {
        Map<String, Float> categories = weights(csv, row, column, rowId, null);
        for (String category : List.copyOf(categories.keySet())) {
            if (ChartLayoutRecipe.PHENOMENON_CATEGORIES.contains(category)) {
                csv.problem(rowId, column, category + " (only phenomena add it)");
                categories.remove(category);
            }
        }
        return categories;
    }

    private static AtlasRegionLook look(CsvRows csv, JSONObject row, String rowId) {
        Map<String, Float> stars = weights(csv, row, "stars", rowId, null);
        for (String star : List.copyOf(stars.keySet())) {
            if (ChartLayoutRecipe.PHENOMENON_STARS.contains(star)) {
                csv.problem(rowId, "stars", star + " (only phenomena add it)");
                stars.remove(star);
            }
        }
        float nebulaChance = csv.number(row, "nebula_chance", Float.NaN, rowId);
        if (nebulaChance < 0f || nebulaChance > 1f) {
            csv.problem(rowId, "nebula_chance", nebulaChance + " (needs 0 to 1)");
            nebulaChance = Math.max(0f, Math.min(1f, nebulaChance));
        }
        return new AtlasRegionLook(CsvRows.entries(ModCsv.text(row, "backgrounds")), CsvRows.entries(ModCsv.text(row, "nebula_backgrounds")),
                nebulaChance, colour(csv, row, "light_tint", rowId), csv.number(row, "light_tint_strength", 0f, rowId), stars,
                categoryWeights(csv, row, "planets", rowId), csv.flag(row, "keep_ruins", false, rowId), ModCsv.text(row, "music"));
    }

    private static Color colour(CsvRows csv, JSONObject row, String column, String rowId) {
        String text = ModCsv.text(row, column);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Color.decode(text.startsWith("#") ? text : "#" + text);
        } catch (NumberFormatException e) {
            csv.problem(rowId, column, text);
            return null;
        }
    }

    static List<String> registerRegions(JSONArray rows, Set<String> knownFactions) throws JSONException {
        Map<String, AtlasRegion> loaded = new LinkedHashMap<>();
        Map<String, AtlasRegionLook> loadedLooks = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(REGIONS_PATH);
        Set<String> layoutIds = layoutWeights.keySet();
        forEachRow(rows, (id, row) -> {
            loaded.put(id, new AtlasRegion(id, csv.number(row, "weight", 1f, id), csv.integer(row, "min_steps", 0, id),
                    ModCsv.text(row, "requires_mod"), ModCsv.text(row, "unlock"), weights(csv, row, "layouts", id, layoutIds),
                    weights(csv, row, "factions", id, knownFactions), weights(csv, row, "markers", id, null)));
            loadedLooks.put(id, look(csv, row, id));
        });
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
        looks = Map.copyOf(loadedLooks);
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
                    ModCsv.text(row, "unlock"), ModCsv.text(row, "faction")));
        });
        csv.report(LOG);
        specials = List.copyOf(loaded);
        return csv.problems();
    }

    public static List<ChartGenContext.AppliedEffect> nodeEffects(String id) {
        return nodeEffects.getOrDefault(id, List.of());
    }

    static List<String> registerNodeEffects(JSONArray rows) throws JSONException {
        Map<String, List<ChartGenContext.AppliedEffect>> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(NODE_EFFECTS_PATH);
        forEachRow(rows, (id, row) -> {
            List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
            weights(csv, row, "effects", id, null, true).forEach((name, value) -> {
                ChartEffect effect = chartEffect(name);
                if (effect == null) {
                    csv.problem(id, "effects", name + " (not a chart effect)");
                } else {
                    effects.add(new ChartGenContext.AppliedEffect(effect, value));
                }
            });
            loaded.put(id, List.copyOf(effects));
        });
        csv.report(LOG);
        nodeEffects = Map.copyOf(loaded);
        return csv.problems();
    }

    private static ChartEffect chartEffect(String name) {
        try {
            return ChartEffect.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Map<String, Float> weights(CsvRows csv, JSONObject row, String column, String rowId, Set<String> knownIds) {
        return weights(csv, row, column, rowId, knownIds, false);
    }

    static Map<String, Float> weights(CsvRows csv, JSONObject row, String column, String rowId, Set<String> knownIds, boolean negativeAllowed) {
        Map<String, Float> weights = new LinkedHashMap<>();
        for (String entry : CsvRows.entries(ModCsv.text(row, column))) {
            int separator = entry.indexOf(WEIGHT_SEPARATOR);
            String id = separator < 0 ? entry : entry.substring(0, separator).trim();
            float weight = separator < 0 ? 1f : CsvRows.numberOrNaN(entry.substring(separator + 1).trim());
            if (id.isEmpty() || Float.isNaN(weight) || Float.isInfinite(weight) || weight < 0f && !negativeAllowed) {
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
        nodeEffects = Map.of();
        layouts = Map.of();
        layoutWeights = Map.of();
        looks = Map.of();
        regions = List.of();
        markerRules = Map.of();
        specials = List.of();
    }
}
