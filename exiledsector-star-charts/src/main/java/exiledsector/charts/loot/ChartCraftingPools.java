package exiledsector.charts.loot;

import exiledsector.items.CraftingDrops;
import exiledsector.items.CraftingInhibitor;
import exiledsector.items.StarChartCipher;

import java.util.function.Predicate;

public final class ChartCraftingPools {

    public static final Predicate<String> ANY_ITEM = CraftingDrops.ANY_ITEM;
    public static final Predicate<String> CIPHERS = itemId -> StarChartCipher.byItemId(itemId) != null;
    public static final Predicate<String> TAG_INHIBITORS = ChartCraftingPools::isTagInhibitor;

    private ChartCraftingPools() {
    }

    private static boolean isTagInhibitor(String itemId) {
        CraftingInhibitor inhibitor = CraftingInhibitor.byItemId(itemId);
        return inhibitor != null && inhibitor.inhibitsTag();
    }
}
