package exiledsector.skills;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class NodeReplacements {

    static final String DATA_PATH = "data/config/exiledSector/node_replacements.csv";
    private static final Logger LOG = Logger.getLogger(NodeReplacements.class);
    private static final AtomicReference<Map<String, String>> BY_OLD_ID = new AtomicReference<>(Map.of());

    private NodeReplacements() {
    }

    public static void load() {
        ModCsv.load("old", DATA_PATH, LOG, NodeReplacements::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new LinkedHashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String oldId = ModCsv.text(row, "old");
            String newId = ModCsv.text(row, "new");
            if (!oldId.isEmpty() && !newId.isEmpty()) {
                loaded.put(oldId, newId);
            }
        });
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
