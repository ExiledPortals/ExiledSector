package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class LiveAnomalyRuns {

    public static final String RUN_ID = "run1";
    public static final String PIRATES = "pirates";
    public static final int SITE_TIER = 4;

    private LiveAnomalyRuns() {
    }

    public static StarSystemAPI systemWith(String... anomalyIds) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(RUN_ID));
        ChartRuns.replace(run(SITE_TIER, anomalyIds));
        return system;
    }

    public static ChartRun run() {
        return new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
    }

    public static ChartRun run(int tier, String... anomalyIds) {
        return run(tier, PIRATES, List.of(anomalyIds));
    }

    public static ChartRun run(int tier, String factionId, List<String> anomalyIds) {
        ChartRun run = run();
        run.recordContent(new ChartContent(tier, factionId, null, 3, 2, anomalyIds));
        return run;
    }

    public static AnomalyContentSite site(StarSystemAPI system, String factionId, ChartGenContext context, Predicate<ChartSpot> clear) {
        return new AnomalyContentSite(system, RUN_ID, SITE_TIER, factionId, context, List.of(), null, clear, salt -> new Random(salt));
    }

    public static <T extends SectorEntityToken> T placedIn(T entity, StarSystemAPI system) {
        when(entity.getContainingLocation()).thenReturn(system);
        return entity;
    }
}
