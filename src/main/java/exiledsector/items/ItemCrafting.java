package exiledsector.items;

import exiledsector.modifiers.ModifierLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class ItemCrafting {

    static final int DEFAULT_COMMON_SYNTHESIS_PARTS = 10;
    static final int MAX_RECALIBRATION_ATTEMPTS = 64;

    private ItemCrafting() {
    }

    public static int commonSynthesisParts() {
        return ItemCraftingCosts.cost(ItemCraftingCosts.COMMON_SUBROUTINE, DEFAULT_COMMON_SYNTHESIS_PARTS);
    }

    public static int parts(CraftingMaterials materials) {
        return materials.count(ItemDisassembly.PARTS_COMMODITY_ID);
    }

    public static boolean synthesise(CraftingCurrency currency, CraftingMaterials materials) {
        if (!materials.take(ItemDisassembly.PARTS_COMMODITY_ID, currency.partsCost())) {
            return false;
        }
        materials.add(currency.commodityId(), 1);
        return true;
    }

    public static Item synthesiseCommon(CraftingMaterials materials, ItemStore store, Random random) {
        int partsCost = commonSynthesisParts();
        if (parts(materials) < partsCost) {
            return null;
        }
        ItemDefinition definition = ItemDrops.pickBasic(random);
        if (definition == null) {
            return null;
        }
        long seed = random.nextLong();
        List<RolledModifier> modifiers = ItemRoller.rollCommon(definition, seed);
        Item created = store.add(new ItemData(definition.id(), seed, modifiers,
                ItemNames.freeze(definition, seed, modifiers)));
        if (created != null) {
            materials.take(ItemDisassembly.PARTS_COMMODITY_ID, partsCost);
        }
        return created;
    }

    public static boolean canUse(CraftingCurrency currency, Item item, Predicate<ItemDefinition> allowedUniques) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null) {
            return false;
        }
        return switch (currency) {
            case AUGMENTATION -> !AffixLayout.openSides(definition, item.explicits()).isEmpty();
            case RECALIBRATION -> canRecalibrate(item, definition);
            case TRANSPOSITION -> canTranspose(definition, allowedUniques);
        };
    }

    public static Item use(CraftingCurrency currency, Item item, CraftingMaterials materials, ItemStore store, Random random,
                                 Predicate<ItemDefinition> allowedUniques) {
        if (materials.count(currency.commodityId()) < 1 || !canUse(currency, item, allowedUniques)) {
            return null;
        }
        Item craftedItem = switch (currency) {
            case AUGMENTATION -> augment(item, random) == null ? null : item;
            case RECALIBRATION -> recalibrate(item, random);
            case TRANSPOSITION -> transpose(store, item, random, allowedUniques);
        };
        if (craftedItem != null) {
            materials.take(currency.commodityId(), 1);
        }
        return craftedItem;
    }

    static RolledModifier augment(Item item, Random random) {
        ItemDefinition definition = item.definition();
        List<List<PoolEntry>> openSides = definition == null ? List.of() : AffixLayout.openSides(definition, item.explicits());
        if (definition == null || openSides.isEmpty()) {
            return null;
        }
        List<PoolEntry> chosenSide = openSides.get(random.nextInt(openSides.size()));
        PoolEntry poolEntry = WeightedPick.pick(chosenSide, PoolEntry::weight, random);
        RolledModifier addedModifier = RolledModifier.roll(poolEntry.modifier(), random, false);
        ItemRarity rarityBefore = item.rarity();
        List<RolledModifier> updatedModifiers = new ArrayList<>(item.explicits());
        updatedModifiers.add(AffixLayout.insertIndex(updatedModifiers, addedModifier.slot()), addedModifier);
        item.replaceExplicits(updatedModifiers);
        if (item.rarity() != rarityBefore) {
            item.refreezeName();
        }
        return addedModifier;
    }

    private static boolean canRecalibrate(Item item, ItemDefinition definition) {
        for (RolledModifier rolled : item.explicits()) {
            PoolEntry poolEntry = definition.poolEntryFor(rolled.modifierId());
            if (poolEntry != null && rolled.lines().stream().map(line -> poolEntry.modifier().line(line.effectName()))
                    .anyMatch(line -> line != null && line.canVary())) {
                return true;
            }
        }
        return false;
    }

    static Item recalibrate(Item item, Random random) {
        ItemDefinition definition = item.definition();
        if (definition == null) {
            return null;
        }
        for (int attempt = 0; attempt < MAX_RECALIBRATION_ATTEMPTS; attempt++) {
            List<RolledModifier> rerolled = reroll(item.explicits(), definition, random);
            if (!rerolled.equals(item.explicits())) {
                item.replaceExplicits(rerolled);
                return item;
            }
        }
        return null;
    }

    private static List<RolledModifier> reroll(List<RolledModifier> modifiers, ItemDefinition definition, Random random) {
        List<RolledModifier> rerolled = new ArrayList<>(modifiers.size());
        for (RolledModifier rolled : modifiers) {
            PoolEntry poolEntry = definition.poolEntryFor(rolled.modifierId());
            if (poolEntry == null) {
                rerolled.add(rolled);
                continue;
            }
            List<RolledEffect> rerolledLines = new ArrayList<>(rolled.lines().size());
            for (RolledEffect line : rolled.lines()) {
                ModifierLine lineDefinition = poolEntry.modifier().line(line.effectName());
                rerolledLines.add(lineDefinition == null ? line : new RolledEffect(line.effectName(), lineDefinition.roll(random)));
            }
            rerolled.add(rolled.withLines(rerolledLines));
        }
        return rerolled;
    }

    private static boolean canTranspose(ItemDefinition currentDefinition, Predicate<ItemDefinition> allowedUniques) {
        if (!currentDefinition.unique()) {
            return false;
        }
        for (ItemDefinition definition : ItemDefinitions.all()) {
            if (otherUnique(definition, currentDefinition, allowedUniques)) {
                return true;
            }
        }
        return false;
    }

    static Item transpose(ItemStore store, Item item, Random random, Predicate<ItemDefinition> allowedUniques) {
        ItemDefinition currentDefinition = item.definition();
        if (currentDefinition == null) {
            return null;
        }
        ItemDefinition nextDefinition = ItemDrops.pickUnique(random, definition -> otherUnique(definition, currentDefinition, allowedUniques));
        if (nextDefinition == null) {
            return null;
        }
        long seed = random.nextLong();
        Item replacement = Item.create(item.id(), nextDefinition.id(), seed, ItemRoller.roll(nextDefinition, seed));
        replacement.freezeName();
        return store.replace(item, replacement) ? replacement : null;
    }

    private static boolean otherUnique(ItemDefinition definition, ItemDefinition currentDefinition, Predicate<ItemDefinition> allowedUniques) {
        return definition.unique() && !definition.id().equals(currentDefinition.id()) && definition.rarity() > 0f && allowedUniques.test(definition);
    }
}
