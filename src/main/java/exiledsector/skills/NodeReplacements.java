package exiledsector.skills;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static exiledsector.ExiledSectorModPlugin.MOD_ID;

public final class NodeReplacements {

    static final String DATA_PATH = "data/config/exiledSector/node_replacements.csv";
    private static final Logger LOG = Logger.getLogger(NodeReplacements.class);
    private static final AtomicReference<Map<String, String>> BY_OLD_ID = new AtomicReference<>(Map.of());

    private NodeReplacements() {
    }

    public static void load() {
        try {
            register(Global.getSettings().getMergedSpreadsheetDataForMod("old", DATA_PATH, MOD_ID));
        } catch (IOException | JSONException e) {
            LOG.error("Failed to load " + DATA_PATH, e);
        }
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new LinkedHashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String oldId = row.optString("old", "").trim();
            String newId = row.optString("new", "").trim();
            if (!oldId.isEmpty() && !newId.isEmpty()) {
                loaded.put(oldId, newId);
            }
        }
        BY_OLD_ID.set(Map.copyOf(loaded));
    }

    public static Map<String, String> all() {
        return BY_OLD_ID.get();
    }

    public static String resolve(String nodeId) {
        String replacement = BY_OLD_ID.get().get(nodeId);
        return replacement != null && SkillTree.get(nodeId) == null ? replacement : nodeId;
    }

    public static void clear() {
        BY_OLD_ID.set(Map.of());
    }
}
