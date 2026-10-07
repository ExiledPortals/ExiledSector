package exiledsector.socketables;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class SocketCraftingCosts {

    static final String DATA_PATH = "data/config/exiledSector/socketable_crafting.csv";
    public static final String COMMON_SUBROUTINE = "common_subroutine";
    private static final Logger LOG = Logger.getLogger(SocketCraftingCosts.class);
    private static final AtomicReference<Map<String, Integer>> COSTS = new AtomicReference<>(Map.of());

    private SocketCraftingCosts() {
    }

    public static void load() {
        ModCsv.load("item", DATA_PATH, LOG, SocketCraftingCosts::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, Integer> loaded = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String item = ModCsv.text(row, "item");
            String cost = ModCsv.text(row, "partsCost");
            if (item.isEmpty() || cost.isEmpty()) {
                return;
            }
            try {
                loaded.put(item, Math.max(0, Integer.parseInt(cost)));
            } catch (NumberFormatException e) {
                LOG.error("Skipping the crafting cost for " + item + ": " + e.getMessage());
            }
        });
        COSTS.set(Map.copyOf(loaded));
    }

    public static int cost(String item, int fallback) {
        Integer cost = COSTS.get().get(item);
        return cost == null ? fallback : cost;
    }

    public static void clear() {
        COSTS.set(Map.of());
    }
}
