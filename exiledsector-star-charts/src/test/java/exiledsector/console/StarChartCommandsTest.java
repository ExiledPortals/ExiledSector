package exiledsector.console;

import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.items.ItemData;
import exiledsector.items.StarChartFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.Console;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartCommandsTest {

    private ConsoleFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        fixture = new ConsoleFixture();
        when(fixture.sector.getPersistentData()).thenReturn(new HashMap<>());
    }

    @AfterEach
    void tearDown() {
        fixture.close();
        StarChartFixtures.clear();
    }

    private static CommandContext campaign() {
        CommandContext context = mock(CommandContext.class);
        when(context.isInCampaign()).thenReturn(true);
        return context;
    }

    @Test
    void grantArgumentsDefaultAndValidateCountAndTier() {
        assertEquals(new GrantStarChartsCommand.GrantArguments(5, 1, null), GrantStarChartsCommand.GrantArguments.parse(""));
        assertEquals(new GrantStarChartsCommand.GrantArguments(3, 1, null), GrantStarChartsCommand.GrantArguments.parse("3"));
        assertEquals(new GrantStarChartsCommand.GrantArguments(3, 12, null), GrantStarChartsCommand.GrantArguments.parse(" 3  12 "));
        assertNotNull(GrantStarChartsCommand.GrantArguments.parse("0").problem());
        assertNotNull(GrantStarChartsCommand.GrantArguments.parse("51").problem());
        assertNotNull(GrantStarChartsCommand.GrantArguments.parse("3 17").problem());
        assertNotNull(GrantStarChartsCommand.GrantArguments.parse("3 0").problem());
        assertNotNull(GrantStarChartsCommand.GrantArguments.parse("three").problem());
        assertNotNull(GrantStarChartsCommand.GrantArguments.parse("1 2 3").problem());
    }

    @Test
    void grantingAddsChartsRolledAtTheTierToCargo() {
        CommandResult result = new GrantStarChartsCommand(new java.util.Random(3)).runCommand("4 7", campaign());

        assertEquals(CommandResult.SUCCESS, result);
        ArgumentCaptor<SpecialItemData> added = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(fixture.cargo, times(4)).addSpecial(added.capture(), anyFloat());
        for (SpecialItemData special : added.getAllValues()) {
            assertEquals(ItemData.STAR_CHART_ITEM_ID, special.getId());
            assertEquals(7, ItemData.ofStarChart(special).preview().tier());
        }
    }

    @Test
    void grantingAtALowTierNeverPicksAChartThatStartsHigher() {
        CommandResult result = new GrantStarChartsCommand(new java.util.Random(11)).runCommand("50 3", campaign());

        assertEquals(CommandResult.SUCCESS, result);
        ArgumentCaptor<SpecialItemData> added = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(fixture.cargo, times(50)).addSpecial(added.capture(), anyFloat());
        for (SpecialItemData special : added.getAllValues()) {
            ItemData chart = ItemData.ofStarChart(special);
            assertNotEquals(StarChartFixtures.TEST_GATED, chart.definitionId(), "the gated chart starts at tier 4");
            assertEquals(3, chart.preview().tier());
        }
    }

    @Test
    void badGrantArgumentsAreReportedWithoutGranting() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantStarChartsCommand().runCommand("2 99", campaign()));
        fixture.verifyNothingGranted();
    }

    @Test
    void storedChartIndicesAreOneBased() {
        assertEquals(CommandResult.BAD_SYNTAX, new ChartOpenStoredCommand().runCommand("", campaign()));
        assertEquals(CommandResult.BAD_SYNTAX, new ChartOpenStoredCommand().runCommand("0", campaign()));
        assertEquals(CommandResult.BAD_SYNTAX, new ChartOpenStoredCommand().runCommand("first", campaign()));
    }

    @Test
    void listingAbsorbsCargoChartsAndDescribesThem() {
        ItemData chart = StarChartFixtures.withEveryModifier(StarChartFixtures.TEST_GATED, 2L, 5);
        com.fs.starfarer.api.campaign.CargoStackAPI stack = mock(com.fs.starfarer.api.campaign.CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(chart.toSpecialItem());
        when(stack.getSize()).thenReturn(1f);
        when(fixture.cargo.getStacksCopy()).thenReturn(new java.util.ArrayList<>(List.of(stack)));

        assertEquals(CommandResult.SUCCESS, new ChartListCommand().runCommand("", campaign()));

        ArgumentCaptor<String> shown = ArgumentCaptor.forClass(String.class);
        fixture.console.verify(() -> Console.showMessage(shown.capture()));
        assertTrue(shown.getValue().contains("Moved 1 Star Chart"), shown.getValue());
        assertTrue(shown.getValue().contains("1. " + chart.preview().name() + " [rare, tier 5, TRINARY_2FAR]"), shown.getValue());
        verify(fixture.cargo).removeStack(stack);
    }
}
