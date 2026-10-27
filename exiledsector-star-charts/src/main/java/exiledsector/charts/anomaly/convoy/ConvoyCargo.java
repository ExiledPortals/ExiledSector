package exiledsector.charts.anomaly.convoy;

import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.charts.loot.ChartLoot;
import exiledsector.charts.loot.DropType;
import exiledsector.items.ItemDrops;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ConvoyCargo {

    static final String CATEGORY_KEY_PREFIX = "chart.convoy.cargo.";

    private ConvoyCargo() {
    }

    static List<String> manifestKeys(int tier) {
        List<String> keys = new ArrayList<>();
        for (DropType type : ChartLoot.dropTypesFor(ConvoyRules.LOOT_SCOPE, tier)) {
            keys.add(CATEGORY_KEY_PREFIX + type.name().toLowerCase(Locale.ROOT));
        }
        if (ItemDrops.hasRule(ConvoyRules.CARGO_SITE)) {
            keys.add(CATEGORY_KEY_PREFIX + "items");
        }
        if (ChartDrops.hasRule(ChartDrops.Source.SYSTEM_SALVAGE)) {
            keys.add(CATEGORY_KEY_PREFIX + "currency");
            keys.add(CATEGORY_KEY_PREFIX + "star_chart");
        }
        return keys;
    }

    static StyledText manifest(int tier) {
        return Translation.list(manifestKeys(tier).stream().map(Translation::styled).toList());
    }
}
