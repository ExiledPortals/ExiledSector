package exiledsector.charts.loot;

import exiledsector.charts.anomaly.LootShaper;
import exiledsector.charts.anomaly.MagneticStorm;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.loot.DropType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCraftingItemsTest {

    private static final int TIER = 5;
    private static final float TOLERANCE = 0.001f;

    private static ChartLootContext chartLoot(AppliedEffect... lines) {
        return new ChartLootContext(ChartGenContext.of(List.of(lines)), TIER);
    }

    @Test
    void aBattleTheIonFrontHoldsDropsMoreCraftingItems() {
        ChartLootContext loot = chartLoot(new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_ION_FRONT_CRAFTING_ITEM_PERCENT, 50f));

        ChartLootContext held = ChartFleetLoot.fleetLootContext(loot, List.<LootShaper>of(MagneticStorm.INSTANCE), false);
        ChartLootContext elsewhere = ChartFleetLoot.fleetLootContext(loot, List.of(), false);

        assertEquals(50f, held.effects().dropPercent(DropType.CRAFTING_ITEM), TOLERANCE);
        assertEquals(0f, elsewhere.effects().dropPercent(DropType.CRAFTING_ITEM), TOLERANCE);
        assertTrue(held.quantityMult(DropType.CRAFTING_ITEM) > elsewhere.quantityMult(DropType.CRAFTING_ITEM));
        assertEquals(elsewhere.quantityMult(DropType.HULL_ITEM), held.quantityMult(DropType.HULL_ITEM), TOLERANCE);
    }

    @Test
    void theBattleCraftingItemsAddToTheChartsOwn() {
        ChartLootContext loot = chartLoot(new AppliedEffect(ChartModifier.CHART_CRAFTING_ITEM_PERCENT, 20f),
                new AppliedEffect(ChartModifier.CHART_MAGNETIC_STORM_ION_FRONT_CRAFTING_ITEM_PERCENT, 50f));

        assertEquals(70f, ChartFleetLoot.fleetLootContext(loot, List.<LootShaper>of(MagneticStorm.INSTANCE), false).effects()
                .dropPercent(DropType.CRAFTING_ITEM), TOLERANCE);
    }

    @Test
    void withoutTheIonFrontLineTheStormBattleAddsNothing() {
        ChartLootContext loot = chartLoot();

        assertEquals(0f, ChartFleetLoot.fleetLootContext(loot, List.<LootShaper>of(MagneticStorm.INSTANCE), false).effects()
                .dropPercent(DropType.CRAFTING_ITEM), TOLERANCE);
    }
}
