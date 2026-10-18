package exiledsector.items;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class ItemIdAliases {

    static final String DATA_PATH = "data/config/exiledSector/item_id_aliases.csv";
    private static final String ALIAS_COLUMN = "alias";
    private static final Logger LOG = Logger.getLogger(ItemIdAliases.class);
    private static final AtomicReference<Map<String, String>> CURRENT_IDS_BY_ALIAS = new AtomicReference<>(Map.of());

    private ItemIdAliases() {
    }

    public static void load() {
        ModCsv.load(ALIAS_COLUMN, DATA_PATH, LOG, ItemIdAliases::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new HashMap<>();
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String alias = ModCsv.text(row, "alias");
                String currentId = ModCsv.text(row, "id");
                if (alias.isEmpty() || currentId.isEmpty()) {
                    return;
                }
                if (loaded.putIfAbsent(alias, currentId) != null) {
                    LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + alias + " is listed more than once");
                }
            });
        }
        CURRENT_IDS_BY_ALIAS.set(Map.copyOf(loaded));
    }

    public static void clear() {
        CURRENT_IDS_BY_ALIAS.set(Map.of());
    }

    static String currentId(String definitionId) {
        if (definitionId == null) {
            return null;
        }
        String currentId = CURRENT_IDS_BY_ALIAS.get().get(definitionId);
        return currentId == null ? definitionId : currentId;
    }
}
