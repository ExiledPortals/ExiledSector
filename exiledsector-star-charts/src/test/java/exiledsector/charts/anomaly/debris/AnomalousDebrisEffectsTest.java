package exiledsector.charts.anomaly.debris;

import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnomalousDebrisEffectsTest {

    @Test
    void theTuningLinesAddFieldsAndRaiseTheEngramsDropped() {
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_EXTRA_ANOMALOUS_DEBRIS, 2f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_EXTRA_ANOMALOUS_DEBRIS, 1f),
                new ChartGenContext.AppliedEffect(ChartEffect.CHART_ANOMALOUS_DEBRIS_CURRENCY_QUANTITY_PERCENT, 50f)));

        assertEquals(3, Math.round(context.anomalyPercent(AnomalousDebris.EXTRA_KEY)));
        assertEquals(1.5f, context.anomalyMult(AnomalousDebris.CURRENCY_KEY), 0.0001f);
        assertEquals(1f, ChartGenContext.none().anomalyMult(AnomalousDebris.CURRENCY_KEY), 0.0001f);
    }
}
