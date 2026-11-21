package exiledsector.charts.generation;

import exiledsector.charts.ChartTables;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.loot.ChartLootContext;
import exiledsector.items.ItemTraits;
import exiledsector.items.drop.CraftingDrops;
import exiledsector.loot.DropType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartTiersTest {

    @AfterEach
    void tearDown() {
        ChartTiers.resetForTests();
    }

    @Test
    void theTierBoundsComeFromTheItemTraitsAndTheTopTierSitsOneAboveTheHighestDrop() {
        assertEquals(ItemTraits.LOWEST_TIER, ChartTiers.LOWEST_TIER);
        assertEquals(ItemTraits.HIGHEST_DROP_TIER, ChartTiers.HIGHEST_DROP_TIER);
        assertEquals(ItemTraits.HIGHEST_TIER, ChartTiers.HIGHEST_TIER);
        assertEquals(ChartTiers.HIGHEST_DROP_TIER + 1, ChartTiers.HIGHEST_TIER);
        assertTrue(ChartTiers.isValid(ChartTiers.HIGHEST_TIER));
        assertEquals(ChartTiers.HIGHEST_TIER, ChartTiers.clamp(99));
        assertEquals(ChartTiers.LOWEST_TIER, ChartTiers.clamp(-3));
    }

    @Test
    void dropTiersStopAtTheHighestDropTier() {
        assertEquals(ChartTiers.HIGHEST_DROP_TIER, ChartTiers.clampDropTier(ChartTiers.HIGHEST_TIER));
        assertEquals(ChartTiers.HIGHEST_DROP_TIER, ChartTiers.clampDropTier(ChartTiers.HIGHEST_DROP_TIER));
        assertEquals(7, ChartTiers.clampDropTier(7));
        assertEquals(ChartTiers.LOWEST_TIER, ChartTiers.clampDropTier(0));
    }

    @Test
    void theShippedTopTierIsStrongerThanTheHighestDropTier() throws Exception {
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));
        ChartTier top = ChartTiers.forTier(ChartTiers.HIGHEST_TIER);
        ChartTier highestDrop = ChartTiers.forTier(ChartTiers.HIGHEST_DROP_TIER);

        assertEquals(ChartTiers.HIGHEST_TIER, top.tier());
        assertTrue(top.fleetSize() > highestDrop.fleetSize());
        assertTrue(top.bossSize() > highestDrop.bossSize());
    }

    @Test
    void anAdjustedTierUsesTheFullRangeAndTreatsNoContextAsNone() throws Exception {
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));

        assertEquals(ChartTiers.forTier(ChartTiers.HIGHEST_TIER), ChartTiers.adjusted(ChartTiers.HIGHEST_TIER, null));
        assertEquals(ChartTiers.forTier(ChartTiers.HIGHEST_TIER), ChartTiers.adjusted(40, ChartGenContext.none()));
        assertEquals(ChartTiers.forTier(ChartTiers.LOWEST_TIER), ChartTiers.adjusted(0, ChartGenContext.none()));
    }

    @Test
    void theShippedCraftingRarityStartsAtBaseAndNeverFallsWithTier() throws Exception {
        assertEquals(CraftingDrops.BASE_RARITY, ChartTiers.craftingRarity(8));
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));

        assertEquals(CraftingDrops.BASE_RARITY, ChartTiers.craftingRarity(ChartTiers.LOWEST_TIER));
        for (int tier = ChartTiers.LOWEST_TIER + 1; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            assertTrue(ChartTiers.craftingRarity(tier) >= ChartTiers.craftingRarity(tier - 1), "tier " + tier);
        }
        assertEquals(1.66f, ChartTiers.craftingRarity(15), 1e-6f);
        assertEquals(ChartTiers.craftingRarity(ChartTiers.HIGHEST_TIER), ChartTiers.craftingRarity(99));
    }

    @Test
    void aRunsCraftingRarityIsItsTiersCraftingRarityTimesItsItemRarity() throws Exception {
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));
        ChartGenContext treasure = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_ITEM_RARITY_PERCENT, 30f)));

        assertEquals(ChartTiers.craftingRarity(5), ChartLootContext.atTier(5).craftingRarity(DropType.BASIC_CRAFTING_ITEM), 1e-6f);
        assertEquals(ChartTiers.craftingRarity(5) * 1.3f, new ChartLootContext(treasure, 5).craftingRarity(DropType.BASIC_CRAFTING_ITEM), 1e-5f);
        assertEquals(ChartTiers.craftingRarity(5) * 1.3f, new ChartLootContext(treasure, 5).craftingRarity(DropType.INHIBITOR), 1e-5f);
        assertEquals(1.3f, treasure.itemChances().rarity(DropType.ITEM), 1e-6f);
    }
}
