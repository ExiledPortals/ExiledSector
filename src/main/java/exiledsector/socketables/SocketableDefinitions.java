package exiledsector.socketables;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static exiledsector.ExiledSectorModPlugin.MOD_ID;

public final class SocketableDefinitions {

    static final String DATA_PATH = "data/config/exiledSector/socketables.csv";
    private static final String ID_COLUMN = "id";
    private static final Logger LOG = Logger.getLogger(SocketableDefinitions.class);
    private static final AtomicReference<Map<String, SocketableDefinition>> BY_ID = new AtomicReference<>(Map.of());

    private SocketableDefinitions() {
    }

    public static void load() {
        try {
            register(Global.getSettings().getMergedSpreadsheetDataForMod(ID_COLUMN, DATA_PATH, MOD_ID));
        } catch (IOException | JSONException e) {
            LOG.error("Failed to load " + DATA_PATH, e);
        }
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, SocketableDefinition> loaded = new LinkedHashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            try {
                SocketableDefinition definition = SocketableDefinition.parse(rows.getJSONObject(i));
                loaded.put(definition.id(), definition);
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (i + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        }
        BY_ID.set(Collections.unmodifiableMap(loaded));
    }

    public static SocketableDefinition get(String id) {
        return id == null ? null : BY_ID.get().get(id);
    }

    public static Collection<SocketableDefinition> all() {
        return BY_ID.get().values();
    }

    public static void clear() {
        BY_ID.set(Map.of());
    }
}
