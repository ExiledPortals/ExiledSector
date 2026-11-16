package exiledsector.charts.generation;

import exiledsector.charts.ChartTables;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.items.CraftingDrops;
import exiledsector.items.ItemTraits;
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
    void theShippedBandShiftStartsAtNoShiftAndNeverFallsWithTier() throws Exception {
        assertEquals(CraftingDrops.NO_BAND_SHIFT, ChartTiers.bandShift(8));
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));

        assertEquals(CraftingDrops.NO_BAND_SHIFT, ChartTiers.bandShift(ChartTiers.LOWEST_TIER));
        for (int tier = ChartTiers.LOWEST_TIER + 1; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            assertTrue(ChartTiers.bandShift(tier) >= ChartTiers.bandShift(tier - 1), "tier " + tier);
        }
        assertEquals(2.11f, ChartTiers.bandShift(15), 1e-6f);
        assertEquals(ChartTiers.bandShift(ChartTiers.HIGHEST_TIER), ChartTiers.bandShift(99));
    }

    @Test
    void aRunsBandShiftIsItsTiersShiftTimesItsItemRarity() throws Exception {
        ChartTiers.register(ChartTables.csvRows(ChartTiers.PATH));
        ChartGenContext treasure = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartModifier.CHART_ITEM_RARITY_PERCENT, 30f)));

        assertEquals(ChartTiers.bandShift(5), ChartGenContext.none().craftingBandShift(5), 1e-6f);
        assertEquals(ChartTiers.bandShift(5) * 1.3f, treasure.craftingBandShift(5), 1e-5f);
        assertEquals(1.3f, treasure.itemChances().rarity(), 1e-6f);
    }
}
