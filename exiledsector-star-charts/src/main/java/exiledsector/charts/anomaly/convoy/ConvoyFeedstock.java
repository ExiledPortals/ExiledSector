package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.charts.table.TierCurve;
import exiledsector.data.CsvRows;
import exiledsector.data.RandomRounding;
import exiledsector.items.InfusedFeedstock;
import exiledsector.items.MaterialItems;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class ConvoyFeedstock {

    static final String PATH = ExiledSector.configPath("convoy_feedstock.csv");
    static final String FEEDSTOCK_COLUMN = "feedstock";
    static final float SPREAD = 0.15f;
    private static final Logger LOG = Logger.getLogger(ConvoyFeedstock.class);
    private static volatile Map<String, TierCurve> amounts = Map.of();

    private ConvoyFeedstock() {
    }

    static void load() {
        ModCsv.load(FEEDSTOCK_COLUMN, PATH, LOG, ConvoyFeedstock::register);
    }

    static List<String> register(JSONArray rows) throws JSONException {
        Map<String, TierCurve> loaded = new LinkedHashMap<>();
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
                loaded.put(feedstock, TierCurve.parse(csv, row, feedstock));
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
        TierCurve amount = amounts.get(feedstock);
        return amount == null ? 0f : amount.at(tier);
    }

    static Map<String, Float> roll(int tier, float quantityMult, FeedstockOdds odds, Random random) {
        List<InfusedFeedstock> carried = new ArrayList<>();
        float total = 0f;
        for (InfusedFeedstock feedstock : InfusedFeedstock.values()) {
            float shared = sharedAmount(feedstock.itemId(), tier);
            if (shared > 0f) {
                carried.add(feedstock);
                total += shared;
            }
        }
        Map<String, Float> rolled = new LinkedHashMap<>();
        if (carried.isEmpty()) {
            return rolled;
        }
        InfusedFeedstock kind = odds.mixed() ? null : pickKind(carried, odds, random);
        float spread = 1f + (random.nextFloat() * 2f - 1f) * SPREAD;
        float doubled = random.nextFloat() < odds.doubleChance() ? 2f : 1f;
        float amount = total * spread * doubled * Math.max(0f, quantityMult);
        if (kind != null) {
            rolled.put(kind.itemId(), amount);
            return rolled;
        }
        for (InfusedFeedstock feedstock : carried) {
            rolled.put(feedstock.itemId(), amount / carried.size());
        }
        return rolled;
    }

    static InfusedFeedstock pickKind(List<InfusedFeedstock> carried, FeedstockOdds odds, Random random) {
        float totalWeight = 0f;
        for (InfusedFeedstock feedstock : carried) {
            totalWeight += odds.weight(feedstock);
        }
        if (totalWeight <= 0f) {
            return carried.get(random.nextInt(carried.size()));
        }
        float roll = random.nextFloat() * totalWeight;
        InfusedFeedstock lastLikely = null;
        for (InfusedFeedstock feedstock : carried) {
            float weight = odds.weight(feedstock);
            if (weight <= 0f) {
                continue;
            }
            lastLikely = feedstock;
            roll -= weight;
            if (roll < 0f) {
                return feedstock;
            }
        }
        return lastLikely;
    }

    static float total(Map<String, Float> feedstock) {
        float total = 0f;
        for (float amount : feedstock.values()) {
            total += amount;
        }
        return total;
    }

    static void add(CargoAPI cargo, Map<String, Float> feedstock, float share, Random random) {
        for (Map.Entry<String, Float> entry : feedstock.entrySet()) {
            MaterialItems.add(cargo, entry.getKey(), RandomRounding.round(entry.getValue() * share, random));
        }
    }
}
