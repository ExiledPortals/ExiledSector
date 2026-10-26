package exiledsector.console;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.ChartIds;
import exiledsector.charts.ChartLayout;
import exiledsector.charts.ChartRun;
import exiledsector.charts.anomaly.breach.SpatialBreach;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import exiledsector.charts.anomaly.skirmish.SkirmishSide;
import exiledsector.console.ChartOpenCommand.OpenArguments;
import org.junit.jupiter.api.Test;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void atlasRegionsAndLayoutsMustBeKnownAndLeaveTheOtherSettingsAlone() {
        OpenArguments unknown = OpenArguments.parse("77 region=nowhere");
        OpenArguments plain = OpenArguments.parse("77 tier=4 breach");

        assertNotNull(unknown.problem());
        assertNotNull(OpenArguments.parse("layout=nothing").problem());
        assertNull(plain.problem());
        assertNull(plain.regionId());
        assertNull(plain.layoutId());
        assertEquals(4, plain.tier());
        assertTrue(plain.breach());
    }

    @Test
    void theWordBreachAddsASpatialBreachInAnyPosition() {
        OpenArguments breach = OpenArguments.parse("BREACH 77 tier=4");
        assertTrue(breach.breach());
        assertEquals(77L, breach.seed());
        assertEquals(4, breach.tier());
        assertFalse(OpenArguments.parse("77 tier=4").breach());
        assertNotNull(OpenArguments.parse("breach breach").problem());
        assertEquals(List.of(SpatialBreach.ID), breach.forcedAnomalyIds());
        assertEquals(List.of(), OpenArguments.parse("77 tier=4").forcedAnomalyIds());
        assertEquals(List.of(SpatialBreach.ID), new OpenArguments(null, null, null, null, true, null, null, List.of(SpatialBreach.ID)).forcedAnomalyIds());
    }

    @Test
    void aSideAddsAFactionSkirmishJoinedOnThatSide() {
        OpenArguments attacker = OpenArguments.parse("77 side=ATTACKER breach");

        assertEquals(SkirmishSide.ATTACKER, attacker.side());
        assertEquals(77L, attacker.seed());
        assertEquals(List.of(SpatialBreach.ID, FactionSkirmish.ID), attacker.forcedAnomalyIds());
        assertNull(OpenArguments.parse("77").side());
        assertNotNull(OpenArguments.parse("side=both").problem());
        assertNotNull(OpenArguments.parse("side=defender side=neither").problem());
    }

    @Test
    void aTierCanBeGivenExplicitlyOrAsTheSmallerOfTwoNumbersInAnyOrder() {
        assertEquals(1, OpenArguments.parse("").tierOrDefault());
        assertNull(OpenArguments.parse("77").tier());
        OpenArguments explicit = OpenArguments.parse("tier=12 77 binary_far");
        assertEquals(12, explicit.tier());
        assertEquals(77L, explicit.seed());
        assertEquals(StarSystemType.BINARY_FAR, explicit.systemType());
        assertEquals(3, OpenArguments.parse("T3").tier());
        OpenArguments seedThenTier = OpenArguments.parse("123456 7");
        assertEquals(123456L, seedThenTier.seed());
        assertEquals(7, seedThenTier.tier());
        OpenArguments tierThenSeed = OpenArguments.parse("SINGLE 7 123456");
        assertEquals(123456L, tierThenSeed.seed());
        assertEquals(7, tierThenSeed.tier());
        OpenArguments bothSmall = OpenArguments.parse("3 9");
        assertEquals(3L, bothSmall.seed());
        assertEquals(9, bothSmall.tier());
    }

    @Test
    void unsafeTypesUnknownWordsAndRepeatsAreRejected() {
        assertNotNull(OpenArguments.parse("NEBULA").problem());
        assertNotNull(OpenArguments.parse("DEEP_SPACE").problem());
        assertNotNull(OpenArguments.parse("galaxy").problem());
        assertNotNull(OpenArguments.parse("100 200").problem());
        assertNotNull(OpenArguments.parse("1 2 3").problem());
        assertNotNull(OpenArguments.parse("tier=16").problem());
        assertNotNull(OpenArguments.parse("tier=0").problem());
        assertNotNull(OpenArguments.parse("5 6 tier=3").problem());
        assertNotNull(OpenArguments.parse("tier=3 t=4").problem());
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
        run.recordLayout(new ChartLayout(77L, StarSystemType.BINARY_CLOSE, false, new Vector2f(), null));

        String described = ChartOpenCommand.describeLayout(run);

        assertTrue(described.contains("seed 77"), described);
        assertTrue(described.contains("BINARY_CLOSE"), described);
        assertTrue(described.contains("single star"), described);
        assertTrue(described.contains("ExiledSectorChartOpen 77 BINARY_CLOSE"), described);
    }

    @Test
    void aStarlessSystemIsReopenedThroughItsRegionAndLayout() {
        ChartRun run = new ChartRun("abc", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        run.recordLayout(new ChartLayout(77L, StarSystemType.DEEP_SPACE, true, new Vector2f(), null));

        String described = ChartOpenCommand.describeLayout(run);

        assertTrue(described.contains("ExiledSectorChartOpen 77 region=ID layout=ID"), described);
    }
}
