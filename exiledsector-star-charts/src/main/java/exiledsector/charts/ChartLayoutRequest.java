package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.items.StarChart;

import java.util.Random;

public record ChartLayoutRequest(long seed, StarSystemType systemType, int tier, ChartGenContext chartContext) {

    public ChartLayoutRequest {
        if (systemType != null && !ChartLayoutGenerator.isAllowed(systemType)) {
            systemType = null;
        }
        tier = ChartTiers.clamp(tier);
        chartContext = chartContext == null ? ChartGenContext.none() : chartContext;
    }

    public ChartLayoutRequest(long seed, StarSystemType systemType, int tier) {
        this(seed, systemType, tier, null);
    }

    public ChartLayoutRequest(long seed, StarSystemType systemType) {
        this(seed, systemType, ChartTiers.LOWEST_TIER);
    }

    public static ChartLayoutRequest randomFrom(Random random) {
        return new ChartLayoutRequest(random.nextLong(), null);
    }

    public static ChartLayoutRequest forChart(StarChart chart) {
        return new ChartLayoutRequest(chart.seed(), ChartLayoutGenerator.parseType(chart.systemType()), chart.tier(), ChartGenContext.from(chart));
    }
}
