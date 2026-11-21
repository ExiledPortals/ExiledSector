package exiledsector.ui.chart;

import exiledsector.charts.atlas.AtlasAnomalyChance;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasMemory;
import exiledsector.charts.atlas.AtlasRegion;
import exiledsector.charts.atlas.AtlasRules;
import exiledsector.charts.atlas.AtlasTerritoryPlacer;
import exiledsector.charts.atlas.AtlasUnlocks;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerText;
import exiledsector.i18n.Translation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExiledSectorOfficerGreetingTest {

    private static final AtlasCell FROM = new AtlasCell(2, 5);
    private static final AtlasCell TO = new AtlasCell(-4, 1);
    private static final AtlasGenerator GENERATOR = new AtlasGenerator(3L, new AtlasRules(
            List.of(new AtlasRegion(AtlasRegion.NORMAL, 5f, 0, "", "", Map.of(), Map.of(), Map.of())), Map.of("compact", 1f),
            Map.of("pirates", 1f), AtlasAnomalyChance.NEVER, AtlasAnomalyChance.NEVER, List.of(), List.of()), AtlasUnlocks.NONE, AtlasMemory.NONE);

    private static AtlasOfficer officer() {
        return AtlasOfficer.waiting(1, "pirates", "captain", "steady", new AtlasOfficer.OfficerSeeds(1L, 2L, 3L, 4L), null);
    }

    @Test
    void aMovedOfficerRemembersWhereItWasDrivenOutOf() {
        AtlasOfficer officer = officer().takingCommand(TO, AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.MOVED_HISTORY, FROM, TO));

        assertEquals(Translation.msg("chart.officer.greeting.moved").arg("title", OfficerText.title(officer))
                        .arg("from", OfficerText.regionOf(GENERATOR, FROM)).arg("region", OfficerText.regionOf(GENERATOR, TO)).text(),
                ExiledSectorOfficerGreeting.greeting(GENERATOR, officer));
    }

    @Test
    void anArrivedOfficerClaimsItsRegion() {
        AtlasOfficer officer = officer().takingCommand(TO, AtlasTerritoryPlacer.history(AtlasTerritoryPlacer.ARRIVED_HISTORY, TO));

        assertEquals(Translation.msg("chart.officer.greeting.arrived").arg("title", OfficerText.title(officer))
                        .arg("region", OfficerText.regionOf(GENERATOR, TO)).text(),
                ExiledSectorOfficerGreeting.greeting(GENERATOR, officer));
    }

    @Test
    void anOfficerWithoutAGreetingForItsLatestEventUsesTheDefault() {
        String expected = Translation.msg("chart.officer.greeting.default").arg("title", OfficerText.title(officer())).text();

        assertEquals(expected, ExiledSectorOfficerGreeting.greeting(GENERATOR, officer()));
        assertEquals(expected, ExiledSectorOfficerGreeting.greeting(GENERATOR,
                officer().takingCommand(TO, AtlasTerritoryPlacer.history("rumoured", TO))));
    }
}
