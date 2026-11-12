package exiledsector.charts.officer;

import exiledsector.charts.atlas.AtlasAnomalyChance;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasMemory;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasTerritoryPlacer;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.i18n.Translation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
