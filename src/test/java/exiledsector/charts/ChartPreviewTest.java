package exiledsector.charts;

import exiledsector.charts.ChartGenContext.AppliedEffect;
import exiledsector.charts.ChartPreview.Stat;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartPreviewTest {

    private static final float TOLERANCE = 0.001f;

    private static ChartTier tierWithFleets(int fleetsMin, int fleetsMax) {
        return new ChartTier(15, 1300f, fleetsMin, fleetsMax, 5, 5, 1f, 3, 200, 300, 45f, true, 400f, 1f, 3f, 2f);
    }

    private static ChartPreview.Line line(List<ChartPreview.Line> lines, Stat stat) {
        return lines.stream().filter(candidate -> candidate.stat() == stat).findFirst().orElseThrow();
    }

    @Test
    void extraFleetsPastTheFleetCapPreviewOnlyTheFleetsThatSpawnAndTheirPower() {
        ChartTier base = tierWithFleets(ChartContentPlanner.MAX_FLEETS - 2, ChartContentPlanner.MAX_FLEETS - 1);
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_EXTRA_FLEETS, 3f)));
        ChartTier adjusted = context.adjust(base);

        List<ChartPreview.Line> lines = ChartPreview.of(context, base);

        ChartPreview.Line fleets = line(lines, Stat.EXTRA_FLEETS);
        assertEquals(ChartContentPlanner.MAX_FLEETS, adjusted.fleetsMin());
        assertEquals(ChartContentPlanner.MAX_FLEETS, adjusted.fleetsMax());
        assertTrue(fleets.isRange());
        assertEquals(1f, fleets.value(), TOLERANCE);
        assertEquals(2f, fleets.upperValue(), TOLERANCE);
        assertEquals(adjusted.fleetPower() / base.fleetPower(), line(lines, Stat.FLEET_POWER).value(), TOLERANCE);
        assertTrue(line(lines, Stat.FLEET_POWER).value() > 1f);
    }

    @Test
    void fleetsAlreadyAtTheCapPreviewNoFleetChangeButStillTheExtraPower() {
        ChartTier base = tierWithFleets(ChartContentPlanner.MAX_FLEETS, ChartContentPlanner.MAX_FLEETS);
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_EXTRA_FLEETS, 2f)));

        List<ChartPreview.Line> lines = ChartPreview.of(context, base);

        assertEquals(List.of(Stat.FLEET_POWER), lines.stream().map(ChartPreview.Line::stat).toList());
        assertEquals(1f + 2f / ChartContentPlanner.MAX_FLEETS, lines.get(0).value(), TOLERANCE);
    }
}
