package exiledsector.items;

import exiledsector.ExiledSector;
import exiledsector.data.IdAliases;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

public final class ItemIdAliases {

    static final String DATA_FILE = "item_id_aliases.csv";
    private static final IdAliases ALIASES = new IdAliases(ExiledSector.configPath(DATA_FILE), "alias", "id", Logger.getLogger(ItemIdAliases.class));

    private ItemIdAliases() {
    }

    public static String dataPath() {
        return ALIASES.path();
    }

    public static void load() {
        ALIASES.load();
    }

    public static void register(JSONArray rows) throws JSONException {
        ALIASES.register(rows);
    }

    public static void clear() {
        ALIASES.clear();
    }

    public static String currentId(String definitionId) {
        return ALIASES.currentId(definitionId);
    }
}
