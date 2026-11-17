package exiledsector.charts.officer.reward;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ItemRoll;
import exiledsector.items.ModifierSources;
import exiledsector.items.PoolEntry;
import exiledsector.items.RolledModifier;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartFixtures;
import exiledsector.loot.DropType;
import exiledsector.loot.Loot;
import exiledsector.modifiers.ModifierSlot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerRewardDropTest {

    private static final String RIFT = "abyssal_rift";
    private static final String CHRONAL = "TIMEFLOW_PERCENT_ABYSSAL_RIFT";
    private static final String BRIDGE = "test_bridge";
    private static final String UNIQUE_SUBROUTINE = "test_unique_subroutine";
    private static final String UNIQUE_BRIDGE = "test_unique_bridge";
    private static final String SWARM = "exiledSector_augmentation_nanoswarm";
    private static final String INHIBITOR = "exiledSector_prefix_inhibitor";
    private static final String FEEDSTOCK = "exiledSector_nanite_feedstock";
    private static final float EPSILON = 1e-4f;

    @BeforeEach
    void setUp() throws Exception {
        ItemModifiers.register(new JSONArray().put(new JSONObject().put("id", CHRONAL).put("affix", "prefix").put("scopes", "hull_item")
                .put("lines", "TIMEFLOW_PERCENT:3:5").put("name", "Chronal")));
        ItemDefinitions.register(List.of(new ItemDefinition(BRIDGE, ItemKind.BRIDGE, BRIDGE, ItemDefinition.FALLBACK_ICON, 1f, false, "",
                List.of(PoolEntry.of(ModifierSlot.PREFIX, "HULL_PERCENT", 4f, 6f, 1f)), List.of(), "")));
        BiomeModifiers.register(new JSONArray().put(new JSONObject().put("modifier", CHRONAL).put("biome", RIFT).put("item", "bridge")));
    }

    @AfterEach
    void tearDown() {
        BiomeModifiers.unregisterAll();
        ModifierSources.clear();
        ItemDefinitions.clear();
        ItemModifiers.clear();
    }

    private static ChartGenContext context(ChartModifier effect, float value) {
        return ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(effect, value)));
    }

    private static ChartGenContext context(float itemPercent, float craftingItemPercent) {
        return ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, itemPercent),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, craftingItemPercent)));
    }

    @Test
    void carriedCraftingItemsReadItemQuantityAndTheCraftingItemPercentButNotItemPercent() {
        assertEquals(1f, ChartLootContext.atTier(5).quantityMult(DropType.CRAFTING_ITEM), EPSILON);
        assertEquals(1f, new ChartLootContext(context(ChartModifier.CHART_ITEM_PERCENT, 50f), 5).quantityMult(DropType.CRAFTING_ITEM), EPSILON);
        assertEquals(1.25f, new ChartLootContext(context(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 25f), 5).quantityMult(DropType.CRAFTING_ITEM), EPSILON);
        assertEquals(1.8f, new ChartLootContext(context(ChartModifier.CHART_QUANTITY_PERCENT, 80f), 5).quantityMult(DropType.CRAFTING_ITEM), EPSILON);
        assertEquals(0f, new ChartLootContext(context(0f, -150f), 5).quantityMult(DropType.CRAFTING_ITEM), EPSILON);
    }

    @Test
    void carriedCraftingItemsDropTheirCountTimesTheMultiplierAndMerge() {
        List<CarriedItem> carried = List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, SWARM, null, 4), new CarriedItem(RewardKind.INHIBITOR, INHIBITOR, null, 2),
                new CarriedItem(RewardKind.CRAFTING_ITEM, SWARM, null, 2));

        Loot drop = OfficerRewardDrop.roll(carried, new ChartLootContext(context(ChartModifier.CHART_QUANTITY_PERCENT, 50f), 5), new Random(1L));

        assertEquals(Map.of(SWARM, 9, INHIBITOR, 3), drop.materials());
        assertTrue(drop.items().isEmpty());
    }

    @Test
    void aCarriedCraftingItemAlwaysDropsAtLeastOne() {
        Loot drop = OfficerRewardDrop.roll(List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, SWARM, null, 3)), new ChartLootContext(context(0f, -100f), 5), new Random(1L));

        assertEquals(Map.of(SWARM, 1), drop.materials());
    }

    @Test
    void convoyFeedstockKeepsItsFinalCountWhileOtherCarriedFeedstockScales() {
        ChartLootContext loot = new ChartLootContext(context(ChartModifier.CHART_QUANTITY_PERCENT, 100f), 5);

        Loot finalCount = OfficerRewardDrop.roll(List.of(CarriedItem.withFinalCount(RewardKind.FEEDSTOCK, FEEDSTOCK, 500)), loot, new Random(1L));
        Loot scaled = OfficerRewardDrop.roll(List.of(new CarriedItem(RewardKind.FEEDSTOCK, FEEDSTOCK, null, 500)), loot, new Random(1L));

        assertEquals(Map.of(FEEDSTOCK, 500), finalCount.materials());
        assertEquals(Map.of(FEEDSTOCK, 1000), scaled.materials());
    }

    @Test
    void carriedHullItemsDropOneCopyPerCountRolledWithDropLimits() {
        Loot drop = OfficerRewardDrop.roll(List.of(new CarriedItem(RewardKind.HULL_ITEM, BRIDGE, null, 2)), ChartLootContext.atTier(3), new Random(1L));

        assertEquals(List.of(BRIDGE, BRIDGE), drop.items().stream().map(ItemData::definitionId).toList());
        assertTrue(drop.materials().isEmpty());
        Random afterCount = new Random(1L);
        afterCount.nextFloat();
        assertEquals(ItemData.rolled(ItemDefinitions.get(BRIDGE), afterCount.nextLong(), ItemRoll.dropAtRarity(1f)), drop.items().get(0));
    }

    @Test
    void carriedHullItemsScaleWithItemQuantityItemPercentAndTheirSectionPercent() {
        ChartModifier bridgePercent = ChartModifier.forDropType(DropType.BRIDGE);
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_QUANTITY_PERCENT, 50f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, 50f), new ChartGenContext.AppliedEffect(bridgePercent, 100f)));

        Loot drop = OfficerRewardDrop.roll(List.of(new CarriedItem(RewardKind.HULL_ITEM, BRIDGE, null, 2)), new ChartLootContext(context, 5), new Random(1L));

        assertEquals(3f, new ChartLootContext(context, 5).quantityMult(DropType.BRIDGE), EPSILON);
        assertEquals(6, drop.items().size());
    }

    @Test
    void aCarriedUniqueSubroutineScalesWithItemQuantityAndTheSubroutinePercentButNotTheHullItemLineWhileAUniqueHullItemKeepsItsCount() {
        ItemDefinitions.register(List.of(new ItemDefinition(UNIQUE_SUBROUTINE, ItemKind.SUBROUTINE, UNIQUE_SUBROUTINE, ItemDefinition.FALLBACK_ICON, 1f, true, "",
                        List.of(), List.of(), ""),
                new ItemDefinition(UNIQUE_BRIDGE, ItemKind.BRIDGE, UNIQUE_BRIDGE, ItemDefinition.FALLBACK_ICON, 1f, true, "", List.of(), List.of(), "")));
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_QUANTITY_PERCENT, 100f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, 50f), new ChartGenContext.AppliedEffect(ChartModifier.CHART_SUBROUTINE_PERCENT, 100f)));

        Loot drop = OfficerRewardDrop.roll(List.of(CarriedItem.of(RewardKind.UNIQUE, UNIQUE_SUBROUTINE), CarriedItem.of(RewardKind.UNIQUE, UNIQUE_BRIDGE)),
                new ChartLootContext(context, 5), new Random(1L));

        assertEquals(3, drop.items().stream().filter(item -> UNIQUE_SUBROUTINE.equals(item.definitionId())).count());
        assertEquals(1, drop.items().stream().filter(item -> UNIQUE_BRIDGE.equals(item.definitionId())).count());
    }

    @Test
    void aCarriedBiomeItemDropsWithItsBiomesModifier() {
        Random random = new Random(6L);
        for (int roll = 0; roll < 20; roll++) {
            Loot drop = OfficerRewardDrop.roll(List.of(new CarriedItem(RewardKind.BIOME_HULL_ITEM, BRIDGE, RIFT, 1)), ChartLootContext.atTier(3),
                    random);

            assertEquals(1, drop.items().size());
            assertTrue(drop.items().get(0).explicits().stream().map(RolledModifier::modifierId).anyMatch(CHRONAL::equals));
        }
    }

    @Test
    void carriedFeedstockScalesWithItemQuantityAndTheFeedstockPercent() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_FEEDSTOCK_PERCENT, 100f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 300f), new ChartGenContext.AppliedEffect(ChartModifier.CHART_ITEM_PERCENT, 500f)));
        List<CarriedItem> carried = List.of(new CarriedItem(RewardKind.FEEDSTOCK, FEEDSTOCK, null, 40), new CarriedItem(RewardKind.CRAFTING_ITEM, SWARM, null, 2));

        Loot drop = OfficerRewardDrop.roll(carried, new ChartLootContext(context, 5), new Random(1L));

        assertEquals(2f, new ChartLootContext(context, 5).quantityMult(DropType.FEEDSTOCK), EPSILON);
        assertEquals(Map.of(FEEDSTOCK, 80, SWARM, 8), drop.materials());
    }

    @Test
    void aCarriedStarChartDropsAtTheRunTierOrSwapsForOneThatRollsThere() throws Exception {
        StarChartFixtures.registerShipped();
        try {
            CarriedItem gated = CarriedItem.of(RewardKind.STAR_CHART, StarChartFixtures.TEST_GATED);
            int highTier = StarChartFixtures.TEST_GATED_MIN_TIER + 2;

            Loot atHighTier = OfficerRewardDrop.roll(List.of(gated), ChartLootContext.atTier(highTier), new Random(1L));
            assertEquals(List.of(StarChartFixtures.TEST_GATED), atHighTier.items().stream().map(ItemData::definitionId).toList());
            assertEquals(highTier, ((StarChart) atHighTier.items().get(0).preview()).tier());

            Random random = new Random(2L);
            for (int roll = 0; roll < 20; roll++) {
                Loot atLowTier = OfficerRewardDrop.roll(List.of(gated), ChartLootContext.atTier(1), random);
                assertEquals(1, atLowTier.items().size());
                assertNotEquals(StarChartFixtures.TEST_GATED, atLowTier.items().get(0).definitionId());
            }
        } finally {
            StarChartFixtures.clear();
        }
    }

    @Test
    void aStarChartFromACorruptedTierRunDropsAtTheHighestNormalTier() throws Exception {
        StarChartFixtures.registerShipped();
        try {
            Loot drop = OfficerRewardDrop.roll(List.of(CarriedItem.of(RewardKind.STAR_CHART, StarChartFixtures.TEST_FIXED)),
                    ChartLootContext.atTier(ChartTiers.HIGHEST_TIER), new Random(4L));

            assertEquals(List.of(StarChartFixtures.TEST_FIXED), drop.items().stream().map(ItemData::definitionId).toList());
            assertEquals(ChartTiers.HIGHEST_DROP_TIER, ((StarChart) drop.items().get(0).preview()).tier());
        } finally {
            StarChartFixtures.clear();
        }
    }

    @Test
    void anItemWhoseDefinitionIsGoneDropsNothing() {
        assertSame(Loot.NONE,
                OfficerRewardDrop.roll(List.of(CarriedItem.of(RewardKind.UNIQUE, "removed_relic")), ChartLootContext.atTier(3), new Random(1L)));
        assertSame(Loot.NONE, OfficerRewardDrop.roll(List.of(), ChartLootContext.atTier(3), new Random(1L)));
    }
}
