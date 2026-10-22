package exiledsector.items;

import exiledlib.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public final class MarketHullItems {

    public static final String DATA_PATH = "data/config/exiledSector/market_hull_items.csv";
    static final String SUBMARKET_COLUMN = "submarket";
    static final String MIN_COUNT_COLUMN = "min_count";
    static final String MAX_COUNT_COLUMN = "max_count";
    static final int EFFECT_COUNT = 1;
    private static final Logger LOG = Logger.getLogger(MarketHullItems.class);
    private static final AtomicReference<Map<String, CountRange>> RANGES_BY_SUBMARKET = new AtomicReference<>(Map.of());

    record CountRange(int min, int max) {
    }

    private MarketHullItems() {
    }

    public static void load() {
        ModCsv.load(SUBMARKET_COLUMN, DATA_PATH, LOG, MarketHullItems::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, CountRange> ranges = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            String submarketId = ModCsv.text(row, SUBMARKET_COLUMN);
            int min = wholeNumber(ModCsv.text(row, MIN_COUNT_COLUMN));
            int max = wholeNumber(ModCsv.text(row, MAX_COUNT_COLUMN));
            if (submarketId.isEmpty() || min < 0 || max < min) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": needs a submarket and counts with 0 <= min_count <= max_count");
            } else {
                ranges.put(submarketId, new CountRange(min, max));
            }
        });
        RANGES_BY_SUBMARKET.set(Collections.unmodifiableMap(ranges));
    }

    private static int wholeNumber(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static boolean stocks(String submarketId) {
        return submarketId != null && RANGES_BY_SUBMARKET.get().containsKey(submarketId);
    }

    public static List<ItemData> roll(String submarketId, Random random) {
        CountRange range = submarketId == null ? null : RANGES_BY_SUBMARKET.get().get(submarketId);
        List<ItemData> stocked = new ArrayList<>();
        if (range == null) {
            return stocked;
        }
        int count = range.min() + random.nextInt(range.max() - range.min() + 1);
        for (int i = 0; i < count; i++) {
            ItemDefinition definition = ItemDrops.pickSectionItemBasic(random, candidate -> ItemRoller.canRollEffectCount(candidate, EFFECT_COUNT));
            if (definition != null) {
                stocked.add(ItemData.rolledWithEffectCount(definition, random.nextLong(), EFFECT_COUNT));
            }
        }
        return stocked;
    }

    public static boolean isStock(ItemData itemData) {
        ItemDefinition definition = itemData == null ? null : itemData.definition();
        return definition != null && definition.type() == ItemType.HULL_ITEM && !definition.unique();
    }

    public static void clear() {
        RANGES_BY_SUBMARKET.set(Map.of());
    }
}
