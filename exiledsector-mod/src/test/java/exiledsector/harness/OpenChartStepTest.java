package exiledsector.harness;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenChartStepTest {

    @Test
    void everyChartParameterIsRead() throws JSONException {
        OpenChartStep.Options options = OpenChartStep.Options.parse(new JSONObject("""
                {"step": "openChart", "seed": 9, "tier": 7, "anomalies": ["excavation_sites", "dense_nebula"], "region": "abyssal_rift",
                 "layout": "ring", "anomalyData": {"relay_network": "stage 2"}, "atlasNodes": ["twin_shafts"]}"""), 1L);

        assertEquals(new OpenChartStep.Options(9L, 7, List.of("excavation_sites", "dense_nebula"), "abyssal_rift", "ring",
                Map.of("relay_network", "stage 2"), List.of("twin_shafts")), options);
        assertEquals("openChart seed 9 tier 7 [excavation_sites, dense_nebula] in abyssal_rift as ring with nodes [twin_shafts]",
                new OpenChartStep(options).name());
    }

    @Test
    void aBareChartUsesTheCaseSeedAtTheLowestTierWithNothingForced() throws JSONException {
        OpenChartStep.Options options = OpenChartStep.Options.parse(new JSONObject("{\"step\": \"openChart\", \"region\": \" \"}"), 42L);

        assertEquals(42L, options.seed());
        assertEquals(List.of(), options.anomalies());
        assertNull(options.region());
        assertEquals("openChart seed 42", new OpenChartStep(options).name());
    }
}
