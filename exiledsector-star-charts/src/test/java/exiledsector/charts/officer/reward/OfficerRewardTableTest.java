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
import java.util.List;
import java.util.Map;

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
                RewardKind.ENGRAM, 8f, RewardKind.UNIQUE, 5f), OfficerRewardTable.shares());
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
