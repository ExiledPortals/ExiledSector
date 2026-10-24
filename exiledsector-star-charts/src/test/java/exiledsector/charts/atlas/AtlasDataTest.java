package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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

        assertEquals(List.of(), AtlasData.registerLayouts(ChartTables.csvRows(AtlasData.LAYOUTS_PATH)));
        assertEquals(List.of(), AtlasData.registerRegions(ChartTables.csvRows(AtlasData.REGIONS_PATH),
                AtlasData.factionIds()));
        assertEquals(List.of(), AtlasData.registerMarkers(ChartTables.csvRows(AtlasData.MARKERS_PATH)));
        assertEquals(List.of(), AtlasData.registerSpecials(ChartTables.csvRows(AtlasData.SPECIALS_PATH)));
        AtlasRules rules = AtlasData.rules();

        assertFalse(rules.regions().isEmpty());
        assertTrue(rules.powerfulBossRule().chanceAt(0) > 0f);
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
