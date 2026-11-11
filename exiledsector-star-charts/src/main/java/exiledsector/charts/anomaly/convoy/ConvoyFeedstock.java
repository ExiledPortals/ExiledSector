package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.data.CsvRows;
import exiledsector.items.InfusedFeedstock;
import exiledsector.items.ItemDrops;
import exiledsector.items.MaterialItems;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class ConvoyFeedstock {

    static final String PATH = ExiledSector.configPath("convoy_feedstock.csv");
    static final String FEEDSTOCK_COLUMN = "feedstock";
    static final float SPREAD = 0.15f;
    private static final Logger LOG = Logger.getLogger(ConvoyFeedstock.class);
    private static volatile Map<String, Amount> amounts = Map.of();

    record Amount(float lowestTierAmount, float highestTierAmount) {

        float at(int tier) {
            float perTier = (highestTierAmount - lowestTierAmount) / (ChartTiers.HIGHEST_TIER - ChartTiers.LOWEST_TIER);
            return Math.max(0f, lowestTierAmount + perTier * (ChartTiers.clamp(tier) - ChartTiers.LOWEST_TIER));
        }
    }

    private ConvoyFeedstock() {
    }

    static void load() {
        ModCsv.load(FEEDSTOCK_COLUMN, PATH, LOG, ConvoyFeedstock::register);
    }

    static List<String> register(JSONArray rows) throws JSONException {
        Map<String, Amount> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(PATH);
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String feedstock = ModCsv.text(row, FEEDSTOCK_COLUMN);
                if (feedstock.isEmpty()) {
                    return;
                }
                if (InfusedFeedstock.byItemId(feedstock) == null) {
                    csv.skipRow(feedstock, "not an infused feedstock");
                    return;
                }
                loaded.put(feedstock, new Amount(csv.number(row, "tier_1", 0f, feedstock), csv.number(row, "tier_15", 0f, feedstock)));
            });
        }
        csv.report(LOG);
        amounts = Map.copyOf(loaded);
        return csv.problems();
    }

    static void clear() {
        amounts = Map.of();
    }

    static boolean hasRows() {
        return !amounts.isEmpty();
    }

    static float sharedAmount(String feedstock, int tier) {
        Amount amount = amounts.get(feedstock);
        return amount == null ? 0f : amount.at(tier);
    }

    static Map<String, Float> roll(int tier, float quantityMult, Random random) {
        Map<String, Float> rolled = new LinkedHashMap<>();
        for (InfusedFeedstock feedstock : InfusedFeedstock.values()) {
            float shared = sharedAmount(feedstock.itemId(), tier);
            if (shared > 0f) {
                float spread = 1f + (random.nextFloat() * 2f - 1f) * SPREAD;
                rolled.put(feedstock.itemId(), shared * spread * Math.max(0f, quantityMult));
            }
        }
        return rolled;
    }

    static void add(CargoAPI cargo, Map<String, Float> feedstock, float share, Random random) {
        for (Map.Entry<String, Float> entry : feedstock.entrySet()) {
            MaterialItems.add(cargo, entry.getKey(), ItemDrops.wholeFeedstock(entry.getValue() * share, random));
        }
    }
}
