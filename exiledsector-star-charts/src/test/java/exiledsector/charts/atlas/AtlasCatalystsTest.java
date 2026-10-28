package exiledsector.charts.atlas;

import exiledsector.charts.ChartTables;
import exiledsector.charts.anomaly.ChartAnomalies;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AtlasCatalystsTest {

    @BeforeEach
    void setUp() throws Exception {
        ChartTables.loadAnomalies();
    }

    @AfterEach
    void tearDown() {
        AtlasCatalysts.resetForTests();
        ChartTables.resetAnomalies();
    }

    @Test
    void theShippedTableLoadsEveryCatalystWhoseAnomalyCanBeInsertedAndSkipsTheRest() throws Exception {
        List<String> problems = AtlasCatalysts.register(ChartTables.csvRows(AtlasCatalysts.PATH));

        assertEquals(List.of("exiledSector_convoy_orders skipped: anomaly 'supply_convoy' has no icon"), problems);
        assertEquals(Set.of("spatial_breach", "asteroid_storm", "magnetic_storm", "stellar_flares", "anomalous_debris", "excavation_sites",
                "relay_network"), Set.copyOf(AtlasCatalysts.all().stream().map(AtlasCatalysts.Catalyst::anomalyId).toList()));
        AtlasCatalysts.Catalyst breach = AtlasCatalysts.byItemId("exiledSector_breach_terminus");
        assertNotNull(breach);
        assertEquals(AtlasCatalysts.Family.TERMINUS, breach.family());
        assertEquals(ChartAnomalies.definition("spatial_breach").icon(), breach.iconPath());
        assertEquals(AtlasCatalysts.Family.SURVEY, AtlasCatalysts.byItemId("exiledSector_relay_survey").family());
    }

    @Test
    void aRowWithAnUnknownFamilyIsSkipped() throws Exception {
        JSONArray rows = new JSONArray().put(new JSONObject().put("id", "test_catalyst").put("anomaly", "spatial_breach").put("family", "relic"));

        List<String> problems = AtlasCatalysts.register(rows);

        assertEquals(List.of("test_catalyst skipped: family 'relic' is not terminus, survey or orders"), problems);
        assertNull(AtlasCatalysts.byItemId("test_catalyst"));
    }
}
