package exiledsector.charts;

import java.util.ArrayList;
import java.util.List;

public final class ChartEditorVocabulary {

    private ChartEditorVocabulary() {
    }

    public static List<String> derelictTypes() {
        return ChartPois.DERELICT_TYPES.stream().sorted().toList();
    }

    public static List<String> lootDropTypes() {
        return ChartLoot.VANILLA_TYPE_NAMES;
    }

    public static List<String> lootScopes() {
        return ChartLoot.SCOPES;
    }

    public static List<String> colonyItemGroups() {
        return ChartLoot.COLONY_ITEM_GROUPS.stream().sorted().toList();
    }

    public static List<String> tierDefaultsJson() {
        List<String> defaults = new ArrayList<>();
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier fallback = ChartTier.fallback(tier);
            defaults.add("{\"tier\": " + tier + ", \"fleetsMin\": " + fallback.fleetsMin() + ", \"fleetsMax\": " + fallback.fleetsMax()
                    + ", \"fleetStrengthMin\": " + fallback.fleetStrengthMin() + ", \"fleetStrengthMax\": " + fallback.fleetStrengthMax()
                    + ", \"wardenSize\": " + fallback.wardenSize() + "}");
        }
        return defaults;
    }
}
