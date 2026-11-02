package exiledsector.items;

import exiledsector.ExiledSector;
import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class MaterialIdAliases {

    static final String DATA_FILE = "material_id_aliases.csv";
    private static final String ALIAS_COLUMN = "alias";
    private static final Logger LOG = Logger.getLogger(MaterialIdAliases.class);
    private static final AtomicReference<Map<String, String>> CURRENT_IDS_BY_ALIAS = new AtomicReference<>(Map.of());

    private MaterialIdAliases() {
    }

    public static String dataPath() {
        return ExiledSector.configPath(DATA_FILE);
    }

    public static void load() {
        ModCsv.load(ALIAS_COLUMN, dataPath(), LOG, MaterialIdAliases::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new LinkedHashMap<>();
        if (rows != null) {
            ModCsv.forEach(rows, (index, row) -> {
                String alias = ModCsv.text(row, "alias");
                String currentId = ModCsv.text(row, "id");
                if (alias.isEmpty() || currentId.isEmpty()) {
                    return;
                }
                if (loaded.putIfAbsent(alias, currentId) != null) {
                    LOG.error("Skipping row " + (index + 1) + " of " + dataPath() + ": " + alias + " is listed more than once");
                }
            });
        }
        CURRENT_IDS_BY_ALIAS.set(Map.copyOf(loaded));
    }

    public static void clear() {
        CURRENT_IDS_BY_ALIAS.set(Map.of());
    }

    public static Map<String, String> currentIdsByAlias() {
        return CURRENT_IDS_BY_ALIAS.get();
    }

    public static String currentId(String itemId) {
        return itemId == null ? null : CURRENT_IDS_BY_ALIAS.get().getOrDefault(itemId, itemId);
    }
}
