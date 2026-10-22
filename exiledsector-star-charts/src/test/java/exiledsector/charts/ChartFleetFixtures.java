package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ChartFleetFixtures {

    private ChartFleetFixtures() {
    }

    public static CampaignFleetAPI chartFleet(String fleetId, String runId, float startDeploymentPoints) {
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MemoryAPI fleetMemory = FakeChartContentEngine.memory();
        when(fleet.getId()).thenReturn(fleetId);
        when(fleet.getMemoryWithoutUpdate()).thenReturn(fleetMemory);
        fleetMemory.set(ChartIds.fleetRunKey(runId), true);
        fleetMemory.set(ChartIds.START_DP_MEMORY_KEY, startDeploymentPoints);
        return fleet;
    }
}
