package exiledsector.console;

import exiledsector.items.DeintegrationSchematic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class GrantDeintegrationSchematicsCommandTest {

    private ConsoleFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ConsoleFixture();
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    void withNoArgumentItAddsOne() {
        assertEquals(CommandResult.SUCCESS, new GrantDeintegrationSchematicsCommand().runCommand("", CommandContext.CAMPAIGN_MAP));

        verify(fixture.cargo).addSpecial(DeintegrationSchematic.INSTANCE.toSpecialItem(), 1f);
    }

    @Test
    void aCountAddsThatMany() {
        assertEquals(CommandResult.SUCCESS, new GrantDeintegrationSchematicsCommand().runCommand(" 7 ", CommandContext.CAMPAIGN_MAP));

        verify(fixture.cargo).addSpecial(DeintegrationSchematic.INSTANCE.toSpecialItem(), 7f);
    }

    @Test
    void aBadCountIsRejectedWithoutAddingAnything() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantDeintegrationSchematicsCommand().runCommand("lots", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, new GrantDeintegrationSchematicsCommand().runCommand("0", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX,
                new GrantDeintegrationSchematicsCommand().runCommand(String.valueOf(GrantDeintegrationSchematicsCommand.MAX_COPIES + 1),
                        CommandContext.CAMPAIGN_MAP));

        fixture.verifyNothingGranted();
    }
}
