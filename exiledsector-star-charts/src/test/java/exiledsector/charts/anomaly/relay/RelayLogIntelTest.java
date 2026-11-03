package exiledsector.charts.anomaly.relay;

import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import exiledsector.MockSector;
import exiledsector.charts.atlas.AtlasAnomalyRecord;
import exiledsector.charts.run.ChartRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelayLogIntelTest {

    private MockSector mockSector;
    private final List<IntelInfoPlugin> added = new ArrayList<>();
    private IntelManagerAPI intelManager;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        intelManager = mock(IntelManagerAPI.class);
        when(mockSector.sector.getIntelManager()).thenReturn(intelManager);
        when(intelManager.getIntel(RelayLogIntel.class)).thenAnswer(invocation -> added);
        doAnswer(invocation -> added.add(invocation.getArgument(0))).when(intelManager).addIntel(any(IntelInfoPlugin.class), eq(false));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    @Test
    void aRelayWokenOutsideItsDialogAddsOneLogIntelPerRunWithANotification() {
        ChartRun run = ChartRelaysTest.liveRelayRun();

        RelayLogIntel first = RelayLogIntel.report(mockSector.sector, run);
        RelayLogIntel second = RelayLogIntel.report(mockSector.sector, run);

        assertSame(first, second);
        assertTrue(first.belongsToRun(run.runId()));
        verify(intelManager, times(1)).addIntel(any(IntelInfoPlugin.class), eq(false));
    }

    @Test
    void theIntelListsTheLogOfEveryAwakeRelayForTheStage() {
        ChartRun run = ChartRelaysTest.liveRelayRun();
        RelayStates.add(run, RelayKind.SENSOR_ARRAY, RelayStates.WOKEN);
        RelayStates.add(run, RelayKind.NAV_BUOY, RelayStates.WOKEN);
        AtlasAnomalyRecord.Chain last = new AtlasAnomalyRecord.Chain(3, 3, null);

        List<RelayLogIntel.Entry> entries = RelayLogIntel.entries(run, last);

        assertEquals(List.of("Domain-era Nav Buoy", "Domain-era Sensor Array"), entries.stream().map(RelayLogIntel.Entry::relayName).toList());
        assertTrue(entries.get(1).log().contains("aboard the flagship"), entries.get(1).log());
    }
}
