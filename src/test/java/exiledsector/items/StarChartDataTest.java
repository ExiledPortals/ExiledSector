package exiledsector.items;

import exiledsector.charts.ChartEffects;
import exiledsector.items.ItemDataIntegrity.Severity;
import exiledsector.items.ItemDataIntegrity.Violation;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.TierRange;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarChartDataTest {

    @BeforeEach
    void setUp() {
        ChartEffects.register();
    }

    @AfterEach
    void tearDown() {
        StarChartFixtures.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static JSONObject chartModifier(String id, String slot, String lines, String name) throws JSONException {
        return ItemFixtures.modifierRow(id, slot, ItemType.STAR_CHART.id(), lines, name);
    }

    private static JSONObject chart(String id, String systemType, String prefixes, String suffixes) throws JSONException {
        return new JSONObject().put("id", id).put("system_type", systemType).put("name", "Chart").put("rarity", "5")
                .put("prefixes", prefixes).put("suffixes", suffixes);
    }

    private static List<String> chartErrors(JSONArray itemModifiers, JSONArray chartModifiers, JSONArray charts, JSONArray chartImplicits)
            throws JSONException {
        return ItemDataIntegrity.checkWithStarCharts(itemModifiers, new JSONArray(), new JSONArray(), new JSONArray(), chartModifiers, charts,
                chartImplicits).stream().filter(violation -> violation.severity() == Severity.ERROR).map(Violation::message).toList();
    }

    @Test
    void theShippedChartDataHasNoIntegrityErrors() throws Exception {
        List<Violation> violations = ItemDataIntegrity.checkWithStarCharts(ItemFixtures.shippedRows(ItemModifiers.DATA_PATH),
                ItemFixtures.shippedRows(ItemDefinitions.SUBROUTINES_PATH), ItemFixtures.shippedRows(ItemDefinitions.HULL_ITEMS_PATH),
                ItemFixtures.shippedRows(ItemImplicits.DATA_PATH), ItemFixtures.shippedRows(StarChartItems.MODIFIERS_PATH),
                ItemFixtures.shippedRows(StarChartDefinitions.DATA_PATH), ItemFixtures.shippedRows(StarChartImplicits.DATA_PATH));

        assertEquals(List.of(), violations.stream().filter(violation -> violation.severity() == Severity.ERROR).toList());
        assertTrue(violations.stream().noneMatch(violation -> violation.file().equals(StarChartItems.MODIFIERS_PATH)), violations.toString());
    }

    @Test
    void poolEntriesCanBeGatedToATierRange() throws Exception {
        ItemModifiers.registerStarCharts(new JSONArray().put(chartModifier("CHART_EXTRA_POIS", "suffix", "CHART_EXTRA_POIS:t1=1/t6=2", "Discovery")),
                StarChartItems.MODIFIERS_PATH);

        assertEquals(TierRange.between(3, 15), PoolEntry.parse("@CHART_EXTRA_POIS:4:t3-15", ModifierSlot.SUFFIX, ItemType.STAR_CHART).tiers());
        assertEquals(4f, PoolEntry.parse("@CHART_EXTRA_POIS:4:t3-15", ModifierSlot.SUFFIX, ItemType.STAR_CHART).weight());
        assertEquals(TierRange.atLeast(5), PoolEntry.parse("@CHART_EXTRA_POIS:t5+", ModifierSlot.SUFFIX, ItemType.STAR_CHART).tiers());
        assertEquals(1f, PoolEntry.parse("@CHART_EXTRA_POIS:t5+", ModifierSlot.SUFFIX, ItemType.STAR_CHART).weight());
        assertEquals(TierRange.between(7, 7), PoolEntry.parse("@CHART_EXTRA_POIS:2:t7", ModifierSlot.SUFFIX, ItemType.STAR_CHART).tiers());
        assertEquals(TierRange.ANY, PoolEntry.parse("@CHART_EXTRA_POIS:2", ModifierSlot.SUFFIX, ItemType.STAR_CHART).tiers());
        assertThrows(IllegalArgumentException.class, () -> PoolEntry.parse("@CHART_EXTRA_POIS:2:3", ModifierSlot.SUFFIX, ItemType.STAR_CHART));
        assertThrows(IllegalArgumentException.class, () -> PoolEntry.parse("@CHART_EXTRA_POIS:2:t9-3", ModifierSlot.SUFFIX, ItemType.STAR_CHART));
        assertThrows(IllegalArgumentException.class, () -> PoolEntry.parse("@CHART_EXTRA_POIS:2:tx", ModifierSlot.SUFFIX, ItemType.STAR_CHART));
        assertThrows(IllegalArgumentException.class, () -> PoolEntry.parse("@CHART_EXTRA_POIS", ModifierSlot.SUFFIX, ItemType.HULL_ITEM));
    }

    @Test
    void chartModifiersLoadIntoTheSharedCatalogueWithoutDisturbingShipModifiers() throws Exception {
        ItemModifiers.register(new JSONArray().put(ItemFixtures.modifierRow("HULL_MULT", "prefix", "HULL_MULT:4:6", "Reinforced")));
        ItemModifiers.registerStarCharts(new JSONArray()
                .put(chartModifier("CHART_EXTRA_POIS", "suffix", "CHART_EXTRA_POIS:t1=1/t6=2", "Discovery"))
                .put(chartModifier("HULL_MULT", "suffix", "CHART_EXTRA_POIS:1:2", "Copy"))
                .put(ItemFixtures.modifierRow("CHART_REWARD_PERCENT", "suffix", "subroutine;star_chart", "CHART_REWARD_PERCENT:1:2", "Mixed")),
                StarChartItems.MODIFIERS_PATH);

        assertNotNull(ItemModifiers.get("CHART_EXTRA_POIS"));
        assertEquals(List.of(ItemType.STAR_CHART), List.copyOf(ItemModifiers.itemTypes("CHART_EXTRA_POIS")));
        assertTrue(ItemModifiers.get("CHART_EXTRA_POIS").line().scalesWithTier());
        assertEquals("HULL_MULT", ItemModifiers.get("HULL_MULT").line().effectName(), "a chart row can never replace a ship modifier");
        assertNull(ItemModifiers.get("CHART_REWARD_PERCENT"), "chart rows may only roll on star charts");
        ItemModifiers.register(new JSONArray().put(ItemFixtures.modifierRow("HULL_MULT", "prefix", "HULL_MULT:4:6", "Reinforced")));
        assertNotNull(ItemModifiers.get("CHART_EXTRA_POIS"), "reloading ship modifiers keeps the chart ones");
        StarChartFixtures.clear();
        assertNull(ItemModifiers.get("CHART_EXTRA_POIS"));
        assertNotNull(ItemModifiers.get("HULL_MULT"));
    }

    @Test
    void theDataCheckKeepsTierSyntaxInTheChartFiles() throws Exception {
        JSONArray itemModifiers = new JSONArray()
                .put(ItemFixtures.modifierRow("HULL_MULT", "prefix", "HULL_MULT:t1=4-6", "Reinforced"))
                .put(ItemFixtures.modifierRow("ARMOR_PERCENT", "prefix", "star_chart", "ARMOR_PERCENT:4:6", "Plated"));
        JSONArray chartModifiers = new JSONArray()
                .put(chartModifier("CHART_EXTRA_POIS", "suffix", "CHART_EXTRA_POIS:t1=1/t20=2", "Discovery"))
                .put(ItemFixtures.modifierRow("CHART_REWARD_PERCENT", "suffix", "hull_item", "CHART_REWARD_PERCENT:1:2", "Bounty"));
        JSONArray charts = new JSONArray()
                .put(chart("chart_a", "QUATERNARY", "", "@CHART_EXTRA_POIS:2:t3-40"))
                .put(chart("chart_b", "SINGLE", "", "@CHART_REWARD_PERCENT:2:t3; @CHART_MISSING"));
        JSONArray implicits = new JSONArray().put(new JSONObject().put("system_type", "NEBULA").put("implicits", "@CHART_EXTRA_POIS"));
        JSONArray subroutines = new JSONArray().put(ItemFixtures.row("chip", "subroutine", "@HULL_MULT:5:t3"));

        List<String> errors = chartErrors(itemModifiers, chartModifiers, charts, implicits);
        List<String> shipErrors = ItemDataIntegrity.checkWithStarCharts(itemModifiers, subroutines, new JSONArray(), new JSONArray(),
                new JSONArray(), new JSONArray(), new JSONArray()).stream().map(Violation::message).toList();

        assertTrue(errors.contains("HULL_MULT gives HULL_MULT tier bands, but only star chart modifiers in " + StarChartItems.MODIFIERS_PATH
                + " have tiers"), errors.toString());
        assertTrue(errors.contains("ARMOR_PERCENT rolls on star_chart; star chart modifiers belong in " + StarChartItems.MODIFIERS_PATH), errors.toString());
        assertTrue(errors.contains("CHART_EXTRA_POIS starts a tier band above tier 15"), errors.toString());
        assertTrue(errors.contains("CHART_REWARD_PERCENT is in " + StarChartItems.MODIFIERS_PATH + ", so its item_types must be star_chart only"),
                errors.toString());
        assertTrue(errors.contains("chart_a has system type \"QUATERNARY\"; expected one of " + StarChartDefinitions.SYSTEM_TYPES + " or any"),
                errors.toString());
        assertTrue(errors.contains("chart_a entry \"@CHART_EXTRA_POIS:2:t3-40\" reaches past tier 15"), errors.toString());
        assertTrue(errors.contains("chart_b references CHART_MISSING, which is not in " + StarChartItems.MODIFIERS_PATH), errors.toString());
        assertTrue(errors.stream().anyMatch(error -> error.contains("NEBULA")), errors.toString());
        assertTrue(shipErrors.contains("chip entry \"@HULL_MULT:5:t3\" has a tier range, but only star charts have tiers"), shipErrors.toString());
    }

    @Test
    void chartsCanStartAtAMinimumTierAndTheDataCheckRejectsBadOnes() throws Exception {
        ItemModifiers.registerStarCharts(new JSONArray().put(chartModifier("CHART_EXTRA_POIS", "suffix", "CHART_EXTRA_POIS:t1=1", "Discovery")),
                StarChartItems.MODIFIERS_PATH);
        StarChartDefinitions.register(new JSONArray()
                .put(chart("chart_low", "SINGLE", "", "@CHART_EXTRA_POIS"))
                .put(chart("chart_high", "SINGLE", "", "@CHART_EXTRA_POIS").put("min_tier", "6"))
                .put(chart("chart_zero", "SINGLE", "", "@CHART_EXTRA_POIS").put("min_tier", "0"))
                .put(chart("chart_word", "SINGLE", "", "@CHART_EXTRA_POIS").put("min_tier", "six")));

        assertEquals(StarChart.LOWEST_TIER, StarChartDefinitions.minTier("chart_low"));
        assertEquals(6, StarChartDefinitions.minTier("chart_high"));
        assertNull(ItemDefinitions.get("chart_zero"));
        assertNull(ItemDefinitions.get("chart_word"));
        assertEquals(6, StarChartDefinitions.tierAtLeastMinimum("chart_high", 2));
        assertEquals(9, StarChartDefinitions.tierAtLeastMinimum("chart_high", 9));
        assertEquals(StarChart.HIGHEST_TIER, StarChartDefinitions.tierAtLeastMinimum("chart_low", 40));
        assertTrue(StarChartDefinitions.rollsAtTier("chart_high", 6));
        assertFalse(StarChartDefinitions.rollsAtTier("chart_high", 5));

        JSONArray charts = new JSONArray()
                .put(chart("chart_zero", "SINGLE", "", "@CHART_EXTRA_POIS").put("min_tier", "0"))
                .put(chart("chart_word", "SINGLE", "", "@CHART_EXTRA_POIS").put("min_tier", "six"))
                .put(chart("chart_fine", "SINGLE", "", "@CHART_EXTRA_POIS").put("min_tier", "15"));
        JSONArray chartModifiers = new JSONArray().put(chartModifier("CHART_EXTRA_POIS", "suffix", "CHART_EXTRA_POIS:t1=1", "Discovery"));
        List<String> errors = chartErrors(new JSONArray(), chartModifiers, charts, new JSONArray());

        assertTrue(errors.contains("chart_zero has minimum tier \"0\"; expected a whole number from 1 to 15 or blank"), errors.toString());
        assertTrue(errors.contains("chart_word has minimum tier \"six\"; expected a whole number from 1 to 15 or blank"), errors.toString());
        assertTrue(errors.stream().noneMatch(error -> error.contains("chart_fine")), errors.toString());
    }

    @Test
    void chartDefinitionsNeedAKnownSystemTypeAndCannotShadowAShipItem() throws Exception {
        ItemFixtures.registerMilitary();
        ItemModifiers.registerStarCharts(new JSONArray().put(chartModifier("CHART_EXTRA_POIS", "suffix", "CHART_EXTRA_POIS:t1=1", "Discovery")),
                StarChartItems.MODIFIERS_PATH);
        StarChartDefinitions.register(new JSONArray()
                .put(chart("chart_ok", "binary_far", "", "@CHART_EXTRA_POIS"))
                .put(chart("chart_any", "", "", "@CHART_EXTRA_POIS"))
                .put(chart("chart_bad", "NEBULA", "", "@CHART_EXTRA_POIS"))
                .put(chart(ItemFixtures.MILITARY, "SINGLE", "", "@CHART_EXTRA_POIS")));

        assertEquals("BINARY_FAR", StarChartDefinitions.systemType("chart_ok"));
        assertEquals(StarChartDefinitions.ANY_SYSTEM_TYPE, StarChartDefinitions.systemType("chart_any"));
        assertNull(ItemDefinitions.get("chart_bad"));
        assertEquals(ItemType.SUBROUTINE, ItemDefinitions.get(ItemFixtures.MILITARY).type());
        assertEquals(2, ItemDefinitions.ofType(ItemType.STAR_CHART).size());
    }
}
