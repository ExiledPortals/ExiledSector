package exiledsector.loot;

import exiledsector.MockSector;
import exiledsector.ProjectFiles;
import exiledsector.items.CraftingDrops;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.Nanoswarm;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleMaterialsTest {

    private static final String AUGMENTATION = Nanoswarm.AUGMENTATION.itemId();
    private static final String BEAM_INHIBITOR = CraftingInhibitor.BEAM.itemId();

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockSector mockSector;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        mockSector = new MockSector();
        BattleLootRates.register(new JSONArray()
                .put(new JSONObject().put("drop", CraftingDrops.RATE_DROP).put("per_100_dp", "1").put("curve", "diminishing"))
                .put(new JSONObject().put("drop", "hull_item").put("per_100_dp", "5").put("curve", "linear")));
        CraftingDrops.register(new JSONArray()
                .put(new JSONObject().put("item", AUGMENTATION).put("weight", "1").put("chart_only", "false"))
                .put(new JSONObject().put("item", BEAM_INHIBITOR).put("weight", "50").put("chart_only", "true")));
    }

    @AfterEach
    void tearDown() {
        BattleLootRates.clear();
        CraftingDrops.clear();
        mockSector.close();
        lunaSettingsMock.close();
    }

    @Test
    void aFeedstockOnlyFleetDropsBaseFeedstockAndNoMaterials() {
        BattleMaterials regular = new BattleMaterials();
        regular.record(600f, 3f, FleetLootRules.LEVEL_SCALED);
        BattleMaterials feedstockOnly = new BattleMaterials();
        feedstockOnly.record(600f, 3f, FleetLootRules.FEEDSTOCK_ONLY);

        assertEquals(3f * feedstockOnly.feedstock(), regular.feedstock(), 1e-4f);
        assertEquals(Map.of(AUGMENTATION, 3), regular.rollCraftingItems(OutsideChartShare.current(), new Random(1L)));
        assertTrue(feedstockOnly.rollCraftingItems(OutsideChartShare.current(), new Random(1L)).isEmpty());
    }

    @Test
    void craftingItemsIgnoreBattleDifficultyAndTheDiminishingCurve() {
        BattleMaterials hardBattle = new BattleMaterials();
        hardBattle.record(10000f, 3f, FleetLootRules.LEVEL_SCALED);

        assertEquals(Map.of(AUGMENTATION, 100), hardBattle.rollCraftingItems(1f, new Random(1L)));
    }

    @Test
    void aFleetWhoseLootIsSpentDropsNoFeedstockOrMaterials() {
        BattleMaterials spent = new BattleMaterials();
        spent.record(200f, 3f, FleetLootRules.SPENT);

        assertEquals(0f, spent.feedstock(), 1e-6f);
        assertTrue(spent.rollCraftingItems(OutsideChartShare.current(), new Random(1L)).isEmpty());
    }

    @Test
    void aFleetWhoseSourceRollsItsMaterialsDropsNothingHere() {
        BattleMaterials battle = new BattleMaterials();
        battle.record(300f, 1f, FleetLootRules.ownLedger(LootMultipliers.NONE, 1f));

        assertTrue(battle.rollCraftingItems(OutsideChartShare.current(), new Random(1L)).isEmpty());
    }

    @Test
    void theShippedTableHasOneCraftingItemRowAndNoPerItemCraftingRows() throws Exception {
        BattleLootRates.register(ProjectFiles.csvRows(BattleLootRates.DATA_PATH));

        assertNotNull(BattleLootRates.rate(CraftingDrops.RATE_DROP));
        for (Nanoswarm nanoswarm : Nanoswarm.values()) {
            assertNull(BattleLootRates.rate(nanoswarm.itemId()), nanoswarm.name());
        }
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            assertNull(BattleLootRates.rate(inhibitor.itemId()), inhibitor.name());
        }
    }
}
