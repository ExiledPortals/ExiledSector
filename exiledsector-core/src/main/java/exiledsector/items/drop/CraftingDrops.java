package exiledsector.items.drop;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import exiledsector.data.RandomRounding;
import exiledsector.data.WeightedPick;
import exiledsector.items.crafting.material.MaterialItems;
import exiledsector.items.crafting.material.MaterialTier;
import exiledsector.loot.DropType;
import exiledsector.loot.LootMultipliers;
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

    public static float shiftedWeight(String itemId, LootMultipliers multipliers) {
        Drop drop = itemId == null ? null : DROPS.get().get(itemId);
        return drop == null ? 0f : drop.shiftedWeight(multipliers.craftingBandShift(DropType.forCraftingItem(itemId)));
    }

    public static Map<String, Integer> roll(float baseCount, Predicate<String> allowedItems, LootMultipliers multipliers, Random random) {
        Map<String, Integer> dropped = new LinkedHashMap<>();
        rollInto(baseCount, allowedItems, multipliers, random, dropped);
        return dropped;
    }

    public static void rollInto(float baseCount, Predicate<String> allowedItems, LootMultipliers multipliers, Random random,
                                Map<String, Integer> dropped) {
        if (!(baseCount > 0f)) {
            return;
        }
        Split split = split(allowedItems, multipliers);
        split.rollInto(baseCount * split.meanQuantity(), random, dropped);
    }

    public static Split split(Predicate<String> allowedItems, LootMultipliers multipliers) {
        List<WeightedDrop> candidates = new ArrayList<>();
        for (Drop drop : DROPS.get().values()) {
            if (drop.weight() > 0f && allowedItems.test(drop.itemId())) {
                DropType subcategory = DropType.forCraftingItem(drop.itemId());
                candidates.add(new WeightedDrop(drop.itemId(), drop.shiftedWeight(multipliers.craftingBandShift(subcategory)),
                        multipliers.quantity(subcategory)));
            }
        }
        return new Split(List.copyOf(candidates), meanQuantity(candidates));
    }

    private static float meanQuantity(List<WeightedDrop> candidates) {
        if (candidates.isEmpty()) {
            return 0f;
        }
        float firstQuantity = candidates.get(0).quantity();
        if (candidates.stream().allMatch(candidate -> candidate.quantity() == firstQuantity)) {
            return firstQuantity;
        }
        float weightTotal = 0f;
        float weightedQuantityTotal = 0f;
        for (WeightedDrop candidate : candidates) {
            weightTotal += candidate.weight();
            weightedQuantityTotal += candidate.weight() * candidate.quantity();
        }
        return weightTotal > 0f ? weightedQuantityTotal / weightTotal : 0f;
    }

    public record WeightedDrop(String itemId, float weight, float quantity) {
    }

    public record Split(List<WeightedDrop> candidates, float meanQuantity) {

        public void rollInto(float expectedCount, Random random, Map<String, Integer> dropped) {
            if (!(expectedCount > 0f) || !(meanQuantity > 0f) || candidates.isEmpty()) {
                return;
            }
            int count = RandomRounding.round(expectedCount, random);
            for (int dropIndex = 0; dropIndex < count; dropIndex++) {
                WeightedDrop picked = WeightedPick.pick(candidates, this::pickWeight, random);
                dropped.merge(picked.itemId(), 1, Integer::sum);
            }
        }

        public Map<String, Integer> roll(float expectedCount, Random random) {
            Map<String, Integer> dropped = new LinkedHashMap<>();
            rollInto(expectedCount, random, dropped);
            return dropped;
        }

        private float pickWeight(WeightedDrop candidate) {
            return candidate.weight() * (candidate.quantity() / meanQuantity);
        }
    }
}
