package exiledsector.charts.anomaly;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

public final class StarAnomalies {

    public static final ChartAnomaly BLACK_HOLE = new Forced("black_hole", "black_hole", StarSystemType.SINGLE);
    public static final ChartAnomaly NEUTRON_STAR = new Forced("neutron_star", "star_neutron", StarSystemType.SINGLE);
    public static final ChartAnomaly DENSE_NEBULA = new Forced("dense_nebula", null, StarSystemType.NEBULA);

    private StarAnomalies() {
    }

    private record Forced(String id, String primaryStar, StarSystemType systemType) implements ChartAnomaly {
    }
}
