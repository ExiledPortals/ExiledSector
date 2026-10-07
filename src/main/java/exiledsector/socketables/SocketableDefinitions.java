package exiledsector.socketables;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class SocketableDefinitions {

    static final String DATA_PATH = "data/config/exiledSector/socketables.csv";
    private static final String ID_COLUMN = "id";
    private static final Logger LOG = Logger.getLogger(SocketableDefinitions.class);
    private static final AtomicReference<Map<String, SocketableDefinition>> BY_ID = new AtomicReference<>(Map.of());

    private SocketableDefinitions() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, DATA_PATH, LOG, SocketableDefinitions::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, SocketableDefinition> loaded = new LinkedHashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            try {
                SocketableDefinition definition = SocketableDefinition.parse(row);
                loaded.put(definition.id(), definition);
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        BY_ID.set(Collections.unmodifiableMap(loaded));
    }

    public static SocketableDefinition get(String definitionId) {
        return definitionId == null ? null : BY_ID.get().get(definitionId);
    }

    public static Collection<SocketableDefinition> all() {
        return BY_ID.get().values();
    }

    public static void clear() {
        BY_ID.set(Map.of());
    }
}
