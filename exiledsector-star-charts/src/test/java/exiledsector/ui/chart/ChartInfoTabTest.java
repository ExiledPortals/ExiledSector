package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContent;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartInfoTabTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private static ChartRun run() {
        ChartRun run = new ChartRun("tabRun", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordContent(new ChartContent(7, "hegemony", "boss", 4, 6));
        return run;
    }

    @Test
    void theTabShowsOnlyInsideTheRunsSystemWithTheCampaignMapInView() {
        SectorAPI sector = mockSector.sector;
        CampaignUIAPI campaignUI = mock(CampaignUIAPI.class);
        when(sector.getCampaignUI()).thenReturn(campaignUI);
        CampaignFleetAPI playerFleet = mockSector.playerFleet();
        StarSystemAPI chartSystem = mock(StarSystemAPI.class);
        ChartRun run = run();
        when(chartSystem.getOptionalUniqueId()).thenReturn(run.systemId());
        ChartRuns.store(run);

        assertNull(ChartInfoTab.shownRun(sector));

        when(playerFleet.getContainingLocation()).thenReturn(chartSystem);
        assertSame(run, ChartInfoTab.shownRun(sector));

        when(campaignUI.getCurrentCoreTab()).thenReturn(CoreUITabId.INTEL);
        assertNull(ChartInfoTab.shownRun(sector));
        when(campaignUI.getCurrentCoreTab()).thenReturn(null);
        when(campaignUI.isShowingDialog()).thenReturn(true);
        assertNull(ChartInfoTab.shownRun(sector));
    }

    @Test
    void mouseEventsAnotherListenerConsumedAreNeverRead() {
        SectorAPI sector = mockSector.sector;
        when(sector.getCampaignUI()).thenReturn(mock(CampaignUIAPI.class));
        StarSystemAPI chartSystem = mock(StarSystemAPI.class);
        ChartRun run = run();
        when(chartSystem.getOptionalUniqueId()).thenReturn(run.systemId());
        when(mockSector.playerFleet().getContainingLocation()).thenReturn(chartSystem);
        ChartRuns.store(run);
        InputEventAPI consumed = mock(InputEventAPI.class);
        when(consumed.isMouseEvent()).thenReturn(true);
        when(consumed.isConsumed()).thenReturn(true);
        when(consumed.getX()).thenThrow(new RuntimeException("Attempt to access data of a consumed InputEvent"));
        when(consumed.getY()).thenThrow(new RuntimeException("Attempt to access data of a consumed InputEvent"));

        assertDoesNotThrow(() -> new ChartInfoTab().processCampaignInputPreFleetControl(List.of(consumed)));
    }
}
