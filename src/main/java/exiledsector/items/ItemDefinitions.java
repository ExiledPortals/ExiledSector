package exiledsector.items;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class ItemDefinitions {

    static final String SUBROUTINES_PATH = "data/config/exiledSector/subroutines.csv";
    static final String HULL_ITEMS_PATH = "data/config/exiledSector/hull_items.csv";
    private static final String ID_COLUMN = "id";
    private static final Logger LOG = Logger.getLogger(ItemDefinitions.class);
    private static final AtomicReference<Map<String, ItemDefinition>> BY_ID = new AtomicReference<>(Map.of());

    private ItemDefinitions() {
    }

    public static void load() {
        try {
            register(rows(SUBROUTINES_PATH), rows(HULL_ITEMS_PATH));
        } catch (JSONException e) {
            LOG.error("Failed to load the item definitions", e);
        }
    }

    private static JSONArray rows(String path) {
        try {
            JSONArray rows = ModCsv.rows(ID_COLUMN, path);
            return rows == null ? new JSONArray() : rows;
        } catch (IOException | JSONException e) {
            LOG.error("Failed to load " + path, e);
            return new JSONArray();
        }
    }

    public static void register(JSONArray subroutineRows, JSONArray hullItemRows) throws JSONException {
        Map<String, ItemDefinition> loaded = new LinkedHashMap<>();
        addRows(loaded, subroutineRows, SUBROUTINES_PATH, ItemDefinition::parseSubroutine);
        addRows(loaded, hullItemRows, HULL_ITEMS_PATH, ItemDefinition::parseHullItem);
        BY_ID.set(Collections.unmodifiableMap(loaded));
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, ItemDefinition> loaded = new LinkedHashMap<>();
        addRows(loaded, rows, "item rows",
                row -> row.has(ItemDefinition.SECTION_COLUMN) ? ItemDefinition.parseHullItem(row) : ItemDefinition.parseSubroutine(row));
        BY_ID.set(Collections.unmodifiableMap(loaded));
    }

    private static void addRows(Map<String, ItemDefinition> loaded, JSONArray rows, String source,
                                Function<JSONObject, ItemDefinition> parser) throws JSONException {
        ModCsv.forEach(rows, (index, row) -> {
            try {
                ItemDefinition definition = parser.apply(row);
                if (loaded.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalArgumentException(definition.id() + " is already defined");
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + source + ": " + e.getMessage());
            }
        });
    }

    static void register(Collection<ItemDefinition> definitions) {
        Map<String, ItemDefinition> loaded = new LinkedHashMap<>();
        definitions.forEach(definition -> loaded.put(definition.id(), definition));
        BY_ID.set(Collections.unmodifiableMap(loaded));
    }

    public static ItemDefinition get(String definitionId) {
        return definitionId == null ? null : BY_ID.get().get(ItemIdAliases.currentId(definitionId));
    }

    public static Collection<ItemDefinition> all() {
        return BY_ID.get().values();
    }

    public static void clear() {
        BY_ID.set(Map.of());
    }
}
