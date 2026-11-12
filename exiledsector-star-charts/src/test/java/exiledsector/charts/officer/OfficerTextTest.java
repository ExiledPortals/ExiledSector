package exiledsector.charts.officer;

import exiledsector.charts.atlas.AtlasAnomalyChance;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasMemory;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasTerritoryPlacer;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.officer.reward.CarriedItem;
import exiledsector.charts.officer.reward.RewardKind;
import exiledsector.charts.officer.turn.OfficerTurn;
import exiledsector.i18n.Translation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfficerTextTest {

    private static final AtlasCell FROM = new AtlasCell(3, -2);
    private static final AtlasCell TO = new AtlasCell(-5, 8);
    private static final AtlasRules RULES = new AtlasRules(List.of(new AtlasRegion(AtlasRegion.NORMAL, 5f, 0, "", "", Map.of(), Map.of(), Map.of()),
            new AtlasRegion("graveyard", 5f, 0, "", "", Map.of(), Map.of(), Map.of())), Map.of("compact", 1f), Map.of("pirates", 1f),
            AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER, List.of(), List.of());

    @Test
    void aHistoryEntryNamesItsEventAndItsFirstAndLastSystems() {
        assertEquals(new OfficerText.HistoryEntry(AtlasTerritoryPlacer.MOVED_HISTORY, FROM, TO),
                OfficerText.HistoryEntry.parse(AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.MOVED_HISTORY, FROM, TO)));
        assertEquals(new OfficerText.HistoryEntry(AtlasTerritoryPlacer.ARRIVED_HISTORY, TO, TO),
                OfficerText.HistoryEntry.parse(AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.ARRIVED_HISTORY, TO)));
    }

    @Test
    void aMalformedHistoryEntryParsesToNothing() {
        assertNull(OfficerText.HistoryEntry.parse(null));
        assertNull(OfficerText.HistoryEntry.parse(""));
        assertNull(OfficerText.HistoryEntry.parse(AtlasTerritoryPlacer.ARRIVED_HISTORY));
        assertNull(OfficerText.HistoryEntry.parse(":" + TO.pack()));
        assertNull(OfficerText.HistoryEntry.parse("arrived:somewhere"));
    }

    @Test
    void detailLinesListTheCarriedItemsThenEachScar() {
        AtlasOfficer officer = AtlasOfficer.waiting(1, "hegemony", "commander", "steady", new AtlasOfficer.OfficerSeeds(1L, 2L, 3L, 4L), null)
                .withCarried(List.of(new CarriedItem(RewardKind.CRAFTING_ITEM, "kernel", null, 2))).withScars(Map.of("retired_scar", 2));

        List<AtlasLine> lines = OfficerText.detailLines(officer);

        assertEquals(2, lines.size());
        assertEquals(AtlasLine.Tone.MARKER, lines.get(0).tone());
        assertTrue(lines.get(0).plain().contains("kernel"), lines.get(0).plain());
        assertEquals(AtlasLine.Tone.STYLED, lines.get(1).tone());
        assertTrue(lines.get(1).plain().contains("retired_scar"), lines.get(1).plain());
    }

    @Test
    void anOfficerWithNothingCarriedAndNoScarsHasNoDetailLines() {
        AtlasOfficer officer = AtlasOfficer.waiting(1, "hegemony", "commander", "steady", new AtlasOfficer.OfficerSeeds(1L, 2L, 3L, 4L), null);

        assertTrue(OfficerText.detailLines(officer).isEmpty());
    }

    @Test
    void theTitleJoinsRankAndNameAndDropsAMissingName() {
        assertEquals("Commander Ilse Varga", OfficerText.title("Commander", "Ilse Varga"));
        assertEquals("Commander", OfficerText.title("Commander", ""));
    }

    @Test
    void aHistoryLineNamesTheRegionsOfItsSystems() {
        AtlasGenerator generator = new AtlasGenerator(1L, RULES, AtlasUnlocks.NONE, AtlasMemory.NONE);
        String fromRegion = AtlasNames.region(generator.node(FROM).regionId());
        String toRegion = AtlasNames.region(generator.node(TO).regionId());

        assertEquals(Translation.msg("chart.officer.history.moved").arg("from", fromRegion).arg("region", toRegion).text(),
                OfficerText.historyLine(generator, new OfficerText.HistoryEntry(AtlasTerritoryPlacer.MOVED_HISTORY, FROM, TO)));
        assertEquals("", OfficerText.historyLine(generator, new OfficerText.HistoryEntry("unknown_event", FROM, TO)));
        assertEquals("", OfficerText.historyLine(generator, null));
    }

    @Test
    void aFledOfficerNamesTheSystemTheyFledFrom() {
        AtlasOfficer officer = AtlasOfficer.waiting(1, "pirates", "lieutenant", "steady", new AtlasOfficer.OfficerSeeds(1L, 2L, 3L, 4L), null)
                .takingCommand(FROM, AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.ARRIVED_HISTORY, FROM));
        assertNull(OfficerText.fledFrom(officer));
        assertFalse(OfficerText.fledFrom(officer, FROM));

        AtlasOfficer fled = officer.losingCommand(AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.FLED_HISTORY, FROM));
        assertEquals(FROM, OfficerText.fledFrom(fled));
        assertTrue(OfficerText.fledFrom(fled, FROM));
        assertFalse(OfficerText.fledFrom(fled, TO));

        AtlasOfficer placedAgain = fled.takingCommand(TO, AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.MOVED_HISTORY, FROM, TO));
        assertNull(OfficerText.fledFrom(placedAgain));
        assertTrue(OfficerText.fledFrom(placedAgain, FROM));
    }

    @Test
    void challengeHistoryEntriesHaveLines() {
        assertTrue(Translation.has(OfficerText.HISTORY_KEY_PREFIX + OfficerTurn.CHALLENGE_WON_HISTORY));
        assertTrue(Translation.has(OfficerText.HISTORY_KEY_PREFIX + OfficerTurn.CHALLENGE_LOST_HISTORY));
        assertTrue(Translation.has(OfficerText.HISTORY_KEY_PREFIX + AtlasTerritoryPlacer.FLED_HISTORY));
    }
}
