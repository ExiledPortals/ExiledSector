package exiledsector.items;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import exiledsector.data.CsvRows;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ItemCraftingCosts {

    static final String DATA_FILE = "item_crafting.csv";
    private static final Logger LOG = Logger.getLogger(ItemCraftingCosts.class);
    private static final AtomicReference<Map<String, Integer>> COSTS = new AtomicReference<>(Map.of());

    private ItemCraftingCosts() {
    }

    public static String dataPath() {
        return ExiledSector.configPath(DATA_FILE);
    }

    public static void load() {
        ModCsv.load("item", dataPath(), LOG, ItemCraftingCosts::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, Integer> loaded = new HashMap<>();
        CsvRows.forEachIdRow(rows, "item", (itemId, row) -> {
            String costText = ModCsv.text(row, "partsCost");
            if (costText.isEmpty()) {
                return;
            }
            try {
                loaded.put(itemId, Math.max(0, Integer.parseInt(costText)));
            } catch (NumberFormatException e) {
                LOG.error("Skipping the crafting cost for " + itemId + ": " + e.getMessage());
            }
        });
        COSTS.set(Map.copyOf(loaded));
    }

    public static int cost(String itemId, int fallback) {
        Integer configuredCost = COSTS.get().get(itemId);
        return configuredCost == null ? fallback : configuredCost;
    }

    public static void clear() {
        COSTS.set(Map.of());
    }
}
