package exiledsector.charts.loot;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.data.CsvRows;
import exiledsector.items.ItemDrops;
import exiledsector.loot.BattleMaterials;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class ChartMaterials {

    public static final String PATH = ExiledSector.configPath("chart_materials.csv");
    static final String DROP_COLUMN = "drop";
    private static final Logger LOG = Logger.getLogger(ChartMaterials.class);
    private static volatile Map<String, Amount> amounts = Map.of();

    record Amount(float lowestTierAmount, float highestTierAmount) {

        float at(int tier) {
            float perTier = (highestTierAmount - lowestTierAmount) / (ChartTiers.HIGHEST_TIER - ChartTiers.LOWEST_TIER);
            return Math.max(0f, lowestTierAmount + perTier * (tier - ChartTiers.LOWEST_TIER));
        }
    }

    private ChartMaterials() {
    }

    public static void load() {
        ModCsv.load(DROP_COLUMN, PATH, LOG, ChartMaterials::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, Amount> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String drop = ModCsv.text(row, DROP_COLUMN);
                if (drop.isEmpty()) {
                    return;
                }
                if (!BattleMaterials.isMaterial(drop)) {
                    csv.skipRow(drop, "not a nanoswarm or inhibitor");
                    return;
                }
                loaded.put(drop, new Amount(csv.number(row, "tier_1", 0f, drop), csv.number(row, "tier_15", 0f, drop)));
            });
        }
        csv.report(LOG);
        amounts = Map.copyOf(loaded);
    }

    public static void clear() {
        amounts = Map.of();
    }

    public static boolean covers(String drop) {
        return amounts.containsKey(drop);
    }

    static float amount(String drop, int tier) {
        Amount amount = amounts.get(drop);
        return amount == null ? 0f : amount.at(tier);
    }

    static Map<String, Integer> roll(int tier, float chartShare, Random random) {
        Map<String, Integer> materials = new LinkedHashMap<>();
        amounts.forEach((drop, amount) -> {
            int count = ItemDrops.wholeFeedstock(amount.at(tier) * Math.max(0f, chartShare), random);
            if (count > 0) {
                materials.put(drop, count);
            }
        });
        return materials;
    }
}
