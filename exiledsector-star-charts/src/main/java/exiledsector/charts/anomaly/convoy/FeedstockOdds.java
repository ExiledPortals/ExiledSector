package exiledsector.charts.anomaly.convoy;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.items.crafting.material.InfusedFeedstock;

import java.util.EnumMap;
import java.util.Map;

record FeedstockOdds(Map<InfusedFeedstock, Float> reducedChancePercents, boolean mixed, float doubleChance) {

    static final FeedstockOdds EVEN = new FeedstockOdds(Map.of(), false, 0f);
    private static final Map<InfusedFeedstock, ChartModifier> CHANCE_LINES = Map.of(
            InfusedFeedstock.THREAT, ChartModifier.CHART_CONVOY_THREAT_FEEDSTOCK_CHANCE_PERCENT,
            InfusedFeedstock.SHROUD, ChartModifier.CHART_CONVOY_SHROUD_FEEDSTOCK_CHANCE_PERCENT,
            InfusedFeedstock.PHASE, ChartModifier.CHART_CONVOY_PHASE_FEEDSTOCK_CHANCE_PERCENT);

    FeedstockOdds {
        reducedChancePercents = reducedChancePercents.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(reducedChancePercents));
    }

    static FeedstockOdds of(ChartGenContext context) {
        ChartGenContext lines = ChartGenContext.orNone(context);
        Map<InfusedFeedstock, Float> reduced = new EnumMap<>(InfusedFeedstock.class);
        CHANCE_LINES.forEach((feedstock, line) -> reduced.put(feedstock, lines.anomalyPercent(line.anomalyTuningKey())));
        return new FeedstockOdds(reduced, lines.anomalyPercent(SupplyConvoy.MIXED_MANIFEST_KEY) > 0f,
                lines.anomalyPercent(SupplyConvoy.DOUBLE_FEEDSTOCK_KEY) / ChartGenContext.PERCENT);
    }

    float weight(InfusedFeedstock feedstock) {
        return Math.max(0f, 1f - reducedChancePercents.getOrDefault(feedstock, 0f) / ChartGenContext.PERCENT);
    }
}
