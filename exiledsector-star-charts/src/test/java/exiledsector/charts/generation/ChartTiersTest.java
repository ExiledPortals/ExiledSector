package exiledsector.charts.generation;

import exiledsector.charts.ChartTables;
import exiledsector.items.ItemTraits;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
}
