package exiledsector.charts.officer.reward;

import exiledsector.charts.ChartTables;
import exiledsector.charts.loot.ChartMaterials;
import exiledsector.items.StarChartCipher;
import exiledsector.items.StarChartFixtures;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerRewardTableTest {

    @AfterEach
    void tearDown() {
        OfficerRewardTable.clear();
        ChartMaterials.clear();
        StarChartFixtures.unregisterCraftingItems();
    }

    private static JSONObject row(String id, String category, String share, String tier) throws Exception {
        return new JSONObject().put("id", id).put("category", category).put("share", share).put("tier", tier);
    }

    @Test
    void rowsWithoutACategoryAreSharesAndRowsWithOneAreItems() throws Exception {
        OfficerRewardTable.register(new JSONArray()
                .put(row("hull_item", "", "40", ""))
                .put(row("unique", "", "-3", ""))
                .put(row("not_a_kind", "", "10", ""))
                .put(row("swarm", "crafting_item", "", "low"))
                .put(row("prefix", "inhibitor", "", "mid"))
                .put(row("wrong_category", "hull_item", "", "low"))
                .put(row("wrong_tier", "crafting_item", "", "legendary"))
                .put(row("bad:id", "crafting_item", "", "high")));

        assertEquals(Map.of(RewardKind.HULL_ITEM, 40f, RewardKind.UNIQUE, 0f), OfficerRewardTable.shares());
        assertEquals(List.of("swarm"), OfficerRewardTable.itemsIn(RewardKind.CRAFTING_ITEM));
        assertEquals(List.of("prefix"), OfficerRewardTable.itemsIn(RewardKind.INHIBITOR));
        assertEquals(CraftingTier.LOW, OfficerRewardTable.tierOf("swarm"));
        assertEquals(RewardKind.INHIBITOR, OfficerRewardTable.categoryOf("prefix"));
        assertNull(OfficerRewardTable.tierOf("wrong_category"));
        assertNull(OfficerRewardTable.tierOf("wrong_tier"));
        assertNull(OfficerRewardTable.tierOf("bad:id"));
    }

    private static JSONObject feedstockRow(String id, String weight, String minCount, String maxCount) throws Exception {
        return row(id, "feedstock", "", "").put("weight", weight).put("min_count", minCount).put("max_count", maxCount);
    }

    @Test
    void feedstockRowsNeedAWeightAndACountRange() throws Exception {
        OfficerRewardTable.register(new JSONArray()
                .put(feedstockRow("plain", "4", "30", "90"))
                .put(feedstockRow("fixed", "1", "5", "5"))
                .put(feedstockRow("weightless", "0", "1", "2"))
                .put(feedstockRow("from_zero", "1", "0", "2"))
                .put(feedstockRow("upside_down", "1", "9", "3")));

        assertEquals(List.of("plain", "fixed"), OfficerRewardTable.itemsIn(RewardKind.FEEDSTOCK));
        assertEquals(RewardKind.FEEDSTOCK, OfficerRewardTable.categoryOf("plain"));
        assertNull(OfficerRewardTable.tierOf("plain"));
        assertEquals(4f, OfficerRewardTable.weightOf("plain"));
        assertEquals(0f, OfficerRewardTable.weightOf("weightless"));
        Random random = new Random(3L);
        for (int roll = 0; roll < 200; roll++) {
            int count = OfficerRewardTable.rollCount("plain", random);
            assertTrue(count >= 30 && count <= 90, String.valueOf(count));
        }
        assertEquals(5, OfficerRewardTable.rollCount("fixed", random));
        assertEquals(1, OfficerRewardTable.rollCount("unknown", random));
    }

    @Test
    void aCraftingRowRollsUpToItsTiersMaximum() throws Exception {
        OfficerRewardTable.register(new JSONArray().put(row("swarm", "crafting_item", "", "low")));
        Random random = new Random(5L);
        Set<Integer> counts = new HashSet<>();
        for (int roll = 0; roll < 200; roll++) {
            counts.add(OfficerRewardTable.rollCount("swarm", random));
        }

        assertEquals(CraftingTier.LOW.maxCount(), counts.stream().mapToInt(Integer::intValue).max().orElseThrow());
        assertEquals(1, counts.stream().mapToInt(Integer::intValue).min().orElseThrow());
    }

    @Test
    void clearingTheTableLeavesNothing() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));
        OfficerRewardTable.clear();

        assertTrue(OfficerRewardTable.shares().isEmpty());
        assertTrue(OfficerRewardTable.itemsIn(RewardKind.CRAFTING_ITEM).isEmpty());
    }

    @Test
    void theShippedTableSharesTheOffersAcrossEveryKind() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));

        assertEquals(Map.of(RewardKind.HULL_ITEM, 35f, RewardKind.BIOME_HULL_ITEM, 15f, RewardKind.CRAFTING_ITEM, 30f, RewardKind.INHIBITOR, 7f,
                RewardKind.ENGRAM, 8f, RewardKind.FEEDSTOCK, 8f, RewardKind.STAR_CHART, 7f, RewardKind.UNIQUE, 5f), OfficerRewardTable.shares());
    }

    @Test
    void theShippedFeedstocksAreTheNaniteAndInfusedFeedstocks() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));

        assertEquals(List.of("exiledSector_nanite_feedstock", "exiledSector_threat_feedstock", "exiledSector_shroud_feedstock",
                "exiledSector_phase_feedstock"), OfficerRewardTable.itemsIn(RewardKind.FEEDSTOCK));
    }

    @Test
    void everyShippedItemHasATierAndAChartMaterialsAmount() throws Exception {
        OfficerRewardTable.register(ChartTables.csvRows(OfficerRewardTable.PATH));
        StarChartCipher.registerAll();
        ChartMaterials.register(ChartTables.csvRows(ChartMaterials.PATH));
        List<String> itemIds = new ArrayList<>(OfficerRewardTable.itemsIn(RewardKind.CRAFTING_ITEM));
        itemIds.addAll(OfficerRewardTable.itemsIn(RewardKind.INHIBITOR));

        assertFalse(itemIds.isEmpty());
        for (String itemId : itemIds) {
            assertNotNull(OfficerRewardTable.tierOf(itemId), itemId);
            assertTrue(ChartMaterials.covers(itemId), itemId);
        }
    }
}
