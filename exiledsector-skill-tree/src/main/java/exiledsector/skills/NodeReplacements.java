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
    static final String NODE_RENAMES_PATH = "data/config/exiledSector/node_id_renames.csv";
    static final String TYPE_RENAMES_PATH = "data/config/exiledSector/type_id_renames.csv";
    private static final Logger LOG = Logger.getLogger(NodeReplacements.class);
    private static final AtomicReference<Lookup> LOOKUP = new AtomicReference<>(Lookup.EMPTY);

    private record Lookup(Map<String, String> replacedNodes, Map<String, String> renamedNodes, Map<String, String> renamedTypes,
                          Map<String, String> allNodes) {

        static final Lookup EMPTY = new Lookup(Map.of(), Map.of(), Map.of(), Map.of());

        Lookup withNodes(Map<String, String> replaced, Map<String, String> renamed) {
            Map<String, String> merged = new LinkedHashMap<>(replaced);
            merged.putAll(renamed);
            return new Lookup(replaced, renamed, renamedTypes, Map.copyOf(merged));
        }

        Lookup withTypes(Map<String, String> types) {
            return new Lookup(replacedNodes, renamedNodes, types, allNodes);
        }
    }

    private NodeReplacements() {
    }

    public static void load() {
        ModCsv.load("old", DATA_PATH, LOG, NodeReplacements::register);
        ModCsv.load("old", NODE_RENAMES_PATH, LOG, NodeReplacements::registerRenames);
        ModCsv.load("old", TYPE_RENAMES_PATH, LOG, NodeReplacements::registerTypeRenames);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, String> replaced = pairs(rows);
        LOOKUP.updateAndGet(lookup -> lookup.withNodes(replaced, lookup.renamedNodes()));
    }

    public static void registerRenames(JSONArray rows) throws JSONException {
        Map<String, String> renamed = pairs(rows);
        LOOKUP.updateAndGet(lookup -> lookup.withNodes(lookup.replacedNodes(), renamed));
    }

    public static void registerTypeRenames(JSONArray rows) throws JSONException {
        Map<String, String> renamed = Map.copyOf(pairs(rows));
        LOOKUP.updateAndGet(lookup -> lookup.withTypes(renamed));
    }

    private static Map<String, String> pairs(JSONArray rows) throws JSONException {
        Map<String, String> loaded = new LinkedHashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            String oldId = ModCsv.text(row, "old");
            String newId = ModCsv.text(row, "new");
            if (!oldId.isEmpty() && !newId.isEmpty()) {
                loaded.put(oldId, newId);
            }
        });
        return Map.copyOf(loaded);
    }

    public static Map<String, String> all() {
        return LOOKUP.get().allNodes();
    }

    public static boolean isRename(String oldId) {
        return LOOKUP.get().renamedNodes().containsKey(oldId);
    }

    public static String current(String nodeId) {
        return current(nodeId, all(), id -> SkillTree.getDeclared(id) != null);
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

    public static String currentTypeId(String typeId) {
        return currentTypeId(typeId, LOOKUP.get().renamedTypes(), id -> SkillTree.getType(id) != null);
    }

    static String currentTypeId(String typeId, Map<String, String> renamedTypes, Predicate<String> known) {
        if (typeId == null || known.test(typeId)) {
            return typeId;
        }
        String renamed = renamedTypes.get(typeId);
        return renamed != null && known.test(renamed) ? renamed : typeId;
    }

    public static void clear() {
        LOOKUP.set(Lookup.EMPTY);
    }
}
