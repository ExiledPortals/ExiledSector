package exiledsector.items;

import exiledsector.i18n.Translation;

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
    public static final UseVerdict INHIBITOR_BLOCKS = refused("inhibitor_blocks");
    public static final UseVerdict CANNOT_USE = refused("cannot_use");
    public static final List<UseVerdict> REFUSALS = List.of(NO_CHART, NONE_OWNED, MIRRORED, CORRUPTED, MAX_TIER, NO_MODIFIERS, FULL, INHIBITOR_BLOCKS,
            CANNOT_USE);

    public record Used(Item result, Item selectedAfter) {
    }

    public static String usedNoticeKey(CraftingMaterialFamily craftingItem) {
        return craftingItem.createsCopy() ? "chart.ui.craftingItem.copied" : "chart.ui.craftingItem.used";
    }

    public static String usedNotice(CraftingMaterialFamily craftingItem, String nameBefore, Used used) {
        return Translation.msg(usedNoticeKey(craftingItem)).arg("craftingItem", MaterialItems.displayName(craftingItem.itemId()))
                .arg("before", nameBefore).arg("name", used.result().name()).text();
    }

    private StarChartCrafting() {
    }

    private static UseVerdict refused(String reason) {
        return UseVerdict.refused("chart.ui.craftingItem.refused." + reason);
    }

    public static String craftingItemDescriptionKey(CraftingMaterialFamily craftingItem) {
        return MaterialItems.descriptionKey(craftingItem.itemId());
    }

    public static List<CraftingMaterialFamily> craftingItems() {
        return CraftingMaterialFamilies.forType(ItemType.STAR_CHART);
    }

    public static UseVerdict refusal(CraftingMaterialFamily craftingItem, Item chart, ItemStore store, CraftingMaterials materials,
                                  Predicate<ItemDefinition> allowedUniques) {
        return refusal(craftingItem, chart, store, materials, allowedUniques, Set.of());
    }

    public static UseVerdict refusal(CraftingMaterialFamily craftingItem, Item chart, ItemStore store, CraftingMaterials materials,
                                  Predicate<ItemDefinition> allowedUniques, Collection<CraftingInhibitor> inhibitors) {
        if (chart == null || chart.type() != ItemType.STAR_CHART || store == null || !store.holdsStarCharts() || store.find(chart.id()) != chart) {
            return NO_CHART;
        }
        if (materials.count(craftingItem.itemId()) < 1) {
            return NONE_OWNED;
        }
        if (ItemCrafting.canUse(craftingItem, chart, allowedUniques, inhibitors)) {
            return UseVerdict.ALLOW;
        }
        if (ItemCrafting.canUse(craftingItem, chart, allowedUniques)) {
            return INHIBITOR_BLOCKS;
        }
        if (chart.mirrored()) {
            return MIRRORED;
        }
        return chart.corrupted() ? CORRUPTED : specificRefusal(craftingItem, chart);
    }

    static UseVerdict specificRefusal(CraftingMaterialFamily craftingItem, Item chart) {
        if (!CraftingMaterialFamilies.appliesTo(craftingItem, chart.type())) {
            return CANNOT_USE;
        }
        if (craftingItem == StarChartCipher.CARTOGRAPHY && chart.tier() >= StarChart.HIGHEST_TIER) {
            return MAX_TIER;
        }
        if ((craftingItem == StarChartCipher.ERASURE || craftingItem == Nanoswarm.ANNULMENT) && chart.explicits().isEmpty()) {
            return NO_MODIFIERS;
        }
        return craftingItem == Nanoswarm.AUGMENTATION ? FULL : CANNOT_USE;
    }

    public static Used use(CraftingMaterialFamily craftingItem, Item chart, ItemStore store, CraftingMaterials materials, Random random,
                           Predicate<ItemDefinition> allowedUniques) {
        return use(craftingItem, chart, store, materials, random, allowedUniques, Set.of());
    }

    public static Used use(CraftingMaterialFamily craftingItem, Item chart, ItemStore store, CraftingMaterials materials, Random random,
                           Predicate<ItemDefinition> allowedUniques, Collection<CraftingInhibitor> inhibitors) {
        if (!refusal(craftingItem, chart, store, materials, allowedUniques, inhibitors).allowed()) {
            return null;
        }
        Item result = ItemCrafting.use(craftingItem, chart, materials, store, random, allowedUniques, inhibitors);
        if (result == null) {
            return null;
        }
        Item selectedAfter = craftingItem.createsCopy() || store.find(result.id()) != result ? chart : result;
        return new Used(result, store.find(selectedAfter.id()) == selectedAfter ? selectedAfter : null);
    }
}
