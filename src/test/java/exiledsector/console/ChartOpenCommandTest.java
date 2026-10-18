package exiledsector.console;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartLayout;
import exiledsector.charts.ChartRun;
import exiledsector.console.ChartOpenCommand.OpenArguments;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChartOpenCommandTest {

    @Test
    void noArgumentsMeansARandomSeedAndType() {
        OpenArguments parsed = OpenArguments.parse("  ");

        assertNull(parsed.seed());
        assertNull(parsed.systemType());
        assertNull(parsed.problem());
    }

    @Test
    void aSeedAloneATypeAloneOrBothInEitherOrderAreAccepted() {
        assertEquals(-42L, OpenArguments.parse("-42").seed());
        assertEquals(StarSystemType.BINARY_FAR, OpenArguments.parse("binary_far").systemType());
        OpenArguments both = OpenArguments.parse("123 TRINARY_2FAR");
        assertEquals(123L, both.seed());
        assertEquals(StarSystemType.TRINARY_2FAR, both.systemType());
        OpenArguments reversed = OpenArguments.parse("Single 9");
        assertEquals(9L, reversed.seed());
        assertEquals(StarSystemType.SINGLE, reversed.systemType());
    }

    @Test
    void unsafeTypesUnknownWordsAndRepeatsAreRejected() {
        assertNotNull(OpenArguments.parse("NEBULA").problem());
        assertNotNull(OpenArguments.parse("DEEP_SPACE").problem());
        assertNotNull(OpenArguments.parse("galaxy").problem());
        assertNotNull(OpenArguments.parse("1 2").problem());
        assertNotNull(OpenArguments.parse("SINGLE BINARY_CLOSE").problem());
    }

    @Test
    void badArgumentsReportTheUsageWithoutOpeningAnything() {
        CommandContext context = mock(CommandContext.class);
        when(context.isInCampaign()).thenReturn(true);
        try (MockedStatic<Console> console = Mockito.mockStatic(Console.class)) {
            assertEquals(CommandResult.BAD_SYNTAX, new ChartOpenCommand().runCommand("NEBULA", context));
            console.verify(() -> Console.showMessage(Mockito.contains("Usage: ExiledSectorChartOpen [seed] [type]")));
        }
    }

    @Test
    void theLayoutDescriptionNamesTheSeedTypeAndHowToReopenIt() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordLayout(new ChartLayout(77L, StarSystemType.BINARY_CLOSE, false, new Vector2f()));

        String described = ChartOpenCommand.describeLayout(run);

        assertTrue(described.contains("seed 77"), described);
        assertTrue(described.contains("BINARY_CLOSE"), described);
        assertTrue(described.contains("single star"), described);
        assertTrue(described.contains("ExiledSectorChartOpen 77 BINARY_CLOSE"), described);
    }
}
