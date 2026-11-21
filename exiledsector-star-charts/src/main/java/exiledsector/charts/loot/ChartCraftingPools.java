package exiledsector.charts.loot;

import exiledsector.items.crafting.material.CraftingInhibitor;
import exiledsector.items.crafting.material.StarChartCipher;
import exiledsector.items.drop.CraftingDrops;

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
