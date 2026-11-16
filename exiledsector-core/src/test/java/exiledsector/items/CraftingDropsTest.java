package exiledsector.items;

import exiledsector.ProjectFiles;
import exiledsector.loot.BattleLootRates;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingDropsTest {

    private static final String AUGMENTATION = Nanoswarm.AUGMENTATION.itemId();
    private static final String CORRUPTION = Nanoswarm.CORRUPTION.itemId();
    private static final String IMMUTABLE = Nanoswarm.IMMUTABLE.itemId();
    private static final String BEAM_INHIBITOR = CraftingInhibitor.BEAM.itemId();
    private static final int MANY_DROPS = 40000;

    @AfterEach
    void tearDown() {
        CraftingDrops.clear();
        BattleLootRates.clear();
    }

    private static JSONObject row(String itemId, String weight, String chartOnly) throws Exception {
        return new JSONObject().put("item", itemId).put("weight", weight).put("chart_only", chartOnly);
    }

    private static void registerSmallTable() throws Exception {
        CraftingDrops.register(new JSONArray()
                .put(row(AUGMENTATION, "6", "FALSE"))
                .put(row(CORRUPTION, "3", "FALSE"))
                .put(row(IMMUTABLE, "1", "FALSE"))
                .put(row(BEAM_INHIBITOR, "2", "TRUE")));
    }

    private static int total(Map<String, Integer> drops) {
        return drops.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Test
    void theTableReadsEachItemsWeightAndWhetherItDropsOnlyInCharts() throws Exception {
        registerSmallTable();

        assertEquals(6f, CraftingDrops.weight(AUGMENTATION), 1e-6f);
        assertEquals(0f, CraftingDrops.weight("not_in_the_table"), 1e-6f);
        assertTrue(CraftingDrops.chartOnly(BEAM_INHIBITOR));
        assertFalse(CraftingDrops.chartOnly(AUGMENTATION));
        assertTrue(CraftingDrops.covers(IMMUTABLE));
        assertFalse(CraftingDrops.covers(null));
    }

    @Test
    void theBandShiftMultipliesEveryBandButCommon() throws Exception {
        registerSmallTable();

        assertEquals(6f, CraftingDrops.shiftedWeight(AUGMENTATION, 2.5f), 1e-6f);
        assertEquals(7.5f, CraftingDrops.shiftedWeight(CORRUPTION, 2.5f), 1e-6f);
        assertEquals(2.5f, CraftingDrops.shiftedWeight(IMMUTABLE, 2.5f), 1e-6f);
    }

    @Test
    void atNoShiftEachItemDropsByItsShareOfTheWeights() throws Exception {
        registerSmallTable();

        Map<String, Integer> drops = CraftingDrops.roll(MANY_DROPS, CraftingDrops.OUTSIDE_CHARTS, CraftingDrops.NO_BAND_SHIFT, new Random(7L));

        assertEquals(MANY_DROPS, total(drops));
        assertEquals(0.6f, drops.get(AUGMENTATION) / (float) MANY_DROPS, 0.01f);
        assertEquals(0.3f, drops.get(CORRUPTION) / (float) MANY_DROPS, 0.01f);
        assertEquals(0.1f, drops.get(IMMUTABLE) / (float) MANY_DROPS, 0.01f);
    }

    @Test
    void aBandShiftMovesDropsOutOfCommonWithoutChangingTheCount() throws Exception {
        registerSmallTable();

        Map<String, Integer> unshifted = CraftingDrops.roll(MANY_DROPS, CraftingDrops.OUTSIDE_CHARTS, 1f, new Random(3L));
        Map<String, Integer> shifted = CraftingDrops.roll(MANY_DROPS, CraftingDrops.OUTSIDE_CHARTS, 2f, new Random(3L));

        assertEquals(total(unshifted), total(shifted));
        assertEquals(6f / 14f, shifted.get(AUGMENTATION) / (float) MANY_DROPS, 0.01f);
        assertEquals(6f / 14f, shifted.get(CORRUPTION) / (float) MANY_DROPS, 0.01f);
        assertTrue(shifted.get(AUGMENTATION) < unshifted.get(AUGMENTATION));
        assertTrue(shifted.get(IMMUTABLE) > unshifted.get(IMMUTABLE));
    }

    @Test
    void chartOnlyItemsStayOutOfOtherSourcesAndASourceGivesOnlyItsItems() throws Exception {
        registerSmallTable();

        Map<String, Integer> outside = CraftingDrops.roll(MANY_DROPS, CraftingDrops.OUTSIDE_CHARTS, 1f, new Random(5L));
        Map<String, Integer> inside = CraftingDrops.roll(MANY_DROPS, CraftingDrops.ANY_ITEM, 1f, new Random(5L));
        Map<String, Integer> inhibitorsOnly = CraftingDrops.roll(30, BEAM_INHIBITOR::equals, 3f, new Random(5L));

        assertFalse(outside.containsKey(BEAM_INHIBITOR));
        assertTrue(inside.containsKey(BEAM_INHIBITOR));
        assertEquals(Map.of(BEAM_INHIBITOR, 30), inhibitorsOnly);
    }

    @Test
    void nothingDropsWithoutACountOrAnAllowedItem() throws Exception {
        registerSmallTable();

        assertTrue(CraftingDrops.roll(0f, CraftingDrops.ANY_ITEM, 1f, new Random(1L)).isEmpty());
        assertTrue(CraftingDrops.roll(5f, itemId -> false, 1f, new Random(1L)).isEmpty());
    }

    @Test
    void theCountIsTheCraftingItemRatePerHundredDeploymentPoints() throws Exception {
        BattleLootRates.register(new JSONArray().put(new JSONObject().put("drop", CraftingDrops.RATE_DROP).put("per_100_dp", "1.5")
                .put("curve", "linear")));

        assertEquals(6f, CraftingDrops.expectedCount(400f), 1e-5f);
        assertEquals(0f, CraftingDrops.expectedCount(-10f), 1e-6f);
    }

    @Test
    void theShippedTableCoversEveryCoreCraftingItemAndMarksTagInhibitorsChartOnly() throws Exception {
        CraftingDrops.register(ProjectFiles.csvRows(CraftingDrops.DATA_PATH));

        for (Nanoswarm nanoswarm : Nanoswarm.values()) {
            assertTrue(CraftingDrops.weight(nanoswarm.itemId()) > 0f, nanoswarm.name());
            assertFalse(CraftingDrops.chartOnly(nanoswarm.itemId()), nanoswarm.name());
        }
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            assertTrue(CraftingDrops.weight(inhibitor.itemId()) > 0f, inhibitor.name());
            assertEquals(inhibitor.inhibitedSlot() == null, CraftingDrops.chartOnly(inhibitor.itemId()), inhibitor.name());
        }
        assertTrue(CraftingDrops.weight(Nanoswarm.AUGMENTATION.itemId()) > CraftingDrops.weight(Nanoswarm.IMMUTABLE.itemId()) * 10f);
    }
}
