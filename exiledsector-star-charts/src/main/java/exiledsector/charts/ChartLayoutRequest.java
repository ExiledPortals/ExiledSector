package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.items.StarChart;

import java.util.Random;

public record ChartLayoutRequest(long seed, StarSystemType systemType, int tier, ChartGenContext chartContext, ChartLayoutRecipe recipe,
                                 String phenomenonId, Long chartSeed) {

    public ChartLayoutRequest {
        if (systemType != null && !ChartLayoutGenerator.isAllowed(systemType)) {
            systemType = null;
        }
        tier = ChartTiers.clamp(tier);
        chartContext = chartContext == null ? ChartGenContext.none() : chartContext;
        recipe = recipe == null ? ChartLayoutRecipe.ORDINARY : recipe;
        phenomenonId = phenomenonId == null ? "" : phenomenonId;
    }

    public long contentSeed() {
        return chartSeed == null ? seed : chartSeed;
    }

    public ChartLayoutRequest(long seed, StarSystemType systemType, int tier, ChartGenContext chartContext) {
        this(seed, systemType, tier, chartContext, null, null, null);
    }

    public ChartLayoutRequest(long seed, StarSystemType systemType, int tier) {
        this(seed, systemType, tier, null);
    }

    public ChartLayoutRequest(long seed, StarSystemType systemType) {
        this(seed, systemType, ChartTiers.LOWEST_TIER);
    }

    public ChartLayoutRequest withRecipe(ChartLayoutRecipe layoutRecipe) {
        return new ChartLayoutRequest(seed, systemType, tier, chartContext, layoutRecipe, phenomenonId, chartSeed);
    }

    public ChartLayoutRequest withPhenomenon(String id) {
        return new ChartLayoutRequest(seed, systemType, tier, chartContext, recipe, id, chartSeed);
    }

    public static ChartLayoutRequest randomFrom(Random random) {
        return new ChartLayoutRequest(random.nextLong(), null);
    }

    public static ChartLayoutRequest forChart(StarChart chart) {
        return new ChartLayoutRequest(chart.seed(), ChartLayoutGenerator.parseType(chart.systemType()), chart.tier(), ChartGenContext.from(chart));
    }
}
