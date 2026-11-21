package exiledsector.charts.anomaly.convoy;

import exiledsector.charts.generation.ChartGenContext;
import exiledsector.loot.DropType;

final class FeedstockBonus {

    private FeedstockBonus() {
    }

    static float rollMult(ChartGenContext context) {
        return increasedMult(ChartGenContext.orNone(context), 0f);
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
        return lines.itemChances().plusPercent(DropType.FEEDSTOCK, lines.anomalyPercent(SupplyConvoy.FEEDSTOCK_KEY) + extraPercent)
                .quantity(DropType.FEEDSTOCK);
    }
}
