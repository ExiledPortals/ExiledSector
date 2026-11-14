package exiledsector.charts.officer.reward;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.items.ItemData;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemKind;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ModifierSources;
import exiledsector.items.PoolEntry;
import exiledsector.items.RolledModifier;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartFixtures;
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
    void craftingItemsScaleWithTheChartsItemAndCraftingItemPercents() {
        assertEquals(1f, OfficerRewardDrop.craftingMult(ChartGenContext.none()), EPSILON);
        assertEquals(1.5f, OfficerRewardDrop.craftingMult(context(ChartModifier.CHART_ITEM_PERCENT, 50f)), EPSILON);
        assertEquals(1.25f, OfficerRewardDrop.craftingMult(context(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 25f)), EPSILON);
        assertEquals(2f, OfficerRewardDrop.craftingMult(context(40f, 60f)), EPSILON);
        assertEquals(0f, OfficerRewardDrop.craftingMult(context(-150f, 0f)), EPSILON);
        assertEquals(1f, OfficerRewardDrop.craftingMult(context(ChartModifier.CHART_QUANTITY_PERCENT, 80f)), EPSILON);
    }

    @Test
    void carriedCraftingItemsDropTheirCountTimesTheMultiplierAndMerge() {
        List<CarriedItem> carried = List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, SWARM, null, 4), new CarriedItem(RewardKind.INHIBITOR, INHIBITOR, null, 2),
                new CarriedItem(RewardKind.CRAFTING_ITEM, SWARM, null, 2));

        ItemDrops.SiteLoot drop = OfficerRewardDrop.roll(carried, 5, context(ChartModifier.CHART_ITEM_PERCENT, 50f), new Random(1L));

        assertEquals(Map.of(SWARM, 9, INHIBITOR, 3), drop.materials());
        assertTrue(drop.items().isEmpty());
    }

    @Test
    void aCarriedCraftingItemAlwaysDropsAtLeastOne() {
        ItemDrops.SiteLoot drop = OfficerRewardDrop.roll(List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, SWARM, null, 3)), 5, context(-100f, 0f), new Random(1L));

        assertEquals(Map.of(SWARM, 1), drop.materials());
    }

    @Test
    void carriedHullItemsDropOneRolledCopyPerCount() {
        ItemDrops.SiteLoot drop = OfficerRewardDrop.roll(List.of(new CarriedItem(RewardKind.HULL_ITEM, BRIDGE, null, 2)), 3, ChartGenContext.none(), new Random(1L));

        assertEquals(List.of(BRIDGE, BRIDGE), drop.items().stream().map(ItemData::definitionId).toList());
        assertTrue(drop.materials().isEmpty());
    }

    @Test
    void aCarriedBiomeItemDropsWithItsBiomesModifier() {
        Random random = new Random(6L);
        for (int roll = 0; roll < 20; roll++) {
            ItemDrops.SiteLoot drop = OfficerRewardDrop.roll(List.of(new CarriedItem(RewardKind.BIOME_HULL_ITEM, BRIDGE, RIFT, 1)), 3,
                    ChartGenContext.none(), random);

            assertEquals(1, drop.items().size());
            assertTrue(drop.items().get(0).explicits().stream().map(RolledModifier::modifierId).anyMatch(CHRONAL::equals));
        }
    }

    @Test
    void carriedFeedstockScalesWithTheItemAndFeedstockPercents() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_FEEDSTOCK_PERCENT, 100f),
                new ChartGenContext.AppliedEffect(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 300f)));
        List<CarriedItem> carried = List.of(new CarriedItem(RewardKind.FEEDSTOCK, FEEDSTOCK, null, 40), new CarriedItem(RewardKind.CRAFTING_ITEM, SWARM, null, 2));

        ItemDrops.SiteLoot drop = OfficerRewardDrop.roll(carried, 5, context, new Random(1L));

        assertEquals(2f, OfficerRewardDrop.feedstockMult(context), EPSILON);
        assertEquals(Map.of(FEEDSTOCK, 80, SWARM, 8), drop.materials());
    }

    @Test
    void aCarriedStarChartDropsAtTheRunTierOrSwapsForOneThatRollsThere() throws Exception {
        StarChartFixtures.registerShipped();
        try {
            CarriedItem gated = CarriedItem.of(RewardKind.STAR_CHART, StarChartFixtures.TEST_GATED);
            int highTier = StarChartFixtures.TEST_GATED_MIN_TIER + 2;

            ItemDrops.SiteLoot atHighTier = OfficerRewardDrop.roll(List.of(gated), highTier, ChartGenContext.none(), new Random(1L));
            assertEquals(List.of(StarChartFixtures.TEST_GATED), atHighTier.items().stream().map(ItemData::definitionId).toList());
            assertEquals(highTier, ((StarChart) atHighTier.items().get(0).preview()).tier());

            Random random = new Random(2L);
            for (int roll = 0; roll < 20; roll++) {
                ItemDrops.SiteLoot atLowTier = OfficerRewardDrop.roll(List.of(gated), 1, ChartGenContext.none(), random);
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
            ItemDrops.SiteLoot drop = OfficerRewardDrop.roll(List.of(CarriedItem.of(RewardKind.STAR_CHART, StarChartFixtures.TEST_FIXED)),
                    ChartTiers.CORRUPTED_TIER, ChartGenContext.none(), new Random(4L));

            assertEquals(List.of(StarChartFixtures.TEST_FIXED), drop.items().stream().map(ItemData::definitionId).toList());
            assertEquals(ChartTiers.HIGHEST_TIER, ((StarChart) drop.items().get(0).preview()).tier());
        } finally {
            StarChartFixtures.clear();
        }
    }

    @Test
    void anItemWhoseDefinitionIsGoneDropsNothing() {
        assertSame(ItemDrops.SiteLoot.NONE,
                OfficerRewardDrop.roll(List.of(CarriedItem.of(RewardKind.UNIQUE, "removed_relic")), 3, ChartGenContext.none(), new Random(1L)));
        assertSame(ItemDrops.SiteLoot.NONE, OfficerRewardDrop.roll(List.of(), 3, ChartGenContext.none(), new Random(1L)));
    }
}
