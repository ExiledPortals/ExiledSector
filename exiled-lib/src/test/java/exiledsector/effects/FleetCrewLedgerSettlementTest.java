package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DataForEncounterSide;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.CrewCompositionAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
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

class FleetCrewLedgerSettlementTest {

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
        FleetCrewLedgerSettlement.Outcome outcome = FleetCrewLedgerSettlement.apply(new CrewChange(10, 2), cargo, 3);

        assertEquals(new FleetCrewLedgerSettlement.Outcome(3, 5, 0), outcome);
        verify(cargo).addCrew(3);
        verify(cargo, never()).removeCrew(anyInt());
    }

    @Test
    void aNetLossIsTakenFromTheFleetButNeverBelowZero() {
        when(cargo.getCrew()).thenReturn(2);

        FleetCrewLedgerSettlement.Outcome outcome = FleetCrewLedgerSettlement.apply(new CrewChange(1, 5), cargo, 10);

        assertEquals(new FleetCrewLedgerSettlement.Outcome(0, 0, 2), outcome);
        verify(cargo).removeCrew(2);
        verify(cargo, never()).addCrew(anyInt());
    }

    @Test
    void stolenCrewThatLiveMunitionsSpentNeedsNoRoom() {
        FleetCrewLedgerSettlement.Outcome outcome = FleetCrewLedgerSettlement.apply(new CrewChange(4, 4), cargo, 0);

        assertEquals(new FleetCrewLedgerSettlement.Outcome(0, 0, 0), outcome);
        verify(cargo, never()).addCrew(anyInt());
        verify(cargo, never()).removeCrew(anyInt());
    }

    private CampaignFleetAPI encounterWithRecoverableCrew(boolean playerWon, int recoverable) {
        CampaignFleetAPI playerFleet = mock(CampaignFleetAPI.class);
        FleetEncounterContext context = mock(FleetEncounterContext.class);
        when(context.didPlayerWinMostRecentBattleOfEncounter()).thenReturn(playerWon);
        DataForEncounterSide data = mock(DataForEncounterSide.class);
        CrewCompositionAPI crew = mock(CrewCompositionAPI.class);
        when(crew.getCrewInt()).thenReturn(recoverable);
        when(data.getRecoverableCrewLosses()).thenReturn(crew);
        when(context.getDataFor(playerFleet)).thenReturn(data);
        InteractionDialogPlugin plugin = mock(InteractionDialogPlugin.class);
        when(plugin.getContext()).thenReturn(context);
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class);
        when(dialog.getPlugin()).thenReturn(plugin);
        CampaignUIAPI campaignUI = mock(CampaignUIAPI.class);
        when(campaignUI.getCurrentInteractionDialog()).thenReturn(dialog);
        when(sector.getCampaignUI()).thenReturn(campaignUI);
        return playerFleet;
    }

    @Test
    void crewTheGameWillStillRecoverAfterAWinIsReservedOutOfTheFreeSpace() {
        CampaignFleetAPI playerFleet = encounterWithRecoverableCrew(true, 60);

        assertEquals(60, FleetCrewLedgerSettlement.crewStillToBeRecovered(playerFleet));
    }

    @Test
    void nothingIsReservedAfterALossOrOutsideAnEncounter() {
        CampaignFleetAPI lost = encounterWithRecoverableCrew(false, 60);
        assertEquals(0, FleetCrewLedgerSettlement.crewStillToBeRecovered(lost));

        when(sector.getCampaignUI().getCurrentInteractionDialog()).thenReturn(null);
        assertEquals(0, FleetCrewLedgerSettlement.crewStillToBeRecovered(lost));
    }

    @Test
    void aBattleWithoutStealingOrSacrificesSchedulesNothing() {
        FleetCrewLedgerSettlement.schedule();

        verify(sector, never()).addTransientScript(any());
    }
}
