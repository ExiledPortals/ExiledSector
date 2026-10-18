package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.Random;

public record ChartLayoutRequest(long seed, StarSystemType systemType, int tier) {

    public ChartLayoutRequest {
        if (systemType != null && !ChartLayoutGenerator.isAllowed(systemType)) {
            systemType = null;
        }
        tier = ChartTiers.clamp(tier);
    }

    public ChartLayoutRequest(long seed, StarSystemType systemType) {
        this(seed, systemType, ChartTiers.LOWEST_TIER);
    }

    public static ChartLayoutRequest randomFrom(Random random) {
        return new ChartLayoutRequest(random.nextLong(), null);
    }
}
