package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.objective.ChartObjective;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class LiveAnomalyRuns {

    public static final String RUN_ID = "run1";

    private LiveAnomalyRuns() {
    }

    public static StarSystemAPI systemWith(String... anomalyIds) {
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(RUN_ID));
        ChartRun run = new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(4, "pirates", ChartObjective.CLEAR, List.of(), null, 3, 2, List.of(anomalyIds)));
        ChartRuns.replace(run);
        return system;
    }

    public static <T extends SectorEntityToken> T placedIn(T entity, StarSystemAPI system) {
        when(entity.getContainingLocation()).thenReturn(system);
        return entity;
    }
}
