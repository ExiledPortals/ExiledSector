package exiledsector.hullitems;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class HullItemDefinitions {

    static final String DATA_PATH = "data/config/exiledSector/socketables.csv";
    private static final String ID_COLUMN = "id";
    private static final Logger LOG = Logger.getLogger(HullItemDefinitions.class);
    private static final AtomicReference<Map<String, HullItemDefinition>> BY_ID = new AtomicReference<>(Map.of());

    private HullItemDefinitions() {
    }

    public static void load() {
        ModCsv.load(ID_COLUMN, DATA_PATH, LOG, HullItemDefinitions::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, HullItemDefinition> loaded = new LinkedHashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            try {
                HullItemDefinition definition = HullItemDefinition.parse(row);
                loaded.put(definition.id(), definition);
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        BY_ID.set(Collections.unmodifiableMap(loaded));
    }

    public static HullItemDefinition get(String definitionId) {
        return definitionId == null ? null : BY_ID.get().get(definitionId);
    }

    public static Collection<HullItemDefinition> all() {
        return BY_ID.get().values();
    }

    public static void clear() {
        BY_ID.set(Map.of());
    }
}
