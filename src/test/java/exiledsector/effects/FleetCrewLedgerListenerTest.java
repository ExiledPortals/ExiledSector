package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import exiledsector.skills.skilleffect.FleetCrewLedger;
import exiledsector.skills.skilleffect.FleetCrewLedger.CrewChange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FleetCrewLedgerListenerTest {

    private MockedStatic<Global> globalMock;
    private SectorAPI sector;
    private CargoAPI cargo;

    @BeforeEach
    void setUp() {
        sector = mock(SectorAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        cargo = mock(CargoAPI.class);
        FleetCrewLedger.drain();
    }

    @AfterEach
    void tearDown() {
        FleetCrewLedger.drain();
        globalMock.close();
    }

    @Test
    void aNetGainJoinsTheFleetOnlyUpToItsFreeCrewSpace() {
        when(cargo.getFreeCrewSpace()).thenReturn(3);

        FleetCrewLedgerListener.Outcome outcome = FleetCrewLedgerListener.apply(new CrewChange(10, 2), cargo);

        assertEquals(new FleetCrewLedgerListener.Outcome(3, 5, 0), outcome);
        verify(cargo).addCrew(3);
        verify(cargo, never()).removeCrew(anyInt());
    }

    @Test
    void aNetLossIsTakenFromTheFleetButNeverBelowZero() {
        when(cargo.getCrew()).thenReturn(2);

        FleetCrewLedgerListener.Outcome outcome = FleetCrewLedgerListener.apply(new CrewChange(1, 5), cargo);

        assertEquals(new FleetCrewLedgerListener.Outcome(0, 0, 2), outcome);
        verify(cargo).removeCrew(2);
        verify(cargo, never()).addCrew(anyInt());
    }

    @Test
    void stolenCrewThatLiveMunitionsSpentNeedsNoRoom() {
        when(cargo.getFreeCrewSpace()).thenReturn(0);

        FleetCrewLedgerListener.Outcome outcome = FleetCrewLedgerListener.apply(new CrewChange(4, 4), cargo);

        assertEquals(new FleetCrewLedgerListener.Outcome(0, 0, 0), outcome);
        verify(cargo, never()).addCrew(anyInt());
        verify(cargo, never()).removeCrew(anyInt());
    }

    @Test
    void aBattleWithoutStealingOrSacrificesSchedulesNothing() {
        new FleetCrewLedgerListener().reportPlayerEngagement(mock(EngagementResultAPI.class));

        verify(sector, never()).addTransientScript(any());
    }
}
