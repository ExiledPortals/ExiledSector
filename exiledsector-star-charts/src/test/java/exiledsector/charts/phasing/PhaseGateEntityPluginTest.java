package exiledsector.charts.phasing;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.MockSector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PhaseGateEntityPluginTest {

    private MockSector mockSector;
    private MockedStatic<PhasePull> pullMock;
    private SectorEntityToken entity;
    private PhaseGateEntityPlugin plugin;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        pullMock = Mockito.mockStatic(PhasePull.class, Mockito.CALLS_REAL_METHODS);
        pullMock.when(() -> PhasePull.finishArrival(any(), any())).thenReturn(null);
        pullMock.clearInvocations();
        entity = mock(SectorEntityToken.class);
        plugin = new PhaseGateEntityPlugin();
        plugin.init(entity, null);
        plugin.configure("graphics/planets/frozen.jpg", 140f);
    }

    @AfterEach
    void tearDown() {
        pullMock.close();
        mockSector.close();
    }

    @Test
    void theGhostGrowsClearerAsThePullProgresses() {
        assertEquals(PhaseGateEntityPlugin.MIN_GHOST_ALPHA, PhaseGateEntityPlugin.ghostAlpha(0f, -1f), 0.001f);
        assertEquals(PhaseGateEntityPlugin.MAX_GHOST_ALPHA, PhaseGateEntityPlugin.ghostAlpha(1f, -1f), 0.001f);
        assertTrue(PhaseGateEntityPlugin.MAX_GHOST_ALPHA < 1f);
    }

    @Test
    void theArrivalSolidifiesTheGhostThenFadesTheGateAway() {
        float phasedIn = PhaseGateEntityPlugin.PHASE_IN_SECONDS;
        assertEquals(1f, PhaseGateEntityPlugin.ghostAlpha(1f, phasedIn), 0.001f);
        assertEquals(1f, PhaseGateEntityPlugin.arrivalIntensity(phasedIn), 0.001f);
        assertEquals(0f, PhaseGateEntityPlugin.arrivalLevels(PhaseGateEntityPlugin.ARRIVAL_SECONDS).settle(), 0.001f);
        assertEquals(0f, PhaseGateEntityPlugin.arrivalIntensity(-1f));
        assertEquals(1f, PhaseGateEntityPlugin.arrivalLevels(-1f).settle());
    }

    @Test
    void theArrivalPlacesThePlanetOnceAndThenExpiresTheGate() {
        SectorAPI sector = mockSector.sector;
        plugin.beginArrival();
        assertTrue(plugin.isArriving());

        plugin.advance(PhaseGateEntityPlugin.PHASE_IN_SECONDS * 0.5f);
        pullMock.verify(() -> PhasePull.finishArrival(any(), any()), never());

        plugin.advance(PhaseGateEntityPlugin.PHASE_IN_SECONDS * 0.6f);
        plugin.advance(0.1f);
        pullMock.verify(() -> PhasePull.finishArrival(sector, entity));
        verify(entity, never()).setExpired(true);

        plugin.advance(PhaseGateEntityPlugin.ARRIVAL_SECONDS);
        verify(entity).setExpired(true);
    }

    @Test
    void aGateThatIsNotArrivingNeverPlacesThePlanet() {
        plugin.advance(10f);

        assertFalse(plugin.isArriving());
        pullMock.verify(() -> PhasePull.finishArrival(any(), any()), never());
    }
}
