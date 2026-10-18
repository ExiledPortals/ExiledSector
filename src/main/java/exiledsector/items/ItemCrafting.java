package exiledsector.items;

import exiledsector.modifiers.ModifierLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class ItemCrafting {

    static final int DEFAULT_COMMON_SYNTHESIS_PARTS = 10;
    static final int MAX_RECALIBRATION_ATTEMPTS = 64;
    static final int MIN_FIXATION_MODIFIERS = 2;

    private ItemCrafting() {
    }

    public static int commonSynthesisParts() {
        return ItemCraftingCosts.cost(ItemCraftingCosts.COMMON_SUBROUTINE, DEFAULT_COMMON_SYNTHESIS_PARTS);
    }

    public static int parts(CraftingMaterials materials) {
        return materials.count(ItemDisassembly.PARTS_ITEM_ID);
    }

    public static boolean synthesise(CraftingCurrency currency, CraftingMaterials materials) {
        if (!currency.synthesisable() || !materials.take(ItemDisassembly.PARTS_ITEM_ID, currency.partsCost())) {
            return false;
        }
        materials.add(currency.itemId(), 1);
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
            materials.take(ItemDisassembly.PARTS_ITEM_ID, partsCost);
        }
        return created;
    }

    public static boolean canUse(CraftingCurrency currency, Item item, Predicate<ItemDefinition> allowedUniques) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || item.mirrored()) {
            return false;
        }
        return switch (currency) {
            case AUGMENTATION -> !AffixLayout.openSides(definition, item.explicits()).isEmpty();
            case ATTUNEMENT -> canAttune(item);
            case RECALIBRATION -> canRecalibrate(item, definition);
            case SUBSTITUTION -> canSubstitute(item, definition);
            case TRANSPOSITION -> canTranspose(definition, allowedUniques);
            case FIXATION -> item.explicits().size() >= MIN_FIXATION_MODIFIERS && !item.hasLockedModifier();
            case REPLICATION -> true;
        };
    }

    public static Item use(CraftingCurrency currency, Item item, CraftingMaterials materials, ItemStore store, Random random,
                                 Predicate<ItemDefinition> allowedUniques) {
        if (materials.count(currency.itemId()) < 1 || !canUse(currency, item, allowedUniques)) {
            return null;
        }
        Item craftedItem = switch (currency) {
            case AUGMENTATION -> augment(item, random) == null ? null : item;
            case ATTUNEMENT -> attune(item, random);
            case RECALIBRATION -> recalibrate(item, random);
            case SUBSTITUTION -> substitute(item, random);
            case TRANSPOSITION -> transpose(store, item, random, allowedUniques);
            case FIXATION -> fixate(item, random);
            case REPLICATION -> store.addMirrorOf(item);
        };
        if (craftedItem != null) {
            materials.take(currency.itemId(), 1);
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
            PoolEntry poolEntry = rolled.locked() ? null : definition.poolEntryFor(rolled.modifierId());
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
            PoolEntry poolEntry = rolled.locked() ? null : definition.poolEntryFor(rolled.modifierId());
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

    private static boolean canAttune(Item item) {
        ItemKind kind = item.kind();
        if (kind == null) {
            return false;
        }
        for (RolledModifier implicit : item.implicits()) {
            for (RolledEffect line : implicit.lines()) {
                ModifierLine lineDefinition = ItemImplicits.line(kind, line.effectName());
                if (lineDefinition != null && lineDefinition.canVary()) {
                    return true;
                }
            }
        }
        return false;
    }

    static Item attune(Item item, Random random) {
        ItemKind kind = item.kind();
        if (kind == null) {
            return null;
        }
        for (int attempt = 0; attempt < MAX_RECALIBRATION_ATTEMPTS; attempt++) {
            List<RolledModifier> rerolled = new ArrayList<>(item.implicits().size());
            for (RolledModifier implicit : item.implicits()) {
                List<RolledEffect> rerolledLines = new ArrayList<>(implicit.lines().size());
                for (RolledEffect line : implicit.lines()) {
                    ModifierLine lineDefinition = ItemImplicits.line(kind, line.effectName());
                    rerolledLines.add(lineDefinition == null ? line : new RolledEffect(line.effectName(), lineDefinition.roll(random)));
                }
                rerolled.add(implicit.withLines(rerolledLines));
            }
            if (!rerolled.equals(item.implicits())) {
                item.replaceImplicits(rerolled);
                return item;
            }
        }
        return null;
    }

    private static boolean canSubstitute(Item item, ItemDefinition definition) {
        return !definition.unique() && !removableForSubstitution(item, definition).isEmpty();
    }

    private static List<RolledModifier> removableForSubstitution(Item item, ItemDefinition definition) {
        return item.explicits().stream()
                .filter(rolled -> !rolled.locked() && !replacementSides(definition, item.explicits(), rolled).isEmpty()).toList();
    }

    private static List<List<PoolEntry>> replacementSides(ItemDefinition definition, List<RolledModifier> modifiers, RolledModifier removed) {
        List<RolledModifier> remaining = new ArrayList<>(modifiers);
        remaining.remove(removed);
        return AffixLayout.openSides(definition, remaining).stream()
                .map(side -> side.stream().filter(entry -> entry.slot() == removed.slot() && !entry.modifier().id().equals(removed.modifierId()))
                        .toList())
                .filter(side -> !side.isEmpty()).toList();
    }

    static Item substitute(Item item, Random random) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique()) {
            return null;
        }
        List<RolledModifier> removable = removableForSubstitution(item, definition);
        if (removable.isEmpty()) {
            return null;
        }
        RolledModifier removed = removable.get(random.nextInt(removable.size()));
        List<List<PoolEntry>> openSides = replacementSides(definition, item.explicits(), removed);
        List<PoolEntry> chosenSide = openSides.get(random.nextInt(openSides.size()));
        RolledModifier addedModifier = RolledModifier.roll(WeightedPick.pick(chosenSide, PoolEntry::weight, random).modifier(), random, false);
        List<RolledModifier> remaining = new ArrayList<>(item.explicits());
        remaining.remove(removed);
        remaining.add(AffixLayout.insertIndex(remaining, addedModifier.slot()), addedModifier);
        item.replaceExplicits(remaining);
        item.refreezeName();
        return item;
    }

    static Item fixate(Item item, Random random) {
        if (item.explicits().size() < MIN_FIXATION_MODIFIERS || item.hasLockedModifier()) {
            return null;
        }
        List<RolledModifier> modifiers = new ArrayList<>(item.explicits());
        int lockedIndex = random.nextInt(modifiers.size());
        modifiers.set(lockedIndex, modifiers.get(lockedIndex).asLocked());
        item.replaceExplicits(modifiers);
        return item;
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
