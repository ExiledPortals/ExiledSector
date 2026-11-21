package exiledsector.console;

import exiledsector.items.schematic.DeintegrationSchematic;
import exiledsector.items.schematic.LogisticsSchematic;
import exiledsector.items.schematic.ShipSchematic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class GrantSchematicsCommandTest {

    private ConsoleFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ConsoleFixture();
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    static Stream<Arguments> commands() {
        return Stream.of(
                Arguments.of((Supplier<GrantSchematicsCommand>) GrantDeintegrationSchematicsCommand::new, DeintegrationSchematic.INSTANCE),
                Arguments.of((Supplier<GrantSchematicsCommand>) GrantLogisticsSchematicsCommand::new, LogisticsSchematic.INSTANCE));
    }

    @ParameterizedTest
    @MethodSource("commands")
    void withNoArgumentItAddsOne(Supplier<GrantSchematicsCommand> command, ShipSchematic schematic) {
        assertEquals(CommandResult.SUCCESS, command.get().runCommand("", CommandContext.CAMPAIGN_MAP));

        verify(fixture.cargo).addSpecial(schematic.toSpecialItem(), 1f);
    }

    @ParameterizedTest
    @MethodSource("commands")
    void aCountAddsThatMany(Supplier<GrantSchematicsCommand> command, ShipSchematic schematic) {
        assertEquals(CommandResult.SUCCESS, command.get().runCommand(" 7 ", CommandContext.CAMPAIGN_MAP));

        verify(fixture.cargo).addSpecial(schematic.toSpecialItem(), 7f);
    }

    @ParameterizedTest
    @MethodSource("commands")
    void aBadCountIsRejectedWithoutAddingAnything(Supplier<GrantSchematicsCommand> command, ShipSchematic schematic) {
        assertEquals(CommandResult.BAD_SYNTAX, command.get().runCommand("lots", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX, command.get().runCommand("0", CommandContext.CAMPAIGN_MAP));
        assertEquals(CommandResult.BAD_SYNTAX,
                command.get().runCommand(String.valueOf(GrantSchematicsCommand.MAX_COPIES + 1), CommandContext.CAMPAIGN_MAP));

        fixture.verifyNothingGranted();
    }
}
