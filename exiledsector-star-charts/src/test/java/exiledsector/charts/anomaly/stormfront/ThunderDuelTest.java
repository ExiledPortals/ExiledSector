package exiledsector.charts.anomaly.stormfront;

import com.fs.starfarer.api.campaign.LocationAPI;
import exiledsector.MockSector;
import exiledsector.charts.run.ChartRun;
import exiledsector.loot.Loot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThunderDuelTest {

    private MockSector mockSector;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    @Test
    void anOfficerFoughtOutsideTheStormDropsNothingExtra() {
        LocationAPI system = mock(LocationAPI.class);
        when(system.getTerrainCopy()).thenReturn(List.of());
        when(mockSector.playerFleet().getContainingLocation()).thenReturn(system);
        ChartRun run = new ChartRun("run1", "origin", 0f, 0f, 1);

        assertSame(ThunderDuel.NONE, ThunderDuel.at(mockSector.sector, run));
        assertSame(ThunderDuel.NONE, ThunderDuel.at(mockSector.sector, null));
        assertSame(ThunderDuel.NONE, ThunderDuel.at(null, run));
        assertSame(Loot.NONE, ThunderDuel.NONE.extraLoot(mockSector.sector, null, run));
    }
}
