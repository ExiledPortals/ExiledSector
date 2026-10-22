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
    static final String MIN_TIER_COLUMN = "min_tier";
    public static final String ANY_SYSTEM_TYPE = "any";
    public static final List<String> SYSTEM_TYPES = List.of("SINGLE", "BINARY_CLOSE", "BINARY_FAR", "TRINARY_2CLOSE", "TRINARY_1CLOSE_1FAR",
            "TRINARY_2FAR");
    private static final Logger LOG = Logger.getLogger(StarChartDefinitions.class);
    private static final AtomicReference<Map<String, String>> SYSTEM_TYPE_BY_ID = new AtomicReference<>(Map.of());
    private static final AtomicReference<Map<String, Integer>> MIN_TIER_BY_ID = new AtomicReference<>(Map.of());

    private StarChartDefinitions() {
    }

    public static void load() {
        ModCsv.load("id", DATA_PATH, LOG, StarChartDefinitions::register);
    }

    public static void register(JSONArray rows) throws JSONException {
        List<ItemDefinition> definitions = new ArrayList<>();
        Map<String, String> systemTypes = new LinkedHashMap<>();
        Map<String, Integer> minTiers = new LinkedHashMap<>();
        ModCsv.forEach(rows == null ? new JSONArray() : rows, (index, row) -> {
            try {
                ItemDefinition definition = ItemDefinition.parseStarChart(row);
                String systemType = normalisedSystemType(ModCsv.text(row, SYSTEM_TYPE_COLUMN));
                if (systemType == null) {
                    throw new IllegalArgumentException(definition.id() + " has system type \"" + ModCsv.text(row, SYSTEM_TYPE_COLUMN)
                            + "\"; expected " + SYSTEM_TYPES + " or " + ANY_SYSTEM_TYPE);
                }
                Integer minTier = parsedMinTier(ModCsv.text(row, MIN_TIER_COLUMN));
                if (minTier == null) {
                    throw new IllegalArgumentException(definition.id() + " has minimum tier \"" + ModCsv.text(row, MIN_TIER_COLUMN)
                            + "\"; expected a whole number from " + StarChart.LOWEST_TIER + " to " + StarChart.HIGHEST_TIER + " or blank");
                }
                ItemDefinition shipItem = ItemDefinitions.get(definition.id());
                if ((shipItem != null && shipItem.type() != ItemType.STAR_CHART) || systemTypes.containsKey(definition.id())) {
                    throw new IllegalArgumentException(definition.id() + " is already defined");
                }
                if (!ItemUnlock.isKnownUnlock(definition.unlock())) {
                    LOG.warn(definition.id() + " in " + DATA_PATH + " has the unknown unlock \"" + definition.unlock()
                            + "\", so it can never drop; use an unlock id or " + ItemUnlock.CLEARED_CHART_TIER_PREFIX + "N");
                }
                definitions.add(definition);
                systemTypes.put(definition.id(), systemType);
                if (minTier > StarChart.LOWEST_TIER) {
                    minTiers.put(definition.id(), minTier);
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Skipping row " + (index + 1) + " of " + DATA_PATH + ": " + e.getMessage());
            }
        });
        ItemDefinitions.registerStarCharts(definitions);
        SYSTEM_TYPE_BY_ID.set(Collections.unmodifiableMap(systemTypes));
        MIN_TIER_BY_ID.set(Collections.unmodifiableMap(minTiers));
    }

    static String normalisedSystemType(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(ANY_SYSTEM_TYPE)) {
            return ANY_SYSTEM_TYPE;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        return SYSTEM_TYPES.contains(upper) ? upper : null;
    }

    static Integer parsedMinTier(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return StarChart.LOWEST_TIER;
        }
        try {
            int minTier = Integer.parseInt(trimmed);
            return minTier >= StarChart.LOWEST_TIER && minTier <= StarChart.HIGHEST_TIER ? minTier : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String systemType(String definitionId) {
        String systemType = definitionId == null ? null : SYSTEM_TYPE_BY_ID.get().get(ItemIdAliases.currentId(definitionId));
        return systemType == null ? ANY_SYSTEM_TYPE : systemType;
    }

    public static int minTier(String definitionId) {
        Integer minTier = definitionId == null ? null : MIN_TIER_BY_ID.get().get(ItemIdAliases.currentId(definitionId));
        return minTier == null ? StarChart.LOWEST_TIER : minTier;
    }

    public static int tierAtLeastMinimum(String definitionId, int tier) {
        return Math.max(minTier(definitionId), StarChart.clampTier(tier));
    }

    public static boolean rollsAtTier(String definitionId, int tier) {
        return minTier(definitionId) <= StarChart.clampTier(tier);
    }

    public static void clear() {
        SYSTEM_TYPE_BY_ID.set(Map.of());
        MIN_TIER_BY_ID.set(Map.of());
        ItemDefinitions.clearStarCharts();
    }
}
