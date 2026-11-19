package exiledsector.charts.anomaly;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.items.modifier.Luck;
import exiledsector.loot.DropType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlareLuckTest {

    private static final List<String> FLARES = List.of(StellarFlares.ANOMALY_ID);
    private static final List<String> CALM = List.of(MagneticStorm.ANOMALY_ID);

    private static final List<AppliedEffect> EVERY_FLARE_NODE = List.of(
            new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_LUCKY_MODIFIERS, 1f),
            new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_LUCKY_IMPLICITS, 1f),
            new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_LUCKY_ROLLS, 4f),
            new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_ALL_LUCKY, 1f));

    @Test
    void theFlaresAloneGiveOneLuckyModifierRolledTwice() {
        assertEquals(Luck.ONE_MODIFIER, FlareLuck.dropLuck(ChartGenContext.none(), FLARES));
        assertEquals(Luck.BASE_ROLLS, FlareLuck.rolls(ChartGenContext.none(), FLARES));
    }

    @Test
    void theTreeLinesAddLuckyModifiersImplicitsRollsAndTheWholeItem() {
        ChartGenContext context = ChartGenContext.of(EVERY_FLARE_NODE);

        assertEquals(new Luck(2, true, true, 4), FlareLuck.dropLuck(context, FLARES));
        assertEquals(4, FlareLuck.rolls(context, FLARES));
    }

    @Test
    void withoutTheFlaresNothingIsLuckyAndRollsStayAtTwo() {
        ChartGenContext context = ChartGenContext.of(EVERY_FLARE_NODE);

        assertEquals(Luck.NONE, FlareLuck.dropLuck(context, CALM));
        assertEquals(Luck.BASE_ROLLS, FlareLuck.rolls(context, CALM));
        assertEquals(Luck.NONE, FlareLuck.dropLuck(null));
        assertEquals(Luck.BASE_ROLLS, FlareLuck.rolls(null));
    }

    @Test
    void theTreeRollCountIncludesGoldenHourOnlyWhileItIsAllocated() {
        assertEquals(Luck.BASE_ROLLS, FlareLuck.treeRolls(List.of()));
        assertEquals(4, FlareLuck.treeRolls(EVERY_FLARE_NODE));
    }

    @Test
    void theFlaresHullItemLinesChangeHullItemQuantityOnlyInChartsWithTheFlares() {
        ChartGenContext tree = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_HULL_ITEM_PERCENT, 10f),
                new AppliedEffect(ChartModifier.CHART_STELLAR_FLARES_HULL_ITEM_PERCENT, -40f)));

        assertEquals(-30f, tree.withAnomalyEffects(FLARES).dropPercent(DropType.HULL_ITEM), 1e-4f);
        assertEquals(0f, tree.withAnomalyEffects(CALM).dropPercent(DropType.HULL_ITEM), 1e-4f);
        assertEquals(0f, tree.dropPercent(DropType.HULL_ITEM), 1e-4f);
    }
}
