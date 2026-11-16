package exiledsector.charts.anomaly.convoy;

import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.DropType;

final class FeedstockBonus {

    private FeedstockBonus() {
    }

    static float rollMult(ChartGenContext context) {
        ChartGenContext lines = ChartGenContext.orNone(context);
        return increasedMult(lines, 0f) * lines.anomalyMult(SupplyConvoy.CARGO_KEY);
    }

    static float extraShare(ChartGenContext context, float extraPercent) {
        ChartGenContext lines = ChartGenContext.orNone(context);
        float carried = increasedMult(lines, 0f);
        return carried > 0f ? increasedMult(lines, extraPercent) / carried : 1f;
    }

    static float breachShare(ChartGenContext context) {
        return extraShare(context, ChartGenContext.orNone(context).anomalyPercent(SupplyConvoy.BREACH_FEEDSTOCK_KEY));
    }

    static float excavationShare(ChartGenContext context) {
        return extraShare(context, ChartGenContext.orNone(context).anomalyPercent(SupplyConvoy.EXCAVATION_LOOT_KEY));
    }

    private static float increasedMult(ChartGenContext lines, float extraPercent) {
        float increased = lines.anomalyPercent(SupplyConvoy.FEEDSTOCK_KEY) + extraPercent;
        return Math.max(0f, lines.dropMult(DropType.CRAFTING_ITEM) + increased / ChartGenContext.PERCENT);
    }
}
