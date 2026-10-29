package exiledsector.ui.chart;

import exiledsector.i18n.Translation;
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
import exiledsector.items.UseVerdict;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class StarChartCrafting {

    public static final UseVerdict NO_CHART = refused("no_chart");
    public static final UseVerdict NONE_OWNED = refused("none_owned");
    public static final UseVerdict MIRRORED = refused("mirrored");
    public static final UseVerdict CORRUPTED = refused("corrupted");
    public static final UseVerdict MAX_TIER = refused("max_tier");
    public static final UseVerdict NO_MODIFIERS = refused("no_modifiers");
    public static final UseVerdict FULL = refused("full");
    public static final UseVerdict WARD_BLOCKS = refused("ward_blocks");
    public static final UseVerdict CANNOT_USE = refused("cannot_use");
    static final List<UseVerdict> REFUSALS = List.of(NO_CHART, NONE_OWNED, MIRRORED, CORRUPTED, MAX_TIER, NO_MODIFIERS, FULL, WARD_BLOCKS,
            CANNOT_USE);

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

    private static UseVerdict refused(String reason) {
        return UseVerdict.refused("chart.ui.currency.refused." + reason);
    }

    public static String currencyDescriptionKey(ItemCurrency currency) {
        String chartKey = chartCurrencyDescriptionKey(currency);
        return Translation.has(chartKey) ? chartKey : MaterialItems.descriptionKey(currency.itemId());
    }

    static String chartCurrencyDescriptionKey(ItemCurrency currency) {
        return "chart.currency." + currency.key() + ".description";
    }

    public static List<ItemCurrency> currencies() {
        return ItemCurrencies.forType(ItemType.STAR_CHART);
    }

    public static UseVerdict refusal(ItemCurrency currency, Item chart, ItemStore store, CraftingMaterials materials,
                                  Predicate<ItemDefinition> allowedUniques) {
        return refusal(currency, chart, store, materials, allowedUniques, Set.of());
    }

    public static UseVerdict refusal(ItemCurrency currency, Item chart, ItemStore store, CraftingMaterials materials,
                                  Predicate<ItemDefinition> allowedUniques, Collection<CraftingWard> wards) {
        if (chart == null || chart.type() != ItemType.STAR_CHART || store == null || !store.holdsStarCharts() || store.find(chart.id()) != chart) {
            return NO_CHART;
        }
        if (materials.count(currency.itemId()) < 1) {
            return NONE_OWNED;
        }
        if (ItemCrafting.canUse(currency, chart, allowedUniques, wards)) {
            return UseVerdict.ALLOWED;
        }
        if (ItemCrafting.canUse(currency, chart, allowedUniques)) {
            return WARD_BLOCKS;
        }
        if (chart.mirrored()) {
            return MIRRORED;
        }
        return chart.corrupted() ? CORRUPTED : specificRefusal(currency, chart);
    }

    static UseVerdict specificRefusal(ItemCurrency currency, Item chart) {
        if (!ItemCurrencies.appliesTo(currency, chart.type())) {
            return CANNOT_USE;
        }
        if (currency == StarChartCurrency.CARTOGRAPHY && chart.tier() >= StarChart.HIGHEST_TIER) {
            return MAX_TIER;
        }
        if ((currency == StarChartCurrency.ANNULMENT || currency == CraftingCurrency.ANNULMENT) && chart.explicits().isEmpty()) {
            return NO_MODIFIERS;
        }
        return currency == CraftingCurrency.AUGMENTATION ? FULL : CANNOT_USE;
    }

    public static Used use(ItemCurrency currency, Item chart, ItemStore store, CraftingMaterials materials, Random random,
                           Predicate<ItemDefinition> allowedUniques) {
        return use(currency, chart, store, materials, random, allowedUniques, Set.of());
    }

    public static Used use(ItemCurrency currency, Item chart, ItemStore store, CraftingMaterials materials, Random random,
                           Predicate<ItemDefinition> allowedUniques, Collection<CraftingWard> wards) {
        if (!refusal(currency, chart, store, materials, allowedUniques, wards).allowed()) {
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
