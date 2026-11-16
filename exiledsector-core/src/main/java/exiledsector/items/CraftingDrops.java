package exiledsector.items;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import exiledsector.data.RandomRounding;
import exiledsector.data.WeightedPick;
import exiledsector.skills.skilleffect.logistics.FoundItemQuantity;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public final class CraftingDrops {

    public static final String DATA_PATH = ExiledSector.configPath("crafting_drops.csv");
    public static final float NO_BAND_SHIFT = 1f;
    public static final Predicate<String> ANY_ITEM = itemId -> true;
    public static final Predicate<String> OUTSIDE_CHARTS = itemId -> !chartOnly(itemId);
    static final String ITEM_COLUMN = "item";
    private static final Logger LOG = Logger.getLogger(CraftingDrops.class);
    private static final AtomicReference<Map<String, Drop>> DROPS = new AtomicReference<>(Map.of());

    public record Drop(String itemId, float weight, boolean chartOnly) {

        public Drop {
            weight = Math.max(0f, weight);
        }

        float shiftedWeight(float bandShift) {
            return MaterialItems.tier(itemId) == MaterialTier.COMMON ? weight : weight * Math.max(0f, bandShift);
        }
    }

    private CraftingDrops() {
    }

    public static void load() {
        ModCsv.load(ITEM_COLUMN, DATA_PATH, LOG, CraftingDrops::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, Drop> loaded = new LinkedHashMap<>();
        CsvRows csv = new CsvRows(DATA_PATH);
        CsvRows.forEachIdRow(rows, ITEM_COLUMN, (itemId, row) ->
                loaded.put(itemId, new Drop(itemId, csv.number(row, "weight", 0f, itemId), csv.flag(row, "chart_only", false, itemId))));
        csv.report(LOG);
        DROPS.set(Collections.unmodifiableMap(loaded));
    }

    public static void clear() {
        DROPS.set(Map.of());
    }

    public static List<Drop> all() {
        return List.copyOf(DROPS.get().values());
    }

    public static boolean covers(String itemId) {
        return itemId != null && DROPS.get().containsKey(itemId);
    }

    public static float weight(String itemId) {
        Drop drop = itemId == null ? null : DROPS.get().get(itemId);
        return drop == null ? 0f : drop.weight();
    }

    public static boolean chartOnly(String itemId) {
        Drop drop = itemId == null ? null : DROPS.get().get(itemId);
        return drop != null && drop.chartOnly();
    }

    public static float shiftedWeight(String itemId, float bandShift) {
        Drop drop = itemId == null ? null : DROPS.get().get(itemId);
        return drop == null ? 0f : drop.shiftedWeight(bandShift);
    }

    public static Map<String, Integer> roll(float expectedCount, Predicate<String> allowedItems, float bandShift, Random random) {
        Map<String, Integer> dropped = new LinkedHashMap<>();
        rollInto(expectedCount, allowedItems, bandShift, random, dropped);
        return dropped;
    }

    public static Map<String, Integer> rollFoundByPlayer(float expectedCount, Predicate<String> allowedItems, float bandShift, Random random) {
        return roll(expectedCount * FoundItemQuantity.craftingItemMult(), allowedItems, bandShift, random);
    }

    public static void rollFoundByPlayerInto(float expectedCount, Predicate<String> allowedItems, float bandShift, Random random,
                                             Map<String, Integer> dropped) {
        rollInto(expectedCount * FoundItemQuantity.craftingItemMult(), allowedItems, bandShift, random, dropped);
    }

    public static void rollInto(float expectedCount, Predicate<String> allowedItems, float bandShift, Random random,
                                Map<String, Integer> dropped) {
        if (!(expectedCount > 0f)) {
            return;
        }
        List<Drop> candidates = DROPS.get().values().stream()
                .filter(drop -> drop.weight() > 0f && allowedItems.test(drop.itemId())).toList();
        if (candidates.isEmpty()) {
            return;
        }
        int count = RandomRounding.round(expectedCount, random);
        for (int dropIndex = 0; dropIndex < count; dropIndex++) {
            Drop picked = WeightedPick.pick(candidates, candidate -> candidate.shiftedWeight(bandShift), random);
            dropped.merge(picked.itemId(), 1, Integer::sum);
        }
    }
}
