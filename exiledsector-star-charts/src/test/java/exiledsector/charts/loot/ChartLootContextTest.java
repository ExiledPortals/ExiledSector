package exiledsector.charts.loot;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import org.junit.jupiter.api.Test;

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
    void itemQuantityItemPercentAndAnExtraQuantityPercentAllAddUp() {
        ChartLootContext loot = lootWith(line(ChartModifier.CHART_QUANTITY_PERCENT, 20f), line(ChartModifier.CHART_ITEM_PERCENT, 30f));

        assertEquals(1.5f, loot.itemChances().general(), EPSILON);
        assertEquals(2f, loot.plusQuantityPercent(50f).itemChances().general(), EPSILON);
        assertSame(loot, loot.plusQuantityPercent(0f));
    }

    @Test
    void craftingItemQuantityAddsTheChartsPercentsToAnyAnomalyPercent() {
        ChartLootContext loot = lootWith(line(ChartModifier.CHART_QUANTITY_PERCENT, 20f), line(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 30f));

        assertEquals(1.5f, loot.craftingItemMult(0f), EPSILON);
        assertEquals(2f, loot.craftingItemMult(50f), EPSILON);
        assertEquals(1.5f * 3f, loot.multipliers(3f).craftingItems().mult(), EPSILON);
        assertEquals(1.5f, loot.chartDropMults(1f).cipher(), EPSILON);
    }
}
