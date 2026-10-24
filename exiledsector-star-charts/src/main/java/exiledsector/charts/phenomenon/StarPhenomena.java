package exiledsector.charts.phenomenon;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

public final class StarPhenomena {

    public static final ChartPhenomenon BLACK_HOLE = new Forced("black_hole", "black_hole", StarSystemType.SINGLE);
    public static final ChartPhenomenon NEUTRON_STAR = new Forced("neutron_star", "star_neutron", StarSystemType.SINGLE);
    public static final ChartPhenomenon DENSE_NEBULA = new Forced("dense_nebula", null, StarSystemType.NEBULA);

    private StarPhenomena() {
    }

    private record Forced(String id, String primaryStar, StarSystemType systemType) implements ChartPhenomenon {

        @Override
        public Kind kind() {
            return Kind.STAR;
        }
    }
}
