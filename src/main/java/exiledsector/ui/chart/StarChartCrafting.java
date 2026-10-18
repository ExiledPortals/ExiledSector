package exiledsector.ui.chart;

import exiledsector.i18n.Translation;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.Item;
import exiledsector.items.ItemCrafting;
import exiledsector.items.ItemCurrencies;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemType;
import exiledsector.items.MaterialItems;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

public final class StarChartCrafting {

    public enum Refusal {
        NONE,
        NO_CHART,
        NONE_OWNED,
        MIRRORED,
        CORRUPTED,
        CANNOT_USE;

        public String noticeKey() {
            return "chart.ui.currency.refused." + name().toLowerCase(Locale.ROOT);
        }
    }

    public record Used(Item result, Item selectedAfter) {
    }

    public static String usedNoticeKey(ItemCurrency currency) {
        return currency.createsCopy() ? "chart.ui.currency.copied" : "chart.ui.currency.used";
    }

    public static String usedNotice(ItemCurrency currency, String nameBefore, Used used) {
        return Translation.msg(usedNoticeKey(currency)).arg("currency", MaterialItems.displayName(currency.itemId()))
                .arg("before", nameBefore).arg("name", used.result().name()).text();
    }

    private StarChartCrafting() {
    }

    public static List<ItemCurrency> currencies() {
        return ItemCurrencies.forType(ItemType.STAR_CHART);
    }

    public static Refusal refusal(ItemCurrency currency, Item chart, ItemStore store, CraftingMaterials materials,
                                  Predicate<ItemDefinition> allowedUniques) {
        if (chart == null || chart.type() != ItemType.STAR_CHART || store == null || !store.holdsStarCharts() || store.find(chart.id()) != chart) {
            return Refusal.NO_CHART;
        }
        if (materials.count(currency.itemId()) < 1) {
            return Refusal.NONE_OWNED;
        }
        if (ItemCrafting.canUse(currency, chart, allowedUniques)) {
            return Refusal.NONE;
        }
        if (chart.mirrored()) {
            return Refusal.MIRRORED;
        }
        return chart.corrupted() ? Refusal.CORRUPTED : Refusal.CANNOT_USE;
    }

    public static Used use(ItemCurrency currency, Item chart, ItemStore store, CraftingMaterials materials, Random random,
                           Predicate<ItemDefinition> allowedUniques) {
        if (refusal(currency, chart, store, materials, allowedUniques) != Refusal.NONE) {
            return null;
        }
        Item result = ItemCrafting.use(currency, chart, materials, store, random, allowedUniques);
        if (result == null) {
            return null;
        }
        Item selectedAfter = currency.createsCopy() || store.find(result.id()) != result ? chart : result;
        return new Used(result, store.find(selectedAfter.id()) == selectedAfter ? selectedAfter : null);
    }
}
