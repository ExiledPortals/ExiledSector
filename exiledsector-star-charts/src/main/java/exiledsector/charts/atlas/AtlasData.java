package exiledsector.charts.atlas;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledlib.ModCsv;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.content.ChartFaction;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.layout.feature.ChartFeatures;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.layout.ChartLayoutGenerator;
import exiledsector.charts.layout.ChartLayoutRecipe;
import exiledsector.charts.layout.ChartLook;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.content.ModdedChartFactions;
import exiledsector.charts.layout.ChartShape;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.data.CsvRows;
import exiledsector.ui.util.ColorUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class AtlasData {

    static final String REGIONS_PATH = "data/config/exiledSector/atlas_regions.csv";
    static final String LAYOUTS_PATH = "data/config/exiledSector/atlas_layouts.csv";
    static final String MARKERS_PATH = "data/config/exiledSector/atlas_markers.csv";
    static final String SPECIALS_PATH = "data/config/exiledSector/atlas_specials.csv";
    static final String NODE_EFFECTS_PATH = "data/config/exiledSector/atlas_node_effects.csv";
    static final String DECORATIONS_PATH = "data/config/exiledSector/atlas_decorations.csv";
    static final String ID_COLUMN = "id";
    static final String VOID_MARKER = "void";
    static final String POWERFUL_BOSS_MARKER = "powerful_boss";
    static final String ANOMALY_MARKER = "anomaly";
    static final String MODDED_FACTIONS = "modded_factions";
    private static final Logger LOG = Logger.getLogger(AtlasData.class);
    private static final Pattern HEX_COLOUR = Pattern.compile("#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})");
    private static volatile Map<String, AtlasLayout> layouts = Map.of();
    private static volatile Map<String, Float> layoutWeights = Map.of();
    private static volatile Map<String, ChartLook> looks = Map.of();
    private static volatile Map<String, AtlasAmbience> ambiences = Map.of();
    private static volatile Map<String, AtlasDecoration> decorations = Map.of();
    private static volatile List<AtlasRegion> regions = List.of();
    private static volatile Map<String, AtlasMarkerRule> markerRules = Map.of();
    private static volatile List<AtlasSpecial> specials = List.of();
    private static volatile Map<String, List<ChartGenContext.AppliedEffect>> nodeEffects = Map.of();

    private AtlasData() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, LAYOUTS_PATH, LOG, AtlasData::registerLayouts);
        ModCsv.load(ID_COLUMN, DECORATIONS_PATH, LOG, AtlasData::registerDecorations);
        ModCsv.load(ID_COLUMN, REGIONS_PATH, LOG, rows -> registerRegions(rows, factionIds(), ModdedChartFactions.detect(factionIds())));
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
        List<AtlasAnomalyCandidate> anomalies = new ArrayList<>();
        for (ChartAnomalies.Definition definition : ChartAnomalies.definitions()) {
            ChartAnomaly anomaly = ChartAnomalies.byId(definition.id());
            if (anomaly != null) {
                anomalies.add(new AtlasAnomalyCandidate(definition.id(), definition.weight(), definition.requiredMod(), definition.replacesStar(),
                        definition.excludes(), (traits, weight) -> anomaly.fitsAt(traits) ? anomaly.atlasWeight(traits, weight) : 0f));
            }
        }
        Map<String, AtlasMarkerRule> rules = markerRules;
        return new AtlasRules(regions, layoutWeights, factionWeights, rule(rules, VOID_MARKER), rule(rules, POWERFUL_BOSS_MARKER),
                rule(rules, ANOMALY_MARKER), specials, anomalies);
    }

    public static Color regionTint(String regionId) {
        ChartLook look = regionId == null ? null : looks.get(regionId);
        return look == null ? null : look.lightTint();
    }

    public static AtlasAmbience ambience(String regionId) {
        AtlasAmbience ambience = regionId == null ? null : ambiences.get(regionId);
        return ambience == null ? AtlasAmbience.BARE : ambience;
    }

    public static AtlasDecoration decoration(String decorationId) {
        return decorations.get(decorationId);
    }

    public static boolean knowsRegion(String regionId) {
        return looks.containsKey(regionId);
    }

    public static boolean knowsLayout(String layoutId) {
        return layouts.containsKey(layoutId);
    }

    public static ChartLayoutRecipe recipeFor(String regionId, String layoutId) {
        ChartLook look = regionId == null ? ChartLook.PLAIN : looks.getOrDefault(regionId, ChartLook.PLAIN);
        AtlasLayout layout = layoutId == null ? null : layouts.get(layoutId);
        return new ChartLayoutRecipe(look, layout == null ? ChartShape.ANY : layout.shape());
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
        return new AtlasUnlocks(ChartPois::modRequirementMet, unlocked);
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
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
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
            loaded.put(id, new AtlasLayout(id, csv.number(row, "weight", 1f, id), new ChartShape(systemTypes(csv, row, id), minOrbits, maxOrbits,
                    csv.number(row, "max_orbit_radius", Float.NaN, id), categoryWeights(csv, row, "terrain", id), feature)));
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
        csv.weights(row, "system_types", rowId, null).forEach((name, weight) -> {
            StarSystemType type = ChartLayoutGenerator.parseRecipeType(name);
            if (type == null) {
                csv.problem(rowId, "system_types", name + " (not a chart system type)");
            } else {
                types.put(type, weight);
            }
        });
        return types;
    }

    private static Map<String, Float> categoryWeights(CsvRows csv, JSONObject row, String column, String rowId) {
        Map<String, Float> categories = csv.weights(row, column, rowId, null);
        for (String category : List.copyOf(categories.keySet())) {
            if (ChartLayoutRecipe.ANOMALY_CATEGORIES.contains(category)) {
                csv.problem(rowId, column, category + " (only anomalies add it)");
                categories.remove(category);
            }
        }
        return categories;
    }

    private static ChartLook look(CsvRows csv, JSONObject row, String rowId) {
        Map<String, Float> stars = csv.weights(row, "stars", rowId, null);
        for (String star : List.copyOf(stars.keySet())) {
            if (ChartLayoutRecipe.ANOMALY_STARS.contains(star)) {
                csv.problem(rowId, "stars", star + " (only anomalies add it)");
                stars.remove(star);
            }
        }
        float nebulaChance = csv.number(row, "nebula_chance", Float.NaN, rowId);
        if (nebulaChance < 0f || nebulaChance > 1f) {
            csv.problem(rowId, "nebula_chance", nebulaChance + " (needs 0 to 1)");
            nebulaChance = Math.max(0f, Math.min(1f, nebulaChance));
        }
        return new ChartLook(CsvRows.entries(ModCsv.text(row, "backgrounds")), CsvRows.entries(ModCsv.text(row, "nebula_backgrounds")),
                nebulaChance, colour(csv, row, "light_tint", rowId), csv.number(row, "light_tint_strength", 0f, rowId), stars,
                categoryWeights(csv, row, "planets", rowId), csv.flag(row, "keep_ruins", false, rowId), ModCsv.text(row, "music"));
    }

    private static Color colour(CsvRows csv, JSONObject row, String column, String rowId) {
        String text = ModCsv.text(row, column);
        Color colour = HEX_COLOUR.matcher(text).matches() ? ColorUtil.parseHexColorOrNull(text) : null;
        if (colour == null && !text.isEmpty()) {
            csv.problem(rowId, column, text);
        }
        return colour;
    }

    static List<String> registerRegions(JSONArray rows, Set<String> knownFactions) throws JSONException {
        return registerRegions(rows, knownFactions, List.of());
    }

    static List<String> registerRegions(JSONArray rows, Set<String> knownFactions, List<String> moddedFactions) throws JSONException {
        Map<String, AtlasRegion> loaded = new LinkedHashMap<>();
        Map<String, ChartLook> loadedLooks = new LinkedHashMap<>();
        Map<String, AtlasAmbience> loadedAmbiences = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(REGIONS_PATH);
        Set<String> layoutIds = layoutWeights.keySet();
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
            float size = csv.number(row, "size", 1f, id);
            if (size < AtlasRegion.MIN_SIZE || size > AtlasRegion.MAX_SIZE) {
                csv.problem(id, "size", size + " (needs " + AtlasRegion.MIN_SIZE + " to " + AtlasRegion.MAX_SIZE + ")");
            }
            float density = csv.number(row, "density", 1f, id);
            if (density < 0f || density > 1f) {
                csv.problem(id, "density", density + " (needs 0 to 1)");
            }
            loaded.put(id, new AtlasRegion(id, csv.number(row, "weight", 1f, id), csv.integer(row, "min_steps", 0, id),
                    ModCsv.text(row, "requires_mod"), ModCsv.text(row, "unlock"), csv.weights(row, "layouts", id, layoutIds),
                    factionWeights(csv, row, id, knownFactions, moddedFactions),
                    csv.weights(row, "markers", id, null),
                    csv.integer(row, "max_steps", AtlasRegion.NO_MAX_STEPS, id), csv.number(row, "weight_per_step", 0f, id), size, density,
                    csv.weights(row, "neighbours", id, null), fraction(csv, row, "clouds", id), fraction(csv, row, "walls", id)));
            loadedLooks.put(id, look(csv, row, id));
            loadedAmbiences.put(id, ambience(csv, row, id));
        });
        for (AtlasRegion region : loaded.values()) {
            for (String neighbour : region.neighbourWeights().keySet()) {
                if (!loaded.containsKey(neighbour)) {
                    csv.problem(region.id(), "neighbours", neighbour + " (no region has this id)");
                }
            }
        }
        AtlasRegion normal = loaded.get(AtlasRegion.NORMAL);
        if (normal == null) {
            csv.problem(AtlasRegion.NORMAL, ID_COLUMN, "missing (normal space uses no layouts, factions or leanings)");
        } else if (!normal.requiredMod().isEmpty() || !normal.unlock().isEmpty() || normal.minSteps() > 0) {
            csv.problem(AtlasRegion.NORMAL, "requires_mod/unlock/min_steps", "set (normal space is always open, so these are ignored)");
            loaded.put(AtlasRegion.NORMAL, new AtlasRegion(AtlasRegion.NORMAL, normal.weight(), 0, "", "", normal.layoutWeights(),
                    normal.factionWeights(), normal.markerWeights(), normal.maxSteps(), normal.weightPerStep(), normal.size(), normal.density(),
                    normal.neighbourWeights(), normal.clouds(), normal.walls()));
        }
        csv.report(LOG);
        regions = List.copyOf(loaded.values());
        looks = Map.copyOf(loadedLooks);
        ambiences = Map.copyOf(loadedAmbiences);
        return csv.problems();
    }

    private static Map<String, Float> factionWeights(CsvRows csv, JSONObject row, String rowId, Set<String> knownFactions,
                                                     List<String> moddedFactions) {
        Set<String> known = null;
        if (ModCsv.text(row, "requires_mod").isEmpty()) {
            known = new HashSet<>(knownFactions);
            known.add(MODDED_FACTIONS);
        }
        Map<String, Float> weights = csv.weights(row, "factions", rowId, known);
        Float moddedWeight = weights.remove(MODDED_FACTIONS);
        if (moddedWeight != null) {
            moddedFactions.forEach(factionId -> weights.putIfAbsent(factionId, moddedWeight));
        }
        return weights;
    }

    private static AtlasAmbience ambience(CsvRows csv, JSONObject row, String rowId) {
        float storms = fraction(csv, row, "storms", rowId);
        Map<String, Float> picks = csv.weights(row, "decorations", rowId, decorations.keySet());
        return new AtlasAmbience(storms, colour(csv, row, "cloud_colour", rowId), picks,
                csv.number(row, "decoration_density", picks.isEmpty() ? 0f : 1f, rowId), csv.flag(row, "abyss", false, rowId));
    }

    private static float fraction(CsvRows csv, JSONObject row, String column, String rowId) {
        float value = csv.number(row, column, 0f, rowId);
        if (value < 0f || value > 1f) {
            csv.problem(rowId, column, value + " (needs 0 to 1)");
        }
        return value;
    }

    static List<String> registerDecorations(JSONArray rows) throws JSONException {
        Map<String, AtlasDecoration> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(DECORATIONS_PATH);
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
            List<String> sprites = CsvRows.entries(ModCsv.text(row, "sprite"));
            if (sprites.isEmpty()) {
                csv.skipRow(id, "needs at least one sprite");
                return;
            }
            loaded.put(id, new AtlasDecoration(id, sprites, ModCsv.text(row, "glow"), csv.integer(row, "columns", 1, id), csv.integer(row, "rows", 1, id),
                    csv.number(row, "size", 0.05f, id), fraction(csv, row, "size_jitter", id), csv.integer(row, "count", 1, id),
                    csv.number(row, "spread", 0f, id), csv.number(row, "spin", 0f, id), csv.number(row, "drift", 0f, id), colour(csv, row, "colour", id),
                    colour(csv, row, "glow_colour", id), csv.number(row, "alpha", 1f, id), csv.number(row, "pulse", 0f, id),
                    csv.number(row, "ping", 0f, id), CsvRows.entries(ModCsv.text(row, "scatter"))));
        });
        loaded.replaceAll((id, decoration) -> knownScatter(csv, decoration, loaded.keySet()));
        csv.report(LOG);
        decorations = Map.copyOf(loaded);
        return csv.problems();
    }

    private static AtlasDecoration knownScatter(CsvRows csv, AtlasDecoration decoration, Set<String> knownIds) {
        List<String> known = new ArrayList<>();
        for (String scatterId : decoration.scatter()) {
            if (knownIds.contains(scatterId) && !scatterId.equals(decoration.id())) {
                known.add(scatterId);
            } else {
                csv.problem(decoration.id(), "scatter", scatterId + " (needs another decoration id)");
            }
        }
        return known.size() == decoration.scatter().size() ? decoration
                : new AtlasDecoration(decoration.id(), decoration.sprites(), decoration.glow(), decoration.columns(), decoration.rows(), decoration.size(),
                decoration.sizeJitter(), decoration.count(), decoration.spread(), decoration.spin(), decoration.drift(), decoration.colour(),
                decoration.glowColour(), decoration.alpha(), decoration.pulse(), decoration.ping(), known);
    }

    static List<String> registerMarkers(JSONArray rows) throws JSONException {
        Map<String, AtlasMarkerRule> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(MARKERS_PATH);
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
            if (!List.of(VOID_MARKER, POWERFUL_BOSS_MARKER, ANOMALY_MARKER).contains(id)) {
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
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
            String placementText = ModCsv.text(row, "placement").toUpperCase(Locale.ROOT);
            AtlasSpecial.Placement placement;
            try {
                placement = placementText.isEmpty() ? AtlasSpecial.Placement.CHUNK : AtlasSpecial.Placement.valueOf(placementText);
            } catch (IllegalArgumentException e) {
                csv.skipRow(id, "placement must be chunk or fixed");
                return;
            }
            AtlasSpecial.Kind kind = AtlasSpecial.Kind.parse(ModCsv.text(row, "kind"));
            if (kind == null) {
                csv.skipRow(id, "kind must be infestation, sensor_array, bastion or unique");
                return;
            }
            float radius = csv.number(row, "radius", 0f, id);
            if (radius > AtlasSpecial.MOST_RADIUS) {
                csv.problem(id, "radius", radius + " (at most " + AtlasSpecial.MOST_RADIUS + ")");
            }
            loaded.add(new AtlasSpecial(id, kind, placement, csv.number(row, "chance", 0f, id), csv.number(row, "min_steps", 1f, id),
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
        CsvRows.forEachIdRow(rows, ID_COLUMN, (id, row) -> {
            List<ChartGenContext.AppliedEffect> effects = new ArrayList<>();
            csv.weights(row, "effects", id, null, true).forEach((name, value) -> {
                ChartEffect effect = ChartEffect.byNameOrNull(name);
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

    static void resetForTests() {
        nodeEffects = Map.of();
        layouts = Map.of();
        layoutWeights = Map.of();
        looks = Map.of();
        ambiences = Map.of();
        decorations = Map.of();
        regions = List.of();
        markerRules = Map.of();
        specials = List.of();
    }
}
