package exiledsector.charts.anomaly.stormfront;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StormTuningTest {

    @Test
    void everyNodeLineReachesItsTuning() {
        ChartGenContext context = ChartGenContext.of(List.of(
                new AppliedEffect(ChartModifier.CHART_STORM_FRONT_HELD_SPEED_PERCENT, 80f),
                new AppliedEffect(ChartModifier.CHART_STORM_FRONT_REDACTED_FRIGATES, 10f),
                new AppliedEffect(ChartModifier.CHART_STORM_FRONT_CAPTURED, 1f),
                new AppliedEffect(ChartModifier.CHART_STORM_FRONT_CAPTURED_IMMUTABLE_CHANCE_PERCENT, 10f),
                new AppliedEffect(ChartModifier.CHART_STORM_FRONT_LUCKY_BIOME_MODIFIERS, 1f),
                new AppliedEffect(ChartModifier.CHART_STORM_FRONT_OFFICER_EXTRA_REWARDS, 1f),
                new AppliedEffect(ChartModifier.CHART_STORM_FRONT_OFFICER_EXTRA_REWARDS, 1f)));

        assertEquals(new StormTuning(80f, 10, true, 10f, true, 2), StormTuning.of(context));
        assertEquals(5, StormTuning.of(context).redactedFrigatesFewest());
    }

    @Test
    void noLinesMeansNoTuning() {
        assertEquals(StormTuning.NONE, StormTuning.of(ChartGenContext.none()));
        assertSame(StormTuning.NONE, StormTuning.of((ChartGenContext) null));
    }

    @Test
    void theHeldSlowShareNeverStopsTheFrontEntirelyPastAFullHold() {
        assertEquals(0.8f, StormRules.heldSlowShare(80f), 1e-6f);
        assertEquals(1f, StormRules.heldSlowShare(150f));
        assertEquals(-0.5f, StormRules.heldSlowShare(-50f), 1e-6f);
        assertEquals(7f, StormFrontTerrainPlugin.travelledDays(15f, 8f));
        assertEquals(0f, StormFrontTerrainPlugin.travelledDays(5f, 8f));
    }

    @Test
    void theRedactedFrigatesRunFromHalfTheMostUpToTheMost() {
        assertEquals(5, StormRules.redactedFrigatesFewest(10));
        assertEquals(6, StormRules.redactedFrigatesFewest(11));
        assertEquals(1, StormRules.redactedFrigatesFewest(1));
        assertEquals(0, StormRules.redactedFrigatesFewest(0));
    }
}
