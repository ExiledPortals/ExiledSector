package exiledsector.skills;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

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

    public static String current(String nodeId) {
        return current(nodeId, BY_OLD_ID.get(), id -> SkillTree.getDeclared(id) != null);
    }

    static String current(String nodeId, Map<String, String> replacements, Predicate<String> inTree) {
        if (nodeId == null || inTree.test(nodeId)) {
            return nodeId;
        }
        String candidate = nodeId;
        for (int hop = 0; hop < replacements.size(); hop++) {
            candidate = replacements.get(candidate);
            if (candidate == null) {
                return nodeId;
            }
            if (inTree.test(candidate)) {
                return candidate;
            }
        }
        return nodeId;
    }

    public static void clear() {
        BY_OLD_ID.set(Map.of());
    }
}
