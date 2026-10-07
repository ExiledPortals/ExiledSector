package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.effects.ShipTreeSync;
import exiledsector.skills.progression.ShipLevelSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class GrantFleetXpCommandTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<Console> consoleMock;
    private MockedStatic<ShipLevelSystem> levelSystemMock;
    private MockedStatic<ShipTreeSync> treeSyncMock;
    private SectorAPI sector;
    private CampaignFleetAPI fleet;
    private FleetDataAPI fleetData;

    @BeforeEach
    void setUp() {
        globalMock = Mockito.mockStatic(Global.class);
        consoleMock = Mockito.mockStatic(Console.class);
        levelSystemMock = Mockito.mockStatic(ShipLevelSystem.class);
        treeSyncMock = Mockito.mockStatic(ShipTreeSync.class);
        sector = mock(SectorAPI.class);
        fleet = mock(CampaignFleetAPI.class);
        fleetData = mock(FleetDataAPI.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        when(sector.getPlayerFleet()).thenReturn(fleet);
        when(fleet.getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenReturn(List.of(mock(FleetMemberAPI.class), mock(FleetMemberAPI.class)));
    }

    @AfterEach
    void tearDown() {
        levelSystemMock.close();
        treeSyncMock.close();
        consoleMock.close();
        globalMock.close();
    }

    private static CommandResult run(String args, CommandContext context) {
        return new GrantFleetXpCommand().runCommand(args, context);
    }

    @Test
    void grantsTheGivenXpToEveryShipInThePlayerFleet() {
        assertEquals(CommandResult.SUCCESS, run(" 250 ", CommandContext.CAMPAIGN_MAP));

        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToFleet(fleet, 250f));
        consoleMock.verify(() -> Console.showMessage("Granted 250 XP to 2 ships in the fleet."));
    }

    @Test
    void syncsTheFleetsTreesBeforeAwardingAndMarksTheShipsForAStatRebuildAfter() {
        assertEquals(CommandResult.SUCCESS, run("250", CommandContext.CAMPAIGN_MAP));

        InOrder order = Mockito.inOrder(ShipTreeSync.class, ShipLevelSystem.class);
        order.verify(treeSyncMock, () -> ShipTreeSync.fleetChanged(fleet));
        order.verify(levelSystemMock, () -> ShipLevelSystem.awardXpToFleet(fleet, 250f));
        order.verify(treeSyncMock, () -> ShipTreeSync.levelsChanged(any(), any()));
    }

    @Test
    void grantsAThousandXpWhenNoAmountIsGivenAndCountsASingleShipInTheSingular() {
        when(fleetData.getMembersListCopy()).thenReturn(List.of(mock(FleetMemberAPI.class)));

        assertEquals(CommandResult.SUCCESS, run("", CommandContext.CAMPAIGN_MARKET));

        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToFleet(fleet, 1000f));
        consoleMock.verify(() -> Console.showMessage("Granted 1000 XP to 1 ship in the fleet."));
    }

    @Test
    void refusesToRunOutsideTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, run("250", CommandContext.COMBAT_SIMULATION));

        consoleMock.verify(() -> Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY));
        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToFleet(any(), anyFloat()), never());
    }

    @Test
    void aNonNumericAmountIsBadSyntax() {
        assertEquals(CommandResult.BAD_SYNTAX, run("lots", CommandContext.CAMPAIGN_MAP));

        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToFleet(any(), anyFloat()), never());
    }

    @Test
    void reportsAnErrorWhenThereIsNoPlayerFleet() {
        when(sector.getPlayerFleet()).thenReturn(null);

        assertEquals(CommandResult.ERROR, run("250", CommandContext.CAMPAIGN_MAP));

        consoleMock.verify(() -> Console.showMessage("No player fleet found."));
        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToFleet(any(), anyFloat()), never());
    }
}
