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
        return new ChartTier(15, 550f, fleetsMin, fleetsMax, 5, 5, 100f, 200, 300, 45f, true, 1100f, 1f, 3f, 2f);
    }

    private static ChartPreview.Line line(List<ChartPreview.Line> lines, Stat stat) {
        return lines.stream().filter(candidate -> candidate.stat() == stat).findFirst().orElseThrow();
    }

    @Test
    void rolledFleetsPastTheFleetLimitPreviewAsLargerAndStrongerFleets() {
        int limit = StarChartSettings.DEFAULT_FLEET_LIMIT;
        ChartTier base = new ChartTier(10, 300f, limit - 1, limit, 0, 0, 75f, 140, 180, 26f, true, 660f, 1f, 2f, 2f);
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_BASE_FLEETS, 2f)));

        List<ChartPreview.Line> lines = ChartPreview.of(context, base);

        assertEquals(List.of(Stat.FLEET_SIZE, Stat.EXTRA_FLEETS, Stat.FLEET_STRENGTH), lines.stream().map(ChartPreview.Line::stat).toList());
        ChartPreview.Line size = line(lines, Stat.FLEET_SIZE);
        assertEquals(0.75f * 300f / limit, size.value(), TOLERANCE);
        assertEquals(0.75f * 300f * 2f / limit, size.upperValue(), TOLERANCE);
        ChartPreview.Line fleets = line(lines, Stat.EXTRA_FLEETS);
        assertEquals(0f, fleets.value(), TOLERANCE);
        assertEquals(1f, fleets.upperValue(), TOLERANCE);
        ChartPreview.Line strength = line(lines, Stat.FLEET_STRENGTH);
        assertEquals(4f, strength.value(), TOLERANCE);
        assertEquals(7f, strength.upperValue(), TOLERANCE);
    }

    @Test
    void fleetsPushedPastTheSizeCapPreviewTheCapAndTheExtraStrength() {
        ChartTier base = tierWithFleets(StarChartSettings.DEFAULT_FLEET_LIMIT, StarChartSettings.DEFAULT_FLEET_LIMIT);
        ChartGenContext context = ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_FLEET_SIZE_PERCENT, 200f)));

        List<ChartPreview.Line> lines = ChartPreview.of(context, base);

        assertEquals(ChartContentPlanner.FLEET_SIZE_CAP - 550f, line(lines, Stat.FLEET_SIZE).value(), TOLERANCE);
        assertEquals(ChartContentPlanner.overflowStrength(750f, ChartContentPlanner.FLEET_SIZE_CAP), line(lines, Stat.FLEET_STRENGTH).value(), TOLERANCE);
    }

    @Test
    void crowdedFleetsPreviewPastTheSoftLimitUpToTheHardCap() {
        int limit = StarChartSettings.DEFAULT_FLEET_LIMIT;
        ChartTier base = tierWithFleets(limit, limit);

        List<ChartPreview.Line> crowded = ChartPreview.of(ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_EXTRA_FLEETS, 2f))), base);
        List<ChartPreview.Line> packed = ChartPreview.of(ChartGenContext.of(List.of(new AppliedEffect(ChartEffect.CHART_EXTRA_FLEETS, 30f))), base);

        assertEquals(List.of(Stat.EXTRA_FLEETS), crowded.stream().map(ChartPreview.Line::stat).toList());
        assertEquals(2f, crowded.get(0).value(), TOLERANCE);
        assertEquals(ChartContentPlanner.MAX_FLEETS - limit, packed.get(0).value(), TOLERANCE);
    }
}
