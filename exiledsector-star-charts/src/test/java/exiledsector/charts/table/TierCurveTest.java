package exiledsector.charts.table;

import exiledsector.charts.generation.ChartTiers;
import exiledsector.data.CsvRows;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TierCurveTest {

    private static TierCurve parsed(JSONObject row) {
        return TierCurve.parse(new CsvRows("test.csv"), row, "row");
    }

    @Test
    void tiersOneToFifteenRunLinearlyBetweenTheTwoColumns() throws Exception {
        TierCurve curve = parsed(new JSONObject().put("tier_1", "150").put("tier_15", "1000"));

        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_DROP_TIER; tier++) {
            float before = Math.max(0f, 150f + (1000f - 150f) / (ChartTiers.HIGHEST_DROP_TIER - ChartTiers.LOWEST_TIER) * (tier - ChartTiers.LOWEST_TIER));
            assertEquals(before, curve.at(tier), "tier " + tier);
        }
        assertEquals(575f, curve.at(8), 0.001f);
    }

    @Test
    void tierSixteenContinuesTheLineAndTiersOutsideTheRangeClampToItsEnds() throws Exception {
        TierCurve curve = parsed(new JSONObject().put("tier_1", "150").put("tier_15", "1000"));

        assertTrue(curve.at(ChartTiers.HIGHEST_TIER) > curve.at(ChartTiers.HIGHEST_DROP_TIER));
        assertEquals(150f + 850f / 14f * 15f, curve.at(ChartTiers.HIGHEST_TIER), 0.001f);
        assertEquals(curve.at(ChartTiers.HIGHEST_TIER), curve.at(40));
        assertEquals(150f, curve.at(0));
    }

    @Test
    void aTierSixteenColumnOverridesTheLineAndAmountsNeverGoNegative() throws Exception {
        assertEquals(42f, parsed(new JSONObject().put("tier_1", "1").put("tier_15", "2").put("tier_16", "42")).at(ChartTiers.HIGHEST_TIER));
        assertEquals(0f, parsed(new JSONObject().put("tier_1", "14").put("tier_15", "0")).at(ChartTiers.HIGHEST_TIER));
    }
}
