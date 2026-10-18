package exiledsector.items;

import exiledsector.ModCsv;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class StarChartDefinitions {

    static final String DATA_PATH = "data/config/exiledSector/star_charts.csv";
    static final String SYSTEM_TYPE_COLUMN = "system_type";
    public static final String ANY_SYSTEM_TYPE = "any";
    public static final List<String> SYSTEM_TYPES = List.of("SINGLE", "BINARY_CLOSE", "BINARY_FAR", "TRINARY_2CLOSE", "TRINARY_1CLOSE_1FAR",
            "TRINARY_2FAR");
    private static final Logger LOG = Logger.getLogger(StarChartDefinitions.class);
    private static final AtomicReference<Map<String, String>> SYSTEM_TYPE_BY_ID = new AtomicReference<>(Map.of());

    private StarChartDefinitions() {
    }

    public static void load() {
        ModCsv.load("id", DATA_PATH, LOG, StarChartDefinitions::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        List<ItemDefinition> definitions = new ArrayList<>();
        Map<String, String> systemTypes = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            try {
                ItemDefinition definition = ItemDefinition.parseStarChart(row);
                String systemType = normalisedSystemType(ModCsv.text(row, SYSTEM_TYPE_COLUMN));
                if (systemType == null) {
                    throw new IllegalArgumentException(definition.id() + " has system type \"" + ModCsv.text(row, SYSTEM_TYPE_COLUMN)
                            + "\"; expected " + SYSTEM_TYPES + " or " + ANY_SYSTEM_TYPE);
                }
                ItemDefinition shipItem = ItemDefinitions.get(definition.id());
                if ((shipItem != null && shipItem.type() != ItemType.STAR_CHART) || systemTypes.containsKey(definition.id())) {
                    throw new IllegalArgumentException(definition.id() + " is already defined");
                }
                definitions.add(definition);
                systemTypes.put(definition.id(), systemType);
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        ItemDefinitions.registerStarCharts(definitions);
        SYSTEM_TYPE_BY_ID.set(Collections.unmodifiableMap(systemTypes));
    }

    static String normalisedSystemType(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(ANY_SYSTEM_TYPE)) {
            return ANY_SYSTEM_TYPE;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        return SYSTEM_TYPES.contains(upper) ? upper : null;
    }

    public static String systemType(String definitionId) {
        String systemType = definitionId == null ? null : SYSTEM_TYPE_BY_ID.get().get(ItemIdAliases.currentId(definitionId));
        return systemType == null ? ANY_SYSTEM_TYPE : systemType;
    }

    public static void clear() {
        SYSTEM_TYPE_BY_ID.set(Map.of());
        ItemDefinitions.clearStarCharts();
    }
}
