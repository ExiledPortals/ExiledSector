package exiledsector.charts.officer.reward;

import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.MaterialItems;

import java.util.List;

public final class OfficerRewardText {

    private static final String KEY_PREFIX = "chart.officer.reward.";
    private static final String REGION_CATEGORY = "region";

    private OfficerRewardText() {
    }

    public static AtlasLine carriedLine(List<CarriedItem> carried) {
        if (carried == null || carried.isEmpty()) {
            return null;
        }
        String items = joined(carried.stream().map(OfficerRewardText::name).toList());
        return new AtlasLine(Translation.msg(KEY_PREFIX + "carries").arg("items", items).text(), AtlasLine.Tone.MARKER);
    }

    public static String name(CarriedItem item) {
        if (item.kind().craftingItem()) {
            return MaterialItems.displayName(item.id());
        }
        ItemDefinition definition = ItemDefinitions.get(item.id());
        String itemName = definition == null ? item.id() : definition.displayName();
        if (item.kind() != RewardKind.BIOME_HULL_ITEM || item.variant() == null) {
            return itemName;
        }
        return Translation.msg(KEY_PREFIX + "biomeItem").arg("item", itemName).arg("biome", AtlasNames.name(REGION_CATEGORY, item.variant())).text();
    }

    static String joined(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        String rest = String.join(Translation.text(KEY_PREFIX + "sep"), names.subList(0, names.size() - 1));
        return Translation.msg(KEY_PREFIX + "and").arg("rest", rest).arg("last", names.get(names.size() - 1)).text();
    }
}
