package exiledsector.charts.effect;

import exiledsector.charts.anomaly.convoy.SupplyConvoy;
import exiledsector.charts.anomaly.forge.DerelictForge;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartModifierTest {

    @Test
    void everyAnomalyLineTunesItsOwnKey() {
        Set<String> keys = new HashSet<>();
        for (ChartModifier effect : ChartModifier.values()) {
            if (effect.anomalyTuningKey() != null) {
                assertTrue(keys.add(effect.anomalyTuningKey()), effect.name());
            }
        }
        assertEquals(124, keys.size());
        assertNull(ChartModifier.CHART_ITEM_PERCENT.anomalyTuningKey());
    }

    @Test
    void anomalyLinesAddToTheirAnomalysTuningAndCountedLinesRoundToWholeCounts() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_EXTRA_CONVOYS, 1.6f),
                new AppliedEffect(ChartModifier.CHART_FORGE_COST_PERCENT, -20f), new AppliedEffect(ChartModifier.CHART_FORGE_COST_PERCENT, 5f)));

        assertEquals(2f, context.anomalyPercent(SupplyConvoy.EXTRA_KEY));
        assertEquals(-15f, context.anomalyPercent(DerelictForge.COST_KEY));
    }

    @Test
    void flagAnomalyLinesAreFlagsAndRaiseTheirTuningKey() {
        assertTrue(ChartModifier.CHART_LISTENING_POST_ARMED_STATION.isFlag());
        assertFalse(ChartModifier.CHART_LISTENING_POST_STOLEN_CHARTS.isFlag());
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_LISTENING_POST_ARMED_STATION, 1f)));

        assertEquals(1f, context.anomalyPercent(ChartModifier.CHART_LISTENING_POST_ARMED_STATION.anomalyTuningKey()));
    }
}
