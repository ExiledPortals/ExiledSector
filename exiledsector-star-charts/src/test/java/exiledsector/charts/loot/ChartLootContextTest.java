package exiledsector.charts.loot;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.loot.DropType;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChartLootContextTest {

    private static final float EPSILON = 1e-5f;

    private static ChartLootContext lootWith(ChartGenContext.AppliedEffect... lines) {
        return new ChartLootContext(ChartGenContext.of(List.of(lines)), ChartTiers.LOWEST_TIER);
    }

    private static ChartGenContext.AppliedEffect line(ChartModifier effect, float value) {
        return new ChartGenContext.AppliedEffect(effect, value);
    }

    @Test
    void itemQuantityTheHullItemLineAndAnExtraQuantityPercentAllAddUpForHullItems() {
        ChartLootContext loot = lootWith(line(ChartModifier.CHART_QUANTITY_PERCENT, 20f), line(ChartModifier.CHART_ITEM_PERCENT, 30f));

        assertEquals(1.5f, loot.quantityMult(DropType.HULL_ITEM), EPSILON);
        assertEquals(2f, loot.plusQuantityPercent(50f).quantityMult(DropType.REACTOR), EPSILON);
        assertEquals(1.2f, loot.quantityMult(DropType.SUBROUTINE), EPSILON);
        assertEquals(1.7f, loot.plusQuantityPercent(50f).quantityMult(DropType.STAR_CHART), EPSILON);
        assertSame(loot, loot.plusQuantityPercent(0f));
    }

    @Test
    void craftingItemQuantityAddsTheChartsPercentsToAnyAnomalyPercent() {
        ChartLootContext loot = lootWith(line(ChartModifier.CHART_QUANTITY_PERCENT, 20f), line(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 30f));

        assertEquals(1.5f, loot.quantityMult(DropType.CRAFTING_ITEM), EPSILON);
        assertEquals(2f, loot.quantityMult(DropType.CRAFTING_ITEM, 50f), EPSILON);
        assertEquals(1.5f * 3f, loot.multipliers(3f).quantity(DropType.CRAFTING_ITEM), EPSILON);
        assertEquals(1.5f, loot.chartDropMults(1f).cipher(), EPSILON);
    }

    @Test
    void feedstockReadsTheFeedstockAndItemQuantityLinesAndNeverTheCraftingItemLine() {
        ChartLootContext loot = lootWith(line(ChartModifier.CHART_QUANTITY_PERCENT, 20f), line(ChartModifier.CHART_FEEDSTOCK_PERCENT, 30f),
                line(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 100f));

        assertEquals(1.5f, loot.quantityMult(DropType.FEEDSTOCK), EPSILON);
        assertEquals(1.5f * 2f, loot.multipliers(2f).quantity(DropType.FEEDSTOCK), EPSILON);
        assertEquals(1f, ChartLootContext.outsideRuns().multipliers(1f).quantity(DropType.FEEDSTOCK), EPSILON);
    }

    @Test
    void thePlayersFoundQuantityStatsAddIntoTheChartsPercentForTheirDropType() {
        ChartLootContext loot = lootWith(line(ChartModifier.CHART_QUANTITY_PERCENT, 20f), line(ChartModifier.CHART_FEEDSTOCK_PERCENT, 30f));

        try (MockedStatic<FoundItemQuantity> found = Mockito.mockStatic(FoundItemQuantity.class)) {
            found.when(() -> FoundItemQuantity.nodePercent(DropType.ITEM)).thenReturn(10f);
            found.when(() -> FoundItemQuantity.nodePercent(DropType.FEEDSTOCK)).thenReturn(50f);
            found.when(() -> FoundItemQuantity.nodePercent(DropType.CRAFTING_ITEM)).thenReturn(10f);
            found.when(() -> FoundItemQuantity.nodePercent(DropType.HULL_ITEM)).thenReturn(40f);

            assertEquals(2.1f, loot.quantityMult(DropType.FEEDSTOCK), EPSILON);
            assertEquals(1.4f, loot.quantityMult(DropType.CRAFTING_ITEM), EPSILON);
            assertEquals(1.7f, loot.quantityMult(DropType.BRIDGE), EPSILON);
            assertEquals(1.3f, loot.quantityMult(DropType.STAR_CHART), EPSILON);
            assertEquals(1.3f, loot.quantityMult(DropType.SUBROUTINE), EPSILON);
            assertEquals(1.3f, loot.quantityMult(DropType.COLONY_ITEM), EPSILON);
            assertEquals(1.3f, loot.quantityMult(DropType.WEAPON), EPSILON);
        }
    }
}
