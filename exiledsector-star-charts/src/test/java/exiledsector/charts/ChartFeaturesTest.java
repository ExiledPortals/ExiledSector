package exiledsector.charts;

import exiledsector.charts.feature.ChartFeature;
import exiledsector.charts.feature.RingedGiant;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChartFeaturesTest {

    @AfterEach
    void tearDown() {
        ChartFeatures.resetForTests();
    }

    @Test
    void theShippedFeatureTableListsEveryBuiltFeatureWithoutProblems() throws Exception {
        assertEquals(List.of(), ChartFeatures.register(ChartTables.csvRows(ChartFeatures.PATH)));

        for (ChartFeature feature : ChartFeatures.BUILT_IN) {
            assertNotNull(ChartFeatures.byId(feature.id()), feature.id());
        }
    }

    @Test
    void aFeatureIsOnlyUsedWhileItHasARowAndRowsWithoutAClassAreSkipped() throws Exception {
        List<String> problems = ChartFeatures.register(new JSONArray().put(new JSONObject().put("id", "crystal_spire")));

        assertEquals(1, problems.size());
        assertNull(ChartFeatures.byId(RingedGiant.ID));
        ChartFeatures.register(new JSONArray().put(new JSONObject().put("id", RingedGiant.ID)));
        assertSame(RingedGiant.INSTANCE, ChartFeatures.byId(RingedGiant.ID));
    }
}
