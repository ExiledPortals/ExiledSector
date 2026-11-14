package exiledsector.charts.loot;

import exiledsector.charts.ChartTables;
import exiledsector.charts.generation.ChartTiers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartMaterialsTest {

    @AfterEach
    void tearDown() {
        ChartMaterials.clear();
    }

    @Test
    void theShippedAmountsKeepTheirTierOneToFifteenValuesAndRiseAtTierSixteen() throws Exception {
        JSONArray rows = ChartTables.csvRows(ChartMaterials.PATH);
        ChartMaterials.register(rows);
        int checked = 0;
        for (int index = 0; index < rows.length(); index++) {
            JSONObject row = rows.getJSONObject(index);
            String drop = row.optString(ChartMaterials.DROP_COLUMN);
            if (!ChartMaterials.covers(drop)) {
                continue;
            }
            float lowestTierAmount = Float.parseFloat(row.getString("tier_1"));
            float highestDropTierAmount = Float.parseFloat(row.getString("tier_15"));
            float perTier = (highestDropTierAmount - lowestTierAmount) / (ChartTiers.HIGHEST_DROP_TIER - ChartTiers.LOWEST_TIER);
            for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_DROP_TIER; tier++) {
                assertEquals(Math.max(0f, lowestTierAmount + perTier * (tier - ChartTiers.LOWEST_TIER)), ChartMaterials.amount(drop, tier), drop + " tier " + tier);
            }
            assertTrue(ChartMaterials.amount(drop, ChartTiers.HIGHEST_TIER) > ChartMaterials.amount(drop, ChartTiers.HIGHEST_DROP_TIER), drop);
            checked++;
        }
        assertTrue(checked > 0);
    }
}
