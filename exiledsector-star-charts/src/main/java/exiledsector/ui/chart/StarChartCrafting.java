package exiledsector.ui.chart;

import exiledlib.i18n.Translation;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.CraftingWard;
import exiledsector.items.Item;
import exiledsector.items.ItemCrafting;
import exiledsector.items.ItemCurrencies;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemStore;
import exiledsector.items.ItemType;
import exiledsector.items.MaterialItems;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartCurrency;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class StarChartCrafting {

    public enum Refusal {
        NONE,
        NO_CHART,
        NONE_OWNED,
        MIRRORED,
        CORRUPTED,
        MAX_TIER,
        UNIQUE,
        NOT_UNIQUE,
        NO_MODIFIERS,
        FULL,
        WARD_BLOCKS,
        WARD_BLOCKS_TRANSPOSITION,
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

    public static String currencyDescriptionKey(ItemCurrency currency) {
        String chartKey = chartCurrencyDescriptionKey(currency);
        return Translation.has(chartKey) ? chartKey : "material." + currency.itemId() + ".description";
    }

    static String chartCurrencyDescriptionKey(ItemCurrency currency) {
        return "chart.currency." + currency.key() + ".description";
    }

    public static List<ItemCurrency> currencies() {
        return ItemCurrencies.forType(ItemType.STAR_CHART);
    }

    public static Refusal refusal(ItemCurrency currency, Item chart, ItemStore store, CraftingMaterials materials,
                                  Predicate<ItemDefinition> allowedUniques) {
        return refusal(currency, chart, store, materials, allowedUniques, Set.of());
    }

    public static Refusal refusal(ItemCurrency currency, Item chart, ItemStore store, CraftingMaterials materials,
                                  Predicate<ItemDefinition> allowedUniques, Collection<CraftingWard> wards) {
        if (chart == null || chart.type() != ItemType.STAR_CHART || store == null || !store.holdsStarCharts() || store.find(chart.id()) != chart) {
            return Refusal.NO_CHART;
        }
        if (materials.count(currency.itemId()) < 1) {
            return Refusal.NONE_OWNED;
        }
        if (ItemCrafting.canUse(currency, chart, allowedUniques, wards)) {
            return Refusal.NONE;
        }
        if (ItemCrafting.canUse(currency, chart, allowedUniques)) {
            return currency == CraftingCurrency.TRANSPOSITION ? Refusal.WARD_BLOCKS_TRANSPOSITION : Refusal.WARD_BLOCKS;
        }
        if (chart.mirrored()) {
            return Refusal.MIRRORED;
        }
        return chart.corrupted() ? Refusal.CORRUPTED : specificRefusal(currency, chart);
    }

    static Refusal specificRefusal(ItemCurrency currency, Item chart) {
        if (!ItemCurrencies.appliesTo(currency, chart.type())) {
            return Refusal.CANNOT_USE;
        }
        boolean unique = chart.definition() != null && chart.definition().unique();
        if (currency == StarChartCurrency.CARTOGRAPHY && chart.tier() >= StarChart.HIGHEST_TIER) {
            return Refusal.MAX_TIER;
        }
        if (currency == CraftingCurrency.TRANSPOSITION && !unique) {
            return Refusal.NOT_UNIQUE;
        }
        boolean changesModifiers = currency == StarChartCurrency.REFORGE || currency == StarChartCurrency.ANNULMENT
                || currency == CraftingCurrency.AUGMENTATION;
        if (changesModifiers && unique) {
            return Refusal.UNIQUE;
        }
        if (currency == StarChartCurrency.ANNULMENT && chart.explicits().isEmpty()) {
            return Refusal.NO_MODIFIERS;
        }
        return currency == CraftingCurrency.AUGMENTATION ? Refusal.FULL : Refusal.CANNOT_USE;
    }

    public static Used use(ItemCurrency currency, Item chart, ItemStore store, CraftingMaterials materials, Random random,
                           Predicate<ItemDefinition> allowedUniques) {
        return use(currency, chart, store, materials, random, allowedUniques, Set.of());
    }

    public static Used use(ItemCurrency currency, Item chart, ItemStore store, CraftingMaterials materials, Random random,
                           Predicate<ItemDefinition> allowedUniques, Collection<CraftingWard> wards) {
        if (refusal(currency, chart, store, materials, allowedUniques, wards) != Refusal.NONE) {
            return null;
        }
        Item result = ItemCrafting.use(currency, chart, materials, store, random, allowedUniques, wards);
        if (result == null) {
            return null;
        }
        Item selectedAfter = currency.createsCopy() || store.find(result.id()) != result ? chart : result;
        return new Used(result, store.find(selectedAfter.id()) == selectedAfter ? selectedAfter : null);
    }
}
