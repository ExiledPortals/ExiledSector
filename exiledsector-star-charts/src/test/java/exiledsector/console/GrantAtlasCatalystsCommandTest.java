package exiledsector.console;

import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.charts.atlas.AtlasCatalysts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrantAtlasCatalystsCommandTest {

    private static final AtlasCatalysts.Catalyst BREACH = new AtlasCatalysts.Catalyst("exiledSector_breach_terminus", "spatial_breach",
            AtlasCatalysts.Family.TERMINUS, 1f, 1);
    private static final AtlasCatalysts.Catalyst REDEPLOYMENT = new AtlasCatalysts.Catalyst("exiledSector_redeployment_orders", "",
            AtlasCatalysts.Family.ORDERS, 1f, 1, AtlasCatalysts.Effect.REDEPLOY, "graphics/test.png");

    private ConsoleFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ConsoleFixture();
        when(fixture.sector.getPersistentData()).thenReturn(new HashMap<>());
        AtlasCatalysts.useForTests(List.of(BREACH, REDEPLOYMENT));
    }

    @AfterEach
    void tearDown() {
        AtlasCatalysts.resetForTests();
        fixture.close();
    }

    private static CommandContext campaign() {
        CommandContext context = mock(CommandContext.class);
        when(context.isInCampaign()).thenReturn(true);
        return context;
    }

    @Test
    void argumentsTakeCopiesAndAnIdInEitherOrderWithOrWithoutThePrefix() {
        assertEquals(new GrantAtlasCatalystsCommand.GrantArguments(1, null, null), GrantAtlasCatalystsCommand.GrantArguments.parse(""));
        assertEquals(new GrantAtlasCatalystsCommand.GrantArguments(5, null, null), GrantAtlasCatalystsCommand.GrantArguments.parse("5"));
        assertEquals(new GrantAtlasCatalystsCommand.GrantArguments(3, BREACH, null),
                GrantAtlasCatalystsCommand.GrantArguments.parse("breach_terminus 3"));
        assertEquals(new GrantAtlasCatalystsCommand.GrantArguments(1, REDEPLOYMENT, null),
                GrantAtlasCatalystsCommand.GrantArguments.parse(" exiledSector_Redeployment_Orders "));
        assertNotNull(GrantAtlasCatalystsCommand.GrantArguments.parse("0").problem());
        assertNotNull(GrantAtlasCatalystsCommand.GrantArguments.parse("101").problem());
        assertNotNull(GrantAtlasCatalystsCommand.GrantArguments.parse("2 3").problem());
        assertNotNull(GrantAtlasCatalystsCommand.GrantArguments.parse("breach_terminus redeployment_orders").problem());
        assertNotNull(GrantAtlasCatalystsCommand.GrantArguments.parse("1 2 3").problem());
        String unknown = GrantAtlasCatalystsCommand.GrantArguments.parse("storm_terminus").problem();
        assertTrue(unknown.contains("breach_terminus, redeployment_orders"), unknown);
    }

    @Test
    void grantingAddsTheCopiesOfEveryCatalystOrTheOneNamed() {
        assertEquals(CommandResult.SUCCESS, new GrantAtlasCatalystsCommand().runCommand("4", campaign()));
        ArgumentCaptor<SpecialItemData> added = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(fixture.cargo, times(2)).addSpecial(added.capture(), eq(4f));
        assertEquals(List.of(BREACH.itemId(), REDEPLOYMENT.itemId()), added.getAllValues().stream().map(SpecialItemData::getId).toList());

        assertEquals(CommandResult.SUCCESS, new GrantAtlasCatalystsCommand().runCommand("redeployment_orders 2", campaign()));
        verify(fixture.cargo).addSpecial(argThat(special -> REDEPLOYMENT.itemId().equals(special.getId())), eq(2f));
    }

    @Test
    void badArgumentsOrNoLoadedCatalystsGrantNothing() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantAtlasCatalystsCommand().runCommand("unknown_orders", campaign()));
        AtlasCatalysts.resetForTests();
        assertEquals(CommandResult.ERROR, new GrantAtlasCatalystsCommand().runCommand("", campaign()));
        fixture.verifyNothingGranted();
    }
}
