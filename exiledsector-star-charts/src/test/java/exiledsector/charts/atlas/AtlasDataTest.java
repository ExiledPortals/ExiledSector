package exiledsector.charts.atlas;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.ChartLayoutRecipe;
import exiledsector.charts.ChartTables;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasDataTest {

    @AfterEach
    void tearDown() {
        AtlasData.resetForTests();
    }

    @Test
    void theShippedAtlasTablesParseWithoutProblems() throws Exception {
        ChartTables.loadShipped();
        ChartTables.loadFeatures();

        assertEquals(List.of(), AtlasData.registerLayouts(ChartTables.csvRows(AtlasData.LAYOUTS_PATH)));
        assertEquals(List.of(), AtlasData.registerDecorations(ChartTables.csvRows(AtlasData.DECORATIONS_PATH)));
        assertEquals(List.of(), AtlasData.registerRegions(ChartTables.csvRows(AtlasData.REGIONS_PATH),
                AtlasData.factionIds()));
        assertEquals(List.of(), AtlasData.registerMarkers(ChartTables.csvRows(AtlasData.MARKERS_PATH)));
        assertEquals(List.of(), AtlasData.registerSpecials(ChartTables.csvRows(AtlasData.SPECIALS_PATH)));
        AtlasRules rules = AtlasData.rules();

        assertFalse(rules.regions().isEmpty());
        assertTrue(rules.powerfulBossRule().chanceAt(0) > 0f);
        for (AtlasRegion region : rules.regions()) {
            for (String decorationId : AtlasData.ambience(region.id()).decorations().keySet()) {
                AtlasDecoration decoration = AtlasData.decoration(decorationId);
                assertTrue(decoration.sprites().stream().allMatch(path -> path.startsWith("graphics/")), decorationId);
            }
        }
        assertTrue(AtlasData.rules().region("storm_belt").walls() > 0.5f);
        assertEquals(Set.of("derelict"), AtlasData.rules().region("derelict_graveyard").factionWeights().keySet());
        assertEquals(Set.of("remnant"), AtlasData.rules().region("remnant_expanse").factionWeights().keySet());
        Set<String> normalFactions = AtlasData.rules().normalRegion().factionWeights().keySet();
        assertTrue(normalFactions.contains("hegemony"), normalFactions.toString());
        assertFalse(normalFactions.contains("derelict") || normalFactions.contains("remnant") || normalFactions.contains("rat_abyssals"));
        assertTrue(AtlasData.ambience(AtlasRegion.NORMAL).decorations().isEmpty());
        assertEquals(3, rules.specials().stream().filter(special -> special.placement() == AtlasSpecial.Placement.FIXED)
                .mapToInt(AtlasSpecial::count).sum());
    }

    @Test
    void weightListsReadIdsWithOptionalWeightsAndRejectUnknownOrBrokenEntries() throws Exception {
        AtlasData.registerLayouts(new JSONArray().put(new JSONObject().put("id", "compact")).put(new JSONObject().put("id", "sprawling")));
        JSONArray rows = new JSONArray().put(new JSONObject().put("id", AtlasRegion.NORMAL)).put(new JSONObject().put("id", "reach").put("layouts", "compact:2; sprawling; missing:1")
                .put("factions", "pirates:x; remnant:3").put("markers", "black_hole:4"));

        List<String> problems = AtlasData.registerRegions(rows, Set.of("pirates", "remnant"));
        AtlasRegion reach = AtlasData.rules().region("reach");

        assertEquals(Map.of("compact", 2f, "sprawling", 1f), reach.layoutWeights());
        assertEquals(Map.of("remnant", 3f), reach.factionWeights());
        assertEquals(4f, reach.markerWeight("black_hole"));
        assertEquals(1f, reach.markerWeight("flares"));
        assertEquals(2, problems.size());
    }

    @Test
    void aRegionLookAndALayoutCombineIntoOneRecipe() throws Exception {
        ChartTables.loadFeatures();
        List<String> layoutProblems = AtlasData.registerLayouts(new JSONArray()
                .put(new JSONObject().put("id", "ringed").put("system_types", "SINGLE:3; BINARY_FAR:1; NEBULA:2").put("min_orbits", "4")
                        .put("max_orbits", "6").put("max_orbit_radius", "12000").put("terrain", "cat_terrain_rings:2; cat_magfield:5")
                        .put("feature", "ringed_giant")));
        List<String> regionProblems = AtlasData.registerRegions(new JSONArray()
                .put(new JSONObject().put("id", AtlasRegion.NORMAL))
                .put(new JSONObject().put("id", "frozen").put("backgrounds", "a.jpg; b.jpg").put("nebula_chance", "0.3")
                        .put("light_tint", "#A0C8FF").put("light_tint_strength", "0.25").put("stars", "star_red_dwarf:2; black_hole:4")
                        .put("planets", "cat_frozen:4; cat_terrain_rings:3").put("keep_ruins", "yes")), Set.of());

        ChartLayoutRecipe recipe = AtlasData.recipeFor("frozen", "ringed");
        ChartLayoutRecipe plain = AtlasData.recipeFor(AtlasRegion.NORMAL, "missing");

        assertEquals(2, layoutProblems.size());
        assertEquals(1, regionProblems.size());
        assertEquals(Map.of(StarSystemType.SINGLE, 3f, StarSystemType.BINARY_FAR, 1f), recipe.shape().systemTypeWeights());
        assertEquals(4, recipe.shape().minOrbits());
        assertEquals(6, recipe.shape().maxOrbits());
        assertEquals(12000f, recipe.shape().orbitRadiusLimit());
        assertEquals("ringed_giant", recipe.shape().featureId());
        assertEquals(List.of("a.jpg", "b.jpg"), recipe.look().backgrounds());
        assertEquals(0.3f, recipe.look().nebulaChance());
        assertEquals(new Color(0xA0, 0xC8, 0xFF), recipe.look().lightTint());
        assertEquals(Map.of("star_red_dwarf", 2f), recipe.look().starWeights());
        assertEquals(4f, recipe.categoryWeight("cat_frozen", 1f));
        assertEquals(6f, recipe.categoryWeight("cat_terrain_rings", 1f));
        assertTrue(recipe.look().keepRuins());
        assertFalse(plain.shape().setsOrbits());
        assertTrue(plain.shape().systemTypeWeights().isEmpty());
    }

    @Test
    void normalSpaceMustBeListedAndIsAlwaysOpen() throws Exception {
        List<String> missing = AtlasData.registerRegions(new JSONArray().put(new JSONObject().put("id", "furnace")), Set.of());

        assertEquals(1, missing.size());
        assertEquals(AtlasRegion.DEFAULT_NORMAL, AtlasData.rules().normalRegion());

        List<String> gated = AtlasData.registerRegions(new JSONArray()
                .put(new JSONObject().put("id", AtlasRegion.NORMAL).put("unlock", "$flag").put("weight", "12"))
                .put(new JSONObject().put("id", "furnace")), Set.of());
        AtlasRules rules = AtlasData.rules();

        assertEquals(1, gated.size());
        assertEquals("", rules.normalRegion().unlock());
        assertEquals(12f, rules.normalRegion().weight());
    }

    @Test
    void unknownMarkersAndBadSpecialsAreReported() throws Exception {
        List<String> markerProblems = AtlasData.registerMarkers(new JSONArray().put(new JSONObject().put("id", "storm").put("chance", "0.1")));
        List<String> specialProblems = AtlasData.registerSpecials(new JSONArray()
                .put(new JSONObject().put("id", "bastion").put("placement", "fixed").put("count", "4").put("steps", "25"))
                .put(new JSONObject().put("id", "nexus").put("kind", "infestation").put("radius", "12"))
                .put(new JSONObject().put("id", "odd").put("placement", "orbit")));
        AtlasRules rules = AtlasData.rules();

        assertEquals(1, markerProblems.size());
        assertEquals(2, specialProblems.size());
        assertEquals(2, rules.specials().size());
        assertEquals(4, rules.specials().get(0).count());
        assertEquals(AtlasSpecial.MOST_RADIUS, rules.specials().get(1).radius());
    }
}
