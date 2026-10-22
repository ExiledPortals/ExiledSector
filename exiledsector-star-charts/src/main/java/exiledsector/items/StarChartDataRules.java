package exiledsector.items;

import exiledlib.ModCsv;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.TierRange;
import exiledsector.modifiers.ValueScaling;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static exiledsector.items.ItemDataIntegrity.ENTRY_SEPARATOR;
import static exiledsector.items.ItemDataIntegrity.FIRST_DATA_LINE;

public final class StarChartDataRules implements ItemDataRules {

    public static final StarChartDataRules INSTANCE = new StarChartDataRules();

    private StarChartDataRules() {
    }

    @Override
    public ItemType type() {
        return ItemType.STAR_CHART;
    }

    @Override
    public String modifierFile() {
        return StarChartItems.MODIFIERS_PATH;
    }

    @Override
    public void checkModifierFiles(ItemDataIntegrity integrity) throws IOException, JSONException {
        integrity.checkModifiers(ItemDataIntegrity.orEmpty(ModCsv.rows("id", StarChartItems.MODIFIERS_PATH)), StarChartItems.MODIFIERS_PATH);
    }

    @Override
    public void checkItemFiles(ItemDataIntegrity integrity) throws IOException, JSONException {
        integrity.checkDefinitions(ItemDataIntegrity.orEmpty(ModCsv.rows("id", StarChartDefinitions.DATA_PATH)), StarChartDefinitions.DATA_PATH,
                ItemType.STAR_CHART);
        checkImplicits(integrity, ItemDataIntegrity.orEmpty(ModCsv.rows(StarChartImplicits.SYSTEM_TYPE_COLUMN, StarChartImplicits.DATA_PATH)));
    }

    @Override
    public void checkModifierRow(ItemDataIntegrity integrity, String file, int line, String modifierId, Set<ModifierScope> scopes,
                                 List<ModifierLine> lines) {
        boolean chartFile = StarChartItems.MODIFIERS_PATH.equals(file);
        if (!scopes.isEmpty()) {
            if (chartFile && !scopes.equals(Set.of(ModifierScope.STAR_CHART))) {
                integrity.error(file, line, modifierId + " is in " + file + ", so its item_types must be " + ModifierScope.STAR_CHART.id() + " only");
            } else if (!chartFile && scopes.contains(ModifierScope.STAR_CHART)) {
                integrity.error(file, line, modifierId + " rolls on " + ModifierScope.STAR_CHART.id() + "; star chart modifiers belong in "
                        + StarChartItems.MODIFIERS_PATH);
            }
        }
        for (ModifierLine modifierLine : lines) {
            checkTierBands(integrity, file, line, modifierId, modifierLine, chartFile);
        }
    }

    private static void checkTierBands(ItemDataIntegrity integrity, String file, int line, String modifierId, ModifierLine modifierLine,
                                       boolean chartFile) {
        if (!modifierLine.scalesWithTier()) {
            return;
        }
        if (!chartFile) {
            integrity.error(file, line, modifierId + " gives " + modifierLine.effectName() + " tier bands, but only star chart modifiers in "
                    + StarChartItems.MODIFIERS_PATH + " have tiers");
            return;
        }
        if (modifierLine.scaling() instanceof ValueScaling.PerTierBand tierBands
                && tierBands.bandStartTiers().get(tierBands.bandStartTiers().size() - 1) > StarChart.HIGHEST_TIER) {
            integrity.error(file, line, modifierId + " starts a tier band above tier " + StarChart.HIGHEST_TIER);
        }
    }

    @Override
    public void checkDefinitionRow(ItemDataIntegrity integrity, String file, int line, String definitionId, JSONObject row) {
        checkSystemType(integrity, file, line, definitionId, ModCsv.text(row, StarChartDefinitions.SYSTEM_TYPE_COLUMN));
        String minTierText = ModCsv.text(row, StarChartDefinitions.MIN_TIER_COLUMN);
        if (StarChartDefinitions.parsedMinTier(minTierText) == null) {
            integrity.error(file, line, definitionId + " has minimum tier \"" + minTierText + "\"; expected a whole number from " + StarChart.LOWEST_TIER
                    + " to " + StarChart.HIGHEST_TIER + " or blank");
        }
    }

    private static void checkSystemType(ItemDataIntegrity integrity, String file, int line, String ownerId, String systemTypeText) {
        if (StarChartDefinitions.normalisedSystemType(systemTypeText) == null) {
            integrity.error(file, line, ownerId + " has system type \"" + systemTypeText + "\"; expected one of " + StarChartDefinitions.SYSTEM_TYPES
                    + " or " + StarChartDefinitions.ANY_SYSTEM_TYPE);
        }
    }

    @Override
    public void checkTierGate(ItemDataIntegrity integrity, String file, int line, String definitionId, String entryText, String tierText) {
        try {
            TierRange tiers = PoolEntry.parseTierRange(tierText, entryText);
            if (tiers.minTier() > StarChart.HIGHEST_TIER || tiers.maxTier() != Integer.MAX_VALUE && tiers.maxTier() > StarChart.HIGHEST_TIER) {
                integrity.error(file, line, definitionId + " entry \"" + entryText + "\" reaches past tier " + StarChart.HIGHEST_TIER);
            }
        } catch (IllegalArgumentException e) {
            integrity.error(file, line, definitionId + ": " + e.getMessage());
        }
    }

    public static void checkImplicits(ItemDataIntegrity integrity, JSONArray rows) throws JSONException {
        if (rows == null) {
            return;
        }
        String file = StarChartImplicits.DATA_PATH;
        Map<String, Integer> linesByType = new HashMap<>();
        ModCsv.forEach(rows, (index, row) -> {
            int line = index + FIRST_DATA_LINE;
            String systemTypeText = ModCsv.text(row, StarChartImplicits.SYSTEM_TYPE_COLUMN);
            checkSystemType(integrity, file, line, "the row", systemTypeText);
            String systemType = StarChartDefinitions.normalisedSystemType(systemTypeText);
            Integer earlierLine = systemType == null ? null : linesByType.putIfAbsent(systemType, line);
            if (earlierLine != null) {
                integrity.error(file, line, systemType + " is already listed on line " + earlierLine);
            }
            List<ItemDataIntegrity.ModifierRow> implicits = new ArrayList<>();
            for (String entryText : ModCsv.text(row, "implicits").split(ENTRY_SEPARATOR)) {
                integrity.checkImplicitEntry(file, line, systemTypeText, entryText.trim(), implicits, ItemType.STAR_CHART);
            }
        });
    }
}
