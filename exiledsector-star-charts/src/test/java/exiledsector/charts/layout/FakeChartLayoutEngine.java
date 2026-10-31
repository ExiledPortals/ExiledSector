package exiledsector.charts.layout;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class FakeChartLayoutEngine implements ChartLayoutEngine {

    public record Build(StarSystemAPI system, String starId, StarSystemType systemType, StarAge age, long firstRandomLong, ChartLayoutRecipe recipe) {
    }

    public interface Layout {
        void build(StarSystemAPI system, String starId, StarSystemType systemType, Random random);
    }

    public final List<Build> builds = new ArrayList<>();
    private final Layout layout;

    public FakeChartLayoutEngine(Layout layout) {
        this.layout = layout;
    }

    public static FakeChartLayoutEngine doingNothing() {
        return new FakeChartLayoutEngine((system, starId, systemType, random) -> {
        });
    }

    public static FakeChartLayoutEngine failing() {
        return new FakeChartLayoutEngine((system, starId, systemType, random) -> {
            throw new IllegalStateException("vanilla generator changed");
        });
    }

    public static FakeChartLayoutEngine install(FakeChartLayoutEngine engine) {
        ChartLayoutGenerator.useEngineForTests(engine);
        return engine;
    }

    @Override
    public Result build(StarSystemAPI system, String starId, StarSystemType systemType, StarAge age, Random random, ChartLayoutRecipe recipe,
                                   java.util.List<exiledsector.charts.anomaly.ChartAnomaly> anomalies) {
        long firstRandomLong = new Random(random.nextLong()).nextLong();
        builds.add(new Build(system, starId, systemType, age, firstRandomLong, recipe));
        layout.build(system, starId, systemType, random);
        return new Result(null, List.of());
    }
}
