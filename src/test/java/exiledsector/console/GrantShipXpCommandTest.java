package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class GrantShipXpCommandTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<Console> consoleMock;
    private MockedStatic<ShipLevelSystem> levelSystemMock;
    private MockedStatic<ShipTreeSync> treeSyncMock;
    private SectorAPI sector;
    private FleetDataAPI fleetData;
    private FleetMemberAPI ravenous;
    private FleetMemberAPI ravenousTwo;
    private FleetMemberAPI vigilant;

    @BeforeEach
    void setUp() {
        globalMock = Mockito.mockStatic(Global.class);
        consoleMock = Mockito.mockStatic(Console.class);
        levelSystemMock = Mockito.mockStatic(ShipLevelSystem.class);
        treeSyncMock = Mockito.mockStatic(ShipTreeSync.class);
        sector = mock(SectorAPI.class);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        fleetData = mock(FleetDataAPI.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        when(sector.getPlayerFleet()).thenReturn(fleet);
        when(fleet.getFleetData()).thenReturn(fleetData);
        ravenous = member("id-1", "ISS Ravenous", "Onslaught");
        ravenousTwo = member("id-2", "ISS Ravenous II", "Lasher");
        vigilant = member("id-3", "ISS Vigilant", "Hammerhead");
        when(fleetData.getMembersListCopy()).thenReturn(List.of(ravenous, ravenousTwo, vigilant));
    }

    @AfterEach
    void tearDown() {
        treeSyncMock.close();
        levelSystemMock.close();
        consoleMock.close();
        globalMock.close();
    }

    private static FleetMemberAPI member(String id, String name, String hullName) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullName()).thenReturn(hullName);
        when(member.getId()).thenReturn(id);
        when(member.getShipName()).thenReturn(name);
        when(member.getHullSpec()).thenReturn(hull);
        return member;
    }

    private static CommandResult run(String args) {
        return new GrantShipXpCommand().runCommand(args, CommandContext.CAMPAIGN_MAP);
    }

    @Test
    void grantsTheGivenXpToTheShipWhosePartialNameMatches() {
        assertEquals(CommandResult.SUCCESS, run("vigil 250"));

        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToMember(vigilant, 250f));
        consoleMock.verify(() -> Console.showMessage("Granted 250 XP to ISS Vigilant (Hammerhead)."));
    }

    @Test
    void anExactNameWinsOverLongerNamesAndTheAmountDefaultsToAThousand() {
        assertEquals(CommandResult.SUCCESS, run("iss ravenous"));

        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToMember(ravenous, 1000f));
    }

    @Test
    void aShipNameEndingInANumberIsNotMistakenForAnAmount() {
        FleetMemberAPI lancer = member("id-4", "Lancer 2", "Lasher");
        when(fleetData.getMembersListCopy()).thenReturn(List.of(lancer));

        assertEquals(CommandResult.SUCCESS, run("Lancer 2"));

        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToMember(lancer, 1000f));
    }

    @Test
    void severalPartialMatchesAreListedAndNothingIsGranted() {
        assertEquals(CommandResult.ERROR, run("ravenous 250"));

        consoleMock.verify(() -> Console.showMessage("Several ships match \"ravenous\": ISS Ravenous (Onslaught), "
                + "ISS Ravenous II (Lasher). Use more of the ship's name."));
        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToMember(any(), anyFloat()), never());
    }

    @Test
    void noMatchingShipIsAnError() {
        assertEquals(CommandResult.ERROR, run("Conquest 250"));

        consoleMock.verify(() -> Console.showMessage("No ship in your fleet matches \"Conquest\"."));
        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToMember(any(), anyFloat()), never());
    }

    @Test
    void noShipNameIsBadSyntax() {
        assertEquals(CommandResult.BAD_SYNTAX, run("  "));
    }

    @Test
    void refusesToRunOutsideTheCampaign() {
        assertEquals(CommandResult.WRONG_CONTEXT, new GrantShipXpCommand().runCommand("vigilant", CommandContext.COMBAT_SIMULATION));

        consoleMock.verify(() -> Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY));
        levelSystemMock.verify(() -> ShipLevelSystem.awardXpToMember(any(), anyFloat()), never());
    }

    @Test
    void reportsAnErrorWhenThereIsNoPlayerFleet() {
        when(sector.getPlayerFleet()).thenReturn(null);

        assertEquals(CommandResult.ERROR, run("vigilant"));

        consoleMock.verify(() -> Console.showMessage("No player fleet found."));
    }
}
