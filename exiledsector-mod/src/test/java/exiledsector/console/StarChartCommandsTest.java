package exiledsector.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StarChartCommandsTest {

    private MockedStatic<Global> globalMock;
    private MockedStatic<Console> consoleMock;
    private CargoAPI cargo;

    @BeforeEach
    void setUp() throws Exception {
        StarChartFixtures.registerShipped();
        globalMock = Mockito.mockStatic(Global.class);
        consoleMock = Mockito.mockStatic(Console.class);
        SectorAPI sector = mock(SectorAPI.class);
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        cargo = mock(CargoAPI.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        when(sector.getPersistentData()).thenReturn(new HashMap<>());
        when(sector.getPlayerFleet()).thenReturn(fleet);
        when(fleet.getCargo()).thenReturn(cargo);
    }

    @AfterEach
    void tearDown() {
        consoleMock.close();
        globalMock.close();
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
        assertNotNull(GrantStarChartsCommand.GrantArguments.parse("3 16").problem());
        assertNotNull(GrantStarChartsCommand.GrantArguments.parse("3 0").problem());
        assertNotNull(GrantStarChartsCommand.GrantArguments.parse("three").problem());
        assertNotNull(GrantStarChartsCommand.GrantArguments.parse("1 2 3").problem());
    }

    @Test
    void grantingAddsChartsRolledAtTheTierToCargo() {
        CommandResult result = new GrantStarChartsCommand(new java.util.Random(3)).runCommand("4 7", campaign());

        assertEquals(CommandResult.SUCCESS, result);
        ArgumentCaptor<SpecialItemData> added = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, times(4)).addSpecial(added.capture(), anyFloat());
        for (SpecialItemData special : added.getAllValues()) {
            assertEquals(ItemData.STAR_CHART_ITEM_ID, special.getId());
            assertEquals(7, ItemData.ofStarChart(special).preview().tier());
        }
    }

    @Test
    void grantingAtALowTierNeverPicksAUniqueThatStartsHigher() {
        CommandResult result = new GrantStarChartsCommand(new java.util.Random(11)).runCommand("50 3", campaign());

        assertEquals(CommandResult.SUCCESS, result);
        ArgumentCaptor<SpecialItemData> added = ArgumentCaptor.forClass(SpecialItemData.class);
        verify(cargo, times(50)).addSpecial(added.capture(), anyFloat());
        for (SpecialItemData special : added.getAllValues()) {
            ItemData chart = ItemData.ofStarChart(special);
            assertNotEquals(StarChartFixtures.TEST_GATED_UNIQUE, chart.definitionId(), "the gated unique starts at tier 4");
            assertEquals(3, chart.preview().tier());
        }
    }

    @Test
    void badGrantArgumentsAreReportedWithoutGranting() {
        assertEquals(CommandResult.BAD_SYNTAX, new GrantStarChartsCommand().runCommand("2 99", campaign()));
        verify(cargo, times(0)).addSpecial(Mockito.any(SpecialItemData.class), anyFloat());
    }

    @Test
    void storedChartIndicesAreOneBased() {
        assertEquals(1, ChartOpenStoredCommand.parseIndex("1"));
        assertEquals(12, ChartOpenStoredCommand.parseIndex(" 12 "));
        assertNull(ChartOpenStoredCommand.parseIndex("0"));
        assertNull(ChartOpenStoredCommand.parseIndex(""));
        assertNull(ChartOpenStoredCommand.parseIndex("first"));
        assertEquals(CommandResult.BAD_SYNTAX, new ChartOpenStoredCommand().runCommand("", campaign()));
    }

    @Test
    void listingAbsorbsCargoChartsAndDescribesThem() {
        ItemData chart = ItemData.rolledAtTier(exiledsector.items.ItemDefinitions.get(StarChartFixtures.TEST_GATED_UNIQUE), 2L, 5);
        com.fs.starfarer.api.campaign.CargoStackAPI stack = mock(com.fs.starfarer.api.campaign.CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(chart.toSpecialItem());
        when(stack.getSize()).thenReturn(1f);
        when(cargo.getStacksCopy()).thenReturn(new java.util.ArrayList<>(List.of(stack)));

        assertEquals(CommandResult.SUCCESS, new ChartListCommand().runCommand("", campaign()));

        ArgumentCaptor<String> shown = ArgumentCaptor.forClass(String.class);
        consoleMock.verify(() -> Console.showMessage(shown.capture()));
        assertTrue(shown.getValue().contains("Moved 1 Star Chart"), shown.getValue());
        assertTrue(shown.getValue().contains("1. Test Gated Unique Chart [unique, tier 5, TRINARY_2FAR]"), shown.getValue());
        assertTrue(shown.getValue().contains("implicit: "), shown.getValue());
        verify(cargo).removeStack(stack);
    }
}
