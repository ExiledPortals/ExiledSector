package exiledsector.loot;

import exiledsector.MockSector;
import exiledsector.ProjectFiles;
import exiledsector.items.crafting.material.CraftingInhibitor;
import exiledsector.items.crafting.material.Nanoswarm;
import exiledsector.items.drop.CraftingDrops;
import exiledsector.items.drop.ItemChances;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;
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
                .put(new JSONObject().put("drop", BattleLootRates.FEEDSTOCK_RATE).put("per_100_dp", "5").put("curve", "linear")
                        .put("difficulty_scaling", "1"))
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
    void battleFeedstockIsFivePer100DeploymentPointsTimesTheCappedDifficulty() {
        assertEquals(15f, CraftingLootSource.feedstock(List.of(new FleetLoss(300f, 1f, 0f, FleetLootRules.LEVEL_SCALED))), 1e-4f);
        assertEquals(30f, CraftingLootSource.feedstock(List.of(new FleetLoss(300f, 2f, 0f, FleetLootRules.LEVEL_SCALED))), 1e-4f);
        assertEquals(1500f, CraftingLootSource.feedstock(List.of(new FleetLoss(10000f, 3f, 0f, FleetLootRules.LEVEL_SCALED))), 1e-4f);
    }

    @Test
    void theChartsFeedstockQuantityAndThePlayersFoundFeedstockQuantityAddIntoOnePercent() {
        LootMultipliers chartFeedstock = new LootMultipliers(ItemChances.increasedBy(DropType.FEEDSTOCK, 50f), CraftingDrops.BASE_RARITY);
        List<FleetLoss> chartFleet = List.of(new FleetLoss(100f, 1f, 0f, FleetLootRules.ownLedger(chartFeedstock)));
        List<FleetLoss> stationFleet = List.of(new FleetLoss(100f, 3f, 0f, FleetLootRules.FEEDSTOCK_ONLY));

        try (MockedStatic<FoundItemQuantity> found = Mockito.mockStatic(FoundItemQuantity.class)) {
            found.when(() -> FoundItemQuantity.nodePercent(DropType.FEEDSTOCK)).thenReturn(50f);

            assertEquals(10f, CraftingLootSource.feedstock(chartFleet), 1e-4f);
            assertEquals(7.5f, CraftingLootSource.feedstock(stationFleet), 1e-4f);
        }
    }

    @Test
    void battleCraftingItemsReadThePlayersFoundCraftingItemQuantity() {
        List<FleetLoss> hardBattle = List.of(new FleetLoss(10000f, 3f, 0f, FleetLootRules.LEVEL_SCALED));

        try (MockedStatic<FoundItemQuantity> found = Mockito.mockStatic(FoundItemQuantity.class)) {
            found.when(() -> FoundItemQuantity.nodePercent(DropType.CRAFTING_ITEM)).thenReturn(150f);

            assertEquals(Map.of(AUGMENTATION, 250), CraftingLootSource.rollCraftingItems(hardBattle, 1f, new Random(1L)));
        }
    }

    @Test
    void thePlayersItemQuantityRaisesBattleFeedstockAndCraftingItemsWithTheirOwnStats() {
        List<FleetLoss> hardBattle = List.of(new FleetLoss(10000f, 3f, 0f, FleetLootRules.LEVEL_SCALED));
        List<FleetLoss> fleet = List.of(new FleetLoss(100f, 1f, 0f, FleetLootRules.LEVEL_SCALED));

        try (MockedStatic<FoundItemQuantity> found = Mockito.mockStatic(FoundItemQuantity.class)) {
            found.when(() -> FoundItemQuantity.nodePercent(DropType.ITEM)).thenReturn(50f);
            found.when(() -> FoundItemQuantity.nodePercent(DropType.CRAFTING_ITEM)).thenReturn(50f);
            found.when(() -> FoundItemQuantity.nodePercent(DropType.FEEDSTOCK)).thenReturn(10f);

            assertEquals(Map.of(AUGMENTATION, 200), CraftingLootSource.rollCraftingItems(hardBattle, 1f, new Random(1L)));
            assertEquals(8f, CraftingLootSource.feedstock(fleet), 1e-4f);
        }
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
    void theShippedTableHasLinearCraftingItemAndFeedstockRowsAndNoPerItemCraftingRows() throws Exception {
        BattleLootRates.register(ProjectFiles.csvRows(BattleLootRates.DATA_PATH));

        BattleLootRates.Rate craftingRate = BattleLootRates.rate(BattleLootRates.CRAFTING_ITEM_RATE);
        assertNotNull(craftingRate);
        assertEquals(LootCurve.LINEAR, craftingRate.curve());
        assertEquals(0f, craftingRate.difficultyScaling());
        BattleLootRates.Rate feedstockRate = BattleLootRates.rate(BattleLootRates.FEEDSTOCK_RATE);
        assertNotNull(feedstockRate);
        assertEquals(5f, feedstockRate.per100DeploymentPoints());
        assertEquals(LootCurve.LINEAR, feedstockRate.curve());
        assertEquals(1f, feedstockRate.difficultyScaling());
        for (Nanoswarm nanoswarm : Nanoswarm.values()) {
            assertNull(BattleLootRates.rate(nanoswarm.itemId()), nanoswarm.name());
        }
        for (CraftingInhibitor inhibitor : CraftingInhibitor.values()) {
            assertNull(BattleLootRates.rate(inhibitor.itemId()), inhibitor.name());
        }
    }
}
