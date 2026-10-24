package exiledsector.items;

import exiledlib.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class ItemDefinitions {

    private static final String ID_COLUMN = "id";
    private static final Logger LOG = Logger.getLogger(ItemDefinitions.class);
    private static final AtomicReference<Map<String, ItemDefinition>> BY_ID = new AtomicReference<>(Map.of());
    private static final Map<ItemType, Map<String, ItemDefinition>> OWN_TYPES_BY_TYPE = new ConcurrentHashMap<>();

    private ItemDefinitions() {
    }

    public static void load() {
        Map<ItemType, JSONArray> rowsByType = new EnumMap<>(ItemType.class);
        for (ItemTypeSupport support : ItemTypes.all()) {
            if (support.definitionsPath() != null) {
                rowsByType.put(support.type(), rows(support.definitionsPath()));
            }
        }
        try {
            register(rowsByType);
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
        Map<ItemType, JSONArray> rowsByType = new EnumMap<>(ItemType.class);
        rowsByType.put(ItemType.SUBROUTINE, subroutineRows);
        rowsByType.put(ItemType.HULL_ITEM, hullItemRows);
        register(rowsByType);
    }

    public static void register(Map<ItemType, JSONArray> rowsByType) throws JSONException {
        Map<String, ItemDefinition> loaded = new LinkedHashMap<>();
        for (ItemType type : ItemType.values()) {
            JSONArray rows = rowsByType.get(type);
            if (rows != null) {
                ItemTypeSupport support = support(type);
                addRows(loaded, rows, support.definitionsPath(), support::parseDefinition);
            }
        }
        BY_ID.set(Collections.unmodifiableMap(loaded));
    }

    public static void register(JSONArray rows) throws JSONException {
        Map<String, ItemDefinition> loaded = new LinkedHashMap<>();
        addRows(loaded, rows, "item rows",
                row -> support(row.has(ItemDefinition.SECTION_COLUMN) ? ItemType.HULL_ITEM : ItemType.SUBROUTINE).parseDefinition(row));
        BY_ID.set(Collections.unmodifiableMap(loaded));
    }

    private static ItemTypeSupport support(ItemType type) {
        ItemTypeSupport support = ItemTypes.of(type);
        if (support == null) {
            throw new IllegalStateException("No domain supports " + type + " items");
        }
        return support;
    }

    private static void addRows(Map<String, ItemDefinition> loaded, JSONArray rows, String source,
                                Function<JSONObject, ItemDefinition> parser) throws JSONException {
        if (rows == null) {
            return;
        }
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

    public static void register(Collection<ItemDefinition> definitions) {
        Map<String, ItemDefinition> loaded = new LinkedHashMap<>();
        definitions.forEach(definition -> loaded.put(definition.id(), definition));
        BY_ID.set(Collections.unmodifiableMap(loaded));
    }

    public static void registerOwnType(ItemType type, Collection<ItemDefinition> definitions) {
        Map<String, ItemDefinition> loaded = new LinkedHashMap<>();
        for (ItemDefinition definition : definitions) {
            if (definition.type() != type) {
                throw new IllegalArgumentException(definition.id() + " is not a " + type.id() + " item");
            }
            loaded.put(definition.id(), definition);
        }
        OWN_TYPES_BY_TYPE.put(type, Collections.unmodifiableMap(loaded));
    }

    public static boolean isOwnType(ItemType type) {
        return OWN_TYPES_BY_TYPE.containsKey(type);
    }

    public static ItemDefinition get(String definitionId) {
        if (definitionId == null) {
            return null;
        }
        String currentId = ItemIdAliases.currentId(definitionId);
        ItemDefinition shipItem = BY_ID.get().get(currentId);
        if (shipItem != null) {
            return shipItem;
        }
        for (Map<String, ItemDefinition> ownType : OWN_TYPES_BY_TYPE.values()) {
            ItemDefinition definition = ownType.get(currentId);
            if (definition != null) {
                return definition;
            }
        }
        return null;
    }

    public static Collection<ItemDefinition> all() {
        return BY_ID.get().values();
    }

    public static Collection<ItemDefinition> ofType(ItemType type) {
        Map<String, ItemDefinition> ownType = OWN_TYPES_BY_TYPE.get(type);
        if (ownType != null) {
            return ownType.values();
        }
        return BY_ID.get().values().stream().filter(definition -> definition.type() == type).toList();
    }

    public static void clear() {
        BY_ID.set(Map.of());
        OWN_TYPES_BY_TYPE.clear();
    }

    public static void clearOwnType(ItemType type) {
        OWN_TYPES_BY_TYPE.remove(type);
    }
}
