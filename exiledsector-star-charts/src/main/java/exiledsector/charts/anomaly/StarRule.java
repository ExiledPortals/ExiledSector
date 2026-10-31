package exiledsector.charts.anomaly;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

public interface StarRule extends ChartAnomaly {

    default String primaryStar() {
        return null;
    }

    default StarSystemType systemType() {
        return null;
    }

    static StarSystemType systemTypeOr(StarRule star, StarSystemType fallback) {
        return star != null && star.systemType() != null ? star.systemType() : fallback;
    }
}
