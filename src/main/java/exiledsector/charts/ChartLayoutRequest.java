package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.Random;

public record ChartLayoutRequest(long seed, StarSystemType systemType) {

    public ChartLayoutRequest {
        if (systemType != null && !ChartLayoutGenerator.isAllowed(systemType)) {
            systemType = null;
        }
    }

    public static ChartLayoutRequest randomFrom(Random random) {
        return new ChartLayoutRequest(random.nextLong(), null);
    }
}
