package exiledsector.items.drop;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import exiledsector.data.RandomRounding;
import exiledsector.data.WeightedPick;
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
    public static final float BASE_RARITY = 1f;
    public static final Predicate<String> ANY_ITEM = itemId -> true;
    public static final Predicate<String> OUTSIDE_CHARTS = itemId -> !chartOnly(itemId);
    public static final float RARITY_FLATTENING = 0.4f;
    static final String ITEM_COLUMN = "item";
    private static final float LOWEST_RARITY = 0.01f;
    private static final Logger LOG = Logger.getLogger(CraftingDrops.class);
    private static final AtomicReference<Table> TABLE = new AtomicReference<>(Table.EMPTY);

    public record Drop(String itemId, float weight, boolean chartOnly) {

        public Drop {
            weight = Math.max(0f, weight);
        }

        float weightAtRarity(float rarity, float tableWeight) {
            if (rarity == BASE_RARITY || !(weight > 0f) || !(tableWeight > 0f)) {
                return weight;
            }
            double tableShare = Math.min(1f, weight / tableWeight);
            double exponent = Math.pow(Math.max(LOWEST_RARITY, rarity), -RARITY_FLATTENING);
            return (float) (tableWeight * Math.pow(tableShare, exponent));
        }
    }

    private record Table(Map<String, Drop> drops, float totalWeight) {

        static final Table EMPTY = new Table(Map.of(), 0f);

        static Table of(Map<String, Drop> drops) {
            float totalWeight = 0f;
            for (Drop drop : drops.values()) {
                totalWeight += drop.weight();
            }
            return new Table(Collections.unmodifiableMap(drops), totalWeight);
        }

        Drop get(String itemId) {
            return itemId == null ? null : drops.get(itemId);
        }

        float weightAtRarity(Drop drop, float rarity) {
            return drop.weightAtRarity(rarity, totalWeight);
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
        TABLE.set(Table.of(loaded));
    }

    public static void clear() {
        TABLE.set(Table.EMPTY);
    }

    public static List<Drop> all() {
        return List.copyOf(TABLE.get().drops().values());
    }

    public static boolean covers(String itemId) {
        return TABLE.get().get(itemId) != null;
    }

    public static float weight(String itemId) {
        Drop drop = TABLE.get().get(itemId);
        return drop == null ? 0f : drop.weight();
    }

    public static boolean chartOnly(String itemId) {
        Drop drop = TABLE.get().get(itemId);
        return drop != null && drop.chartOnly();
    }

    public static float weightAtRarity(String itemId, LootMultipliers multipliers) {
        return weightAtRarity(itemId, multipliers.craftingRarity(DropType.forCraftingItem(itemId)));
    }

    public static float weightAtRarity(String itemId, float rarity) {
        Table table = TABLE.get();
        Drop drop = table.get(itemId);
        return drop == null ? 0f : table.weightAtRarity(drop, rarity);
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
        Table table = TABLE.get();
        List<WeightedDrop> candidates = new ArrayList<>();
        for (Drop drop : table.drops().values()) {
            if (drop.weight() > 0f && allowedItems.test(drop.itemId())) {
                DropType subcategory = DropType.forCraftingItem(drop.itemId());
                candidates.add(new WeightedDrop(drop.itemId(), table.weightAtRarity(drop, multipliers.craftingRarity(subcategory)),
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
