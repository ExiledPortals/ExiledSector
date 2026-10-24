package exiledsector.charts;

import exiledsector.charts.expansion.ChartExpansion;
import exiledsector.charts.feature.ChartFeature;
import exiledsector.charts.phenomenon.ChartPhenomenon;

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

    public static List<String> expansionIds() {
        return ChartExpansions.BUILT_IN.stream().map(ChartExpansion::id).toList();
    }

    public static List<String> phenomenonIds() {
        return ChartPhenomena.BUILT_IN.stream().map(ChartPhenomenon::id).toList();
    }

    public static List<String> featureIds() {
        return ChartFeatures.BUILT_IN.stream().map(ChartFeature::id).toList();
    }

    public static List<String> tierDefaultsJson() {
        List<String> defaults = new ArrayList<>();
        for (int tier = ChartTiers.LOWEST_TIER; tier <= ChartTiers.HIGHEST_TIER; tier++) {
            ChartTier fallback = ChartTier.fallback(tier);
            defaults.add("{\"tier\": " + tier + ", \"fleetsMin\": " + fallback.fleetsMin() + ", \"fleetsMax\": " + fallback.fleetsMax()
                    + ", \"levelMin\": " + fallback.levelMin() + ", \"levelMax\": " + fallback.levelMax()
                    + ", \"bossSize\": " + fallback.bossSize() + "}");
        }
        return defaults;
    }
}
