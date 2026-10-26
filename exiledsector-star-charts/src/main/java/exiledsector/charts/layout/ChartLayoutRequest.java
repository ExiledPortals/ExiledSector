package exiledsector.charts.layout;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.anomaly.skirmish.SkirmishSide;
import exiledsector.items.StarChart;

import java.util.List;
import java.util.Random;

public record ChartLayoutRequest(long seed, StarSystemType systemType, int tier, ChartGenContext chartContext, ChartLayoutRecipe recipe,
                                 Long chartSeed) {

    public ChartLayoutRequest {
        if (systemType != null && !ChartLayoutGenerator.isAllowed(systemType)) {
            systemType = null;
        }
        tier = ChartTiers.clamp(tier);
        chartContext = chartContext == null ? ChartGenContext.none() : chartContext;
        recipe = recipe == null ? ChartLayoutRecipe.ORDINARY : recipe;
    }

    public long contentSeed() {
        return chartSeed == null ? seed : chartSeed;
    }

    public ChartLayoutRequest(long seed, StarSystemType systemType, int tier, ChartGenContext chartContext) {
        this(seed, systemType, tier, chartContext, null, null);
    }

    public ChartLayoutRequest(long seed, StarSystemType systemType, int tier) {
        this(seed, systemType, tier, null);
    }

    public ChartLayoutRequest(long seed, StarSystemType systemType) {
        this(seed, systemType, ChartTiers.LOWEST_TIER);
    }

    public ChartLayoutRequest withRecipe(ChartLayoutRecipe layoutRecipe) {
        return new ChartLayoutRequest(seed, systemType, tier, chartContext, layoutRecipe, chartSeed);
    }

    public ChartLayoutRequest withAnomalies(List<String> anomalyIds) {
        return new ChartLayoutRequest(seed, systemType, tier, chartContext.withAnomalies(anomalyIds), recipe, chartSeed);
    }

    public ChartLayoutRequest withSkirmishSide(SkirmishSide side) {
        return new ChartLayoutRequest(seed, systemType, tier, chartContext.withSkirmishSide(side), recipe, chartSeed);
    }

    public static ChartLayoutRequest randomFrom(Random random) {
        return new ChartLayoutRequest(random.nextLong(), null);
    }

    public static ChartLayoutRequest forChart(StarChart chart) {
        return new ChartLayoutRequest(chart.seed(), ChartLayoutGenerator.parseType(chart.systemType()), chart.tier(), ChartGenContext.from(chart));
    }
}
