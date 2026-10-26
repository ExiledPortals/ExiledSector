package exiledsector.charts;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class FakeChartLayoutEngine implements ChartLayoutEngine {

    record Build(StarSystemAPI system, String starId, StarSystemType systemType, StarAge age, long firstRandomLong, ChartLayoutRecipe recipe) {
    }

    interface Layout {
        void build(StarSystemAPI system, String starId, StarSystemType systemType, Random random);
    }

    final List<Build> builds = new ArrayList<>();
    private final Layout layout;

    FakeChartLayoutEngine(Layout layout) {
        this.layout = layout;
    }

    static FakeChartLayoutEngine doingNothing() {
        return new FakeChartLayoutEngine((system, starId, systemType, random) -> {
        });
    }

    static FakeChartLayoutEngine failing() {
        return new FakeChartLayoutEngine((system, starId, systemType, random) -> {
            throw new IllegalStateException("vanilla generator changed");
        });
    }

    static FakeChartLayoutEngine install(FakeChartLayoutEngine engine) {
        ChartLayoutGenerator.useEngineForTests(engine);
        return engine;
    }

    @Override
    public SectorEntityToken build(StarSystemAPI system, String starId, StarSystemType systemType, StarAge age, Random random, ChartLayoutRecipe recipe,
                                   java.util.List<exiledsector.charts.anomaly.ChartAnomaly> anomalies) {
        long firstRandomLong = new Random(random.nextLong()).nextLong();
        builds.add(new Build(system, starId, systemType, age, firstRandomLong, recipe));
        layout.build(system, starId, systemType, random);
        return null;
    }
}
