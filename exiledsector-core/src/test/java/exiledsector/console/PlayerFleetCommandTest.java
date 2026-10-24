package exiledsector.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class PlayerFleetCommandTest {

    private final ConsoleFixture fixture = new ConsoleFixture();
    private final List<String> received = new ArrayList<>();

    private final PlayerFleetCommand command = new PlayerFleetCommand() {
        @Override
        protected CommandResult runForFleet(String args, CampaignFleetAPI playerFleet) {
            received.add(args);
            assertEquals(fixture.fleet, playerFleet);
            return CommandResult.SUCCESS;
        }
    };

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    void theCommandGetsTrimmedArgumentsAndThePlayerFleet() {
        assertEquals(CommandResult.SUCCESS, command.runCommand("  ravenous 250 ", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.SUCCESS, command.runCommand(null, CommandContext.CAMPAIGN_MARKET));

        assertEquals(List.of("ravenous 250", ""), received);
    }

    @Test
    void outsideTheCampaignItStopsBeforeTheCommand() {
        assertEquals(CommandResult.WRONG_CONTEXT, command.runCommand("", CommandContext.COMBAT_SIMULATION));

        fixture.console.verify(() -> Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY));
        assertEquals(List.of(), received);
    }

    @Test
    void withoutAPlayerFleetItStopsBeforeTheCommand() {
        when(fixture.sector.getPlayerFleet()).thenReturn(null);

        assertEquals(CommandResult.ERROR, command.runCommand("", CommandContext.CAMPAIGN_MAP));

        fixture.console.verify(() -> Console.showMessage("No player fleet found."));
        assertEquals(List.of(), received);
    }
}
