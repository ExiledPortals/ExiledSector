package exiledsector.charts;

import exiledsector.charts.phenomenon.ChartPhenomenon;
import exiledsector.charts.phenomenon.StarPhenomena;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChartPhenomenaTest {

    @AfterEach
    void tearDown() {
        ChartPhenomena.resetForTests();
    }

    @Test
    void theShippedTableListsEveryPhenomenonWithTheRightKind() throws Exception {
        assertEquals(List.of(), ChartPhenomena.register(ChartTables.csvRows(ChartPhenomena.PATH)));

        for (ChartPhenomenon phenomenon : ChartPhenomena.BUILT_IN) {
            assertNotNull(ChartPhenomena.byId(phenomenon.id()), phenomenon.id());
            assertEquals(phenomenon.kind(), ChartPhenomena.definition(phenomenon.id()).kind());
        }
    }

    @Test
    void unknownRowsAreSkippedAndAWrongKindIsCorrected() throws Exception {
        List<String> problems = ChartPhenomena.register(new JSONArray()
                .put(new JSONObject().put("id", "ion_storm"))
                .put(new JSONObject().put("id", "black_hole").put("kind", "overlay")));

        assertEquals(2, problems.size());
        assertNull(ChartPhenomena.byId("ion_storm"));
        assertEquals(ChartPhenomenon.Kind.STAR, ChartPhenomena.definition("black_hole").kind());
    }

    @Test
    void aChartsChanceLineCanAddAnOverlayPhenomenonButNeverAStarOne() throws Exception {
        ChartPhenomena.register(ChartTables.csvRows(ChartPhenomena.PATH));
        ChartGenContext storm = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_ASTEROID_STORM_CHANCE, 100f)));

        for (long seed = 0; seed < 10; seed++) {
            assertEquals("asteroid_storm", ChartPhenomena.rolledOverlay(seed, 5, storm));
            assertEquals("", ChartPhenomena.rolledOverlay(seed, 5, ChartGenContext.none()));
        }
    }

    @Test
    void aPhenomenonIsOnlyAvailableInsideItsTiersAndWithItsMod() {
        ChartPhenomena.useForTests(List.of(new ChartPhenomena.Definition("black_hole", 1f, 5, 10, "", "", ChartPhenomenon.Kind.STAR),
                new ChartPhenomena.Definition("neutron_star", 1f, 1, 15, "a_mod_that_is_not_here", "", ChartPhenomenon.Kind.STAR)));

        assertNull(ChartPhenomena.availableAt("black_hole", 4));
        assertSame(StarPhenomena.BLACK_HOLE, ChartPhenomena.availableAt("black_hole", 7));
        assertNull(ChartPhenomena.availableAt("neutron_star", 7));
        assertNull(ChartPhenomena.availableAt("", 7));
    }
}
