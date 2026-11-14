package exiledsector.items;

import exiledsector.ExiledSector;
import exiledsector.data.IdAliases;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Map;

public final class MaterialIdAliases {

    static final String DATA_FILE = "material_id_aliases.csv";
    private static final IdAliases ALIASES = new IdAliases(ExiledSector.configPath(DATA_FILE), "alias", "id", Logger.getLogger(MaterialIdAliases.class));

    private MaterialIdAliases() {
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

    public static Map<String, String> currentIdsByAlias() {
        return ALIASES.currentIdsByAlias();
    }

    public static String currentId(String itemId) {
        return ALIASES.currentId(itemId);
    }
}
