package exiledsector.console;

import exiledsector.items.InfusedFeedstock;
import exiledsector.items.ItemDisassembly;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantFeedstockCommandTest {

    private ConsoleFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ConsoleFixture();
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    private static CommandContext campaign() {
        CommandContext context = mock(CommandContext.class);
        when(context.isInCampaign()).thenReturn(true);
        return context;
    }

    @Test
    void everyFeedstockHasAType() {
        assertEquals(List.of("nanite", "threat", "shroud", "phase"), List.copyOf(GrantFeedstockCommand.FEEDSTOCK_IDS_BY_TYPE.keySet()));
        assertEquals(new GrantFeedstockCommand.GrantArguments(ItemDisassembly.FEEDSTOCK_ITEM_ID, 50, null),
                GrantFeedstockCommand.GrantArguments.parse("nanite 50"));
        assertEquals(new GrantFeedstockCommand.GrantArguments(InfusedFeedstock.PHASE.itemId(), 7, null),
                GrantFeedstockCommand.GrantArguments.parse(" Phase 7 "));
    }

    @Test
    void theTypeAndAmountAreBothNeeded() {
        assertNotNull(GrantFeedstockCommand.GrantArguments.parse("").problem());
        assertNotNull(GrantFeedstockCommand.GrantArguments.parse("threat").problem());
        assertNotNull(GrantFeedstockCommand.GrantArguments.parse("10 threat").problem());
        assertNotNull(GrantFeedstockCommand.GrantArguments.parse("threat 0").problem());
        assertNotNull(GrantFeedstockCommand.GrantArguments.parse("threat 100001").problem());
        assertNotNull(GrantFeedstockCommand.GrantArguments.parse("threat 5 5").problem());
    }

    @Test
    void grantingAddsTheAmountOfTheNamedFeedstock() {
        assertEquals(CommandResult.SUCCESS, new GrantFeedstockCommand().runCommand("shroud 250", campaign()));
        verify(fixture.cargo).addSpecial(argThat(special -> InfusedFeedstock.SHROUD.itemId().equals(special.getId())), eq(250f));
    }

    @Test
    void badArgumentsGrantNothing() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantFeedstockCommand().runCommand("void 10", campaign()));
        fixture.verifyNothingGranted();
    }
}
