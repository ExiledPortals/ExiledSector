package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpoilsOfWarTest {

    @Test
    void theExtraRewardsAreTheChartsCountedLine() {
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartModifier.CHART_SKIRMISH_OFFICER_EXTRA_REWARDS, 1f),
                new AppliedEffect(ChartModifier.CHART_SKIRMISH_OFFICER_EXTRA_REWARDS, 1f)));

        assertEquals(new SpoilsOfWar(2), SpoilsOfWar.of(context));
        assertEquals(SpoilsOfWar.NONE, SpoilsOfWar.of(ChartGenContext.of(List.of())));
        assertEquals(SpoilsOfWar.NONE, SpoilsOfWar.of((ChartGenContext) null));
    }
}
