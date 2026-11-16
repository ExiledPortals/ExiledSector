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

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingLootSourceTest {

    private static final String AUGMENTATION = Nanoswarm.AUGMENTATION.itemId();
    private static final String BEAM_INHIBITOR = CraftingInhibitor.BEAM.itemId();

    private MockedStatic<LunaSettings> lunaSettingsMock;
    private MockSector mockSector;

    @BeforeEach
    void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        mockSector = new MockSector();
        BattleLootRates.register(new JSONArray()
                .put(new JSONObject().put("drop", BattleLootRates.CRAFTING_ITEM_RATE).put("per_100_dp", "1").put("curve", "linear")
                        .put("difficulty_scaling", "0"))
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
        List<FleetLoss> regular = List.of(new FleetLoss(600f, 3f, 0f, FleetLootRules.LEVEL_SCALED));
        List<FleetLoss> feedstockOnly = List.of(new FleetLoss(600f, 3f, 0f, FleetLootRules.FEEDSTOCK_ONLY));

        assertEquals(3f * CraftingLootSource.feedstock(feedstockOnly), CraftingLootSource.feedstock(regular), 1e-4f);
        assertEquals(Map.of(AUGMENTATION, 3), CraftingLootSource.rollCraftingItems(regular, OutsideChartShare.current(), new Random(1L)));
        assertTrue(CraftingLootSource.rollCraftingItems(feedstockOnly, OutsideChartShare.current(), new Random(1L)).isEmpty());
    }

    @Test
    void aLinearCraftingRateWithoutDifficultyScalingIgnoresBattleDifficultyAndSize() {
        List<FleetLoss> hardBattle = List.of(new FleetLoss(10000f, 3f, 0f, FleetLootRules.LEVEL_SCALED));

        assertEquals(Map.of(AUGMENTATION, 100), CraftingLootSource.rollCraftingItems(hardBattle, 1f, new Random(1L)));
    }

    @Test
    void aFleetWhoseLootIsSpentDropsNoFeedstockOrMaterials() {
        List<FleetLoss> spent = List.of(new FleetLoss(200f, 3f, 0f, FleetLootRules.SPENT));

        assertEquals(0f, CraftingLootSource.feedstock(spent), 1e-6f);
        assertTrue(CraftingLootSource.rollCraftingItems(spent, OutsideChartShare.current(), new Random(1L)).isEmpty());
    }

    @Test
    void aFleetWhoseSourceRollsItsMaterialsDropsNothingHere() {
        List<FleetLoss> ownLedger = List.of(new FleetLoss(300f, 1f, 0f, FleetLootRules.ownLedger(LootMultipliers.NONE)));

        assertTrue(CraftingLootSource.rollCraftingItems(ownLedger, OutsideChartShare.current(), new Random(1L)).isEmpty());
    }

    @Test
    void theShippedTableHasOneLinearCraftingItemRowWithoutDifficultyScalingAndNoPerItemCraftingRows() throws Exception {
        BattleLootRates.register(ProjectFiles.csvRows(BattleLootRates.DATA_PATH));

        BattleLootRates.Rate craftingRate = BattleLootRates.rate(BattleLootRates.CRAFTING_ITEM_RATE);
        assertNotNull(craftingRate);
        assertEquals(LootCurve.LINEAR, craftingRate.curve());
        assertEquals(0f, craftingRate.difficultyScaling());
        for (Nanoswarm nanoswarm : Nanoswarm.values()) {
            assertNull(BattleLootRates.rate(nanoswarm.itemId()), nanoswarm.name());
        }
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            assertNull(BattleLootRates.rate(inhibitor.itemId()), inhibitor.name());
        }
    }
}
