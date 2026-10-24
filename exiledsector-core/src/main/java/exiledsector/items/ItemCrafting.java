package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class ItemCrafting {

    static final int MAX_RECALIBRATION_ATTEMPTS = 64;
    static final int MIN_FIXATION_MODIFIERS = 2;
    static final float CORRUPTION_RANGE_STRETCH = 0.35f;
    static final int CORRUPTION_REROLL_ATTEMPTS = 16;
    static final int RARE_BASE_MIN_MODIFIERS = 3;
    static final int RARE_BASE_MAX_MODIFIERS = 4;

    private ItemCrafting() {
    }

    public static int parts(CraftingMaterials materials) {
        return materials.count(ItemDisassembly.PARTS_ITEM_ID);
    }

    public static boolean synthesise(ItemCurrency currency, CraftingMaterials materials) {
        if (!currency.synthesisable() || !materials.take(ItemDisassembly.PARTS_ITEM_ID, currency.partsCost())) {
            return false;
        }
        materials.add(currency.itemId(), 1);
        return true;
    }

    public static boolean canUse(ItemCurrency currency, Item item, Predicate<ItemDefinition> allowedUniques) {
        return canUse(currency, item, allowedUniques, Set.of());
    }

    public static boolean canUse(ItemCurrency currency, Item item, Predicate<ItemDefinition> allowedUniques,
                                 Collection<CraftingWard> wards) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || !takesCurrency(definition.type()) || item.mirrored() || item.corrupted() || isLockedFromCurrency(definition)
                || !ItemCurrencies.appliesTo(currency, definition.type())) {
            return false;
        }
        return currency.canUse(item, new CraftingContext(null, allowedUniques, CraftingWard.guardedSlots(wards)));
    }

    public static boolean takesCurrency(ItemType type) {
        return type != null && type != ItemType.WORMHOLE_ANCHOR;
    }

    public static boolean isLockedFromCurrency(ItemDefinition definition) {
        return definition != null && ItemTypes.lockedFromCurrency(definition);
    }

    public static boolean wardBlocksTransposition(Item item, Set<ModifierSlot> guarded) {
        return item != null && item.explicits().stream().anyMatch(rolled -> guarded.contains(rolled.slot()));
    }

    public static Set<CraftingWard> wardsSpentBy(ItemCurrency currency, Item item, Collection<CraftingWard> wards) {
        Set<CraftingWard> spent = EnumSet.noneOf(CraftingWard.class);
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || !currency.respectsWards()) {
            return spent;
        }
        for (CraftingWard ward : wards) {
            if (item.explicits().stream().anyMatch(rolled -> rolled.slot() == ward.guardedSlot()
                    && !rolled.locked() && currency.wouldChange(item, rolled))) {
                spent.add(ward);
            }
        }
        return spent;
    }

    static boolean canVary(RolledModifier rolled, ItemDefinition definition) {
        PoolEntry poolEntry = definition == null ? null : definition.poolEntryFor(rolled.modifierId());
        return poolEntry != null && rolled.lines().stream().map(line -> poolEntry.modifier().line(line.effectName()))
                .anyMatch(line -> line != null && line.canVary());
    }

    public static Item use(ItemCurrency currency, Item item, CraftingMaterials materials, ItemStore store, Random random,
                                 Predicate<ItemDefinition> allowedUniques) {
        return use(currency, item, materials, store, random, allowedUniques, Set.of());
    }

    public static Item use(ItemCurrency currency, Item item, CraftingMaterials materials, ItemStore store, Random random,
                           Predicate<ItemDefinition> allowedUniques, Collection<CraftingWard> wards) {
        if (materials.count(currency.itemId()) < 1 || !canUse(currency, item, allowedUniques, wards)
                || wards.stream().anyMatch(ward -> materials.count(ward.itemId()) < 1)) {
            return null;
        }
        Set<CraftingWard> spentWards = wardsSpentBy(currency, item, wards);
        Item craftedItem = currency.use(item, new CraftingContext(store, allowedUniques, CraftingWard.guardedSlots(wards)), random);
        if (craftedItem != null) {
            materials.take(currency.itemId(), 1);
            spentWards.forEach(ward -> materials.take(ward.itemId(), 1));
        }
        return craftedItem;
    }

    private static boolean guards(RolledModifier rolled, Set<ModifierSlot> guarded) {
        return rolled.locked() || guarded.contains(rolled.slot());
    }

    static boolean canAugment(Item item) {
        ItemDefinition definition = item.definition();
        return definition != null && !AffixLayout.openSides(definition, item.explicits(), item.tier()).isEmpty();
    }

    static RolledModifier augment(Item item, Random random) {
        ItemDefinition definition = item.definition();
        List<List<PoolEntry>> openSides = definition == null ? List.of() : AffixLayout.openSides(definition, item.explicits(), item.tier());
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

    static boolean canRecalibrate(Item item, ItemDefinition definition, Set<ModifierSlot> guarded) {
        return item.explicits().stream().anyMatch(rolled -> !guards(rolled, guarded) && canVary(rolled, definition));
    }

    static Item recalibrate(Item item, Random random) {
        return recalibrate(item, random, Set.of());
    }

    static Item recalibrate(Item item, Random random, Set<ModifierSlot> guarded) {
        ItemDefinition definition = item.definition();
        if (definition == null) {
            return null;
        }
        for (int attempt = 0; attempt < MAX_RECALIBRATION_ATTEMPTS; attempt++) {
            List<RolledModifier> rerolled = reroll(item.explicits(), definition, random, guarded);
            if (!rerolled.equals(item.explicits())) {
                item.replaceExplicits(rerolled);
                return item;
            }
        }
        return null;
    }

    private static List<RolledModifier> reroll(List<RolledModifier> modifiers, ItemDefinition definition, Random random,
                                               Set<ModifierSlot> guarded) {
        List<RolledModifier> rerolled = new ArrayList<>(modifiers.size());
        for (RolledModifier rolled : modifiers) {
            PoolEntry poolEntry = guards(rolled, guarded) ? null : definition.poolEntryFor(rolled.modifierId());
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

    static boolean canAttune(Item item) {
        return item.implicits().stream().anyMatch(item::implicitCanVary);
    }

    static Item attune(Item item, Random random) {
        if (item.implicits().isEmpty()) {
            return null;
        }
        for (int attempt = 0; attempt < MAX_RECALIBRATION_ATTEMPTS; attempt++) {
            List<RolledModifier> rerolled = new ArrayList<>(item.implicits().size());
            for (RolledModifier implicit : item.implicits()) {
                rerolled.add(item.rerolledImplicit(implicit, random));
            }
            if (!rerolled.equals(item.implicits())) {
                item.replaceImplicits(rerolled);
                return item;
            }
        }
        return null;
    }

    static boolean canSubstitute(Item item, ItemDefinition definition, Set<ModifierSlot> guarded) {
        return definition != null && !definition.unique() && !removableForSubstitution(item, definition, guarded).isEmpty();
    }

    static boolean canSubstituteAway(Item item, ItemDefinition definition, RolledModifier rolled) {
        return definition != null && !definition.unique() && !replacementSides(definition, item.explicits(), rolled, item.tier()).isEmpty();
    }

    private static List<RolledModifier> removableForSubstitution(Item item, ItemDefinition definition, Set<ModifierSlot> guarded) {
        return item.explicits().stream()
                .filter(rolled -> !guards(rolled, guarded) && !replacementSides(definition, item.explicits(), rolled, item.tier()).isEmpty()).toList();
    }

    private static List<List<PoolEntry>> replacementSides(ItemDefinition definition, List<RolledModifier> modifiers, RolledModifier removed,
                                                          int tier) {
        List<RolledModifier> remaining = new ArrayList<>(modifiers);
        remaining.remove(removed);
        return AffixLayout.openSides(definition, remaining, tier).stream()
                .map(side -> side.stream().filter(entry -> entry.slot() == removed.slot() && !entry.modifier().id().equals(removed.modifierId()))
                        .toList())
                .filter(side -> !side.isEmpty()).toList();
    }

    static Item substitute(Item item, Random random) {
        return substitute(item, random, Set.of());
    }

    static Item substitute(Item item, Random random, Set<ModifierSlot> guarded) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique()) {
            return null;
        }
        List<RolledModifier> removable = removableForSubstitution(item, definition, guarded);
        if (removable.isEmpty()) {
            return null;
        }
        RolledModifier removed = removable.get(random.nextInt(removable.size()));
        List<List<PoolEntry>> openSides = replacementSides(definition, item.explicits(), removed, item.tier());
        List<PoolEntry> chosenSide = openSides.get(random.nextInt(openSides.size()));
        RolledModifier addedModifier = RolledModifier.roll(WeightedPick.pick(chosenSide, PoolEntry::weight, random).modifier(), random, false);
        List<RolledModifier> remaining = new ArrayList<>(item.explicits());
        remaining.remove(removed);
        remaining.add(AffixLayout.insertIndex(remaining, addedModifier.slot()), addedModifier);
        item.replaceExplicits(remaining);
        item.refreezeName();
        return item;
    }

    static boolean canFixate(Item item, Set<ModifierSlot> guarded) {
        return item.explicits().size() >= MIN_FIXATION_MODIFIERS && !item.hasLockedModifier() && !fixable(item.explicits(), guarded).isEmpty();
    }

    static Item fixate(Item item, Random random) {
        return fixate(item, random, Set.of());
    }

    static Item fixate(Item item, Random random, Set<ModifierSlot> guarded) {
        List<Integer> candidates = fixable(item.explicits(), guarded);
        if (item.explicits().size() < MIN_FIXATION_MODIFIERS || item.hasLockedModifier() || candidates.isEmpty()) {
            return null;
        }
        List<RolledModifier> modifiers = new ArrayList<>(item.explicits());
        int lockedIndex = candidates.get(random.nextInt(candidates.size()));
        modifiers.set(lockedIndex, modifiers.get(lockedIndex).asLocked());
        item.replaceExplicits(modifiers);
        return item;
    }

    private static List<Integer> fixable(List<RolledModifier> modifiers, Set<ModifierSlot> guarded) {
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < modifiers.size(); index++) {
            if (!guarded.contains(modifiers.get(index).slot())) {
                candidates.add(index);
            }
        }
        return candidates;
    }

    static Item corrupt(ItemStore store, Item item, Random random, Predicate<ItemDefinition> allowedUniques) {
        if (item == null) {
            return null;
        }
        List<CorruptionOutcome> outcomes = corruptionOutcomes(item);
        CorruptionOutcome outcome = outcomes.get(random.nextInt(outcomes.size()));
        Item corrupted = switch (outcome) {
            case NOTHING, INNATE -> item;
            case IMPLICIT_REPLACED -> replaceImplicit(item, random) ? item : null;
            case IMPLICIT_ADDED -> addImplicit(item, random) ? item : null;
            case VALUES_RANDOMISED -> randomiseValues(item, random);
            case MODIFIERS_RANDOMISED -> randomiseModifiers(store, item, random, allowedUniques);
            case BECAME_RARE -> reforgeAsRareBase(store, item, random);
        };
        if (corrupted == null) {
            corrupted = item;
            outcome = CorruptionOutcome.NOTHING;
        }
        corrupted.corrupt(outcome);
        return corrupted;
    }

    static List<CorruptionOutcome> corruptionOutcomes(Item item) {
        if (item.type() == ItemType.SUBROUTINE) {
            return CorruptionOutcome.SUBROUTINE_OUTCOMES;
        }
        ItemDefinition definition = item.definition();
        boolean uniqueHullItem = item.type() == ItemType.HULL_ITEM && definition != null && definition.unique();
        return uniqueHullItem ? CorruptionOutcome.UNIQUE_HULL_ITEM_OUTCOMES : CorruptionOutcome.NANOSWARM_OUTCOMES;
    }

    static boolean replaceImplicit(Item item, Random random) {
        List<RolledModifier> implicits = new ArrayList<>(item.implicits());
        RolledModifier replacement = rollCorruptionImplicit(item, random);
        if (replacement == null) {
            return false;
        }
        if (implicits.isEmpty()) {
            implicits.add(replacement);
        } else {
            implicits.set(random.nextInt(implicits.size()), replacement);
        }
        item.replaceImplicits(implicits);
        return true;
    }

    static boolean addImplicit(Item item, Random random) {
        RolledModifier added = rollCorruptionImplicit(item, random);
        if (added == null) {
            return false;
        }
        List<RolledModifier> implicits = new ArrayList<>(item.implicits());
        implicits.add(added);
        item.replaceImplicits(implicits);
        return true;
    }

    private static RolledModifier rollCorruptionImplicit(Item item, Random random) {
        List<RolledModifier> implicits = item.implicits();
        List<Modifier> candidates = item.implicitCorruptionPool().stream()
                .filter(implicit -> implicits.stream().noneMatch(current -> current.modifierId().equals(implicit.id()))).toList();
        if (candidates.isEmpty()) {
            return null;
        }
        Modifier chosen = candidates.get(random.nextInt(candidates.size()));
        return new RolledModifier(chosen.id(), ModifierSlot.IMPLICIT, RolledModifier.roll(chosen, random, false).lines());
    }

    static Item reforgeAsRareBase(ItemStore store, Item item, Random random) {
        ItemDefinition uniqueDefinition = item.definition();
        if (item.type() != ItemType.HULL_ITEM || uniqueDefinition == null || !uniqueDefinition.unique()) {
            return null;
        }
        List<ItemDefinition> bases = ItemDefinitions.ofType(ItemType.HULL_ITEM).stream()
                .filter(definition -> definition.kind() == uniqueDefinition.kind() && !definition.unique() && definition.rarity() > 0f).toList();
        ItemDefinition base = WeightedPick.pick(bases, ItemDefinition::rarity, random);
        List<Integer> modifierCounts = base == null ? List.of() : rareBaseModifierCounts(base);
        if (modifierCounts.isEmpty()) {
            return null;
        }
        long seed = random.nextLong();
        int modifierCount = modifierCounts.get(random.nextInt(modifierCounts.size()));
        Item replacement = Item.create(item.id(), base.id(), seed, ItemRoller.rollWithEffectCount(base, seed, modifierCount));
        replacement.replaceImplicits(item.implicits());
        replacement.copyTraitsFrom(item);
        replacement.freezeName();
        return store.replace(item, replacement) ? replacement : null;
    }

    private static List<Integer> rareBaseModifierCounts(ItemDefinition base) {
        List<Integer> counts = new ArrayList<>();
        for (int count = RARE_BASE_MIN_MODIFIERS; count <= RARE_BASE_MAX_MODIFIERS; count++) {
            if (ItemRoller.canRollEffectCount(base, count)) {
                counts.add(count);
            }
        }
        return counts;
    }

    static Item randomiseValues(Item item, Random random) {
        if (item.explicits().isEmpty() && item.implicits().isEmpty()) {
            return null;
        }
        ItemDefinition definition = item.definition();
        List<RolledModifier> explicits = new ArrayList<>(item.explicits().size());
        for (RolledModifier rolled : item.explicits()) {
            PoolEntry poolEntry = rolled.locked() || definition == null ? null : definition.poolEntryFor(rolled.modifierId());
            explicits.add(stretchedReroll(rolled, poolEntry == null ? null : poolEntry.modifier(), random));
        }
        List<RolledModifier> implicits = new ArrayList<>(item.implicits().size());
        for (RolledModifier rolled : item.implicits()) {
            implicits.add(stretchedReroll(rolled, item.implicitModifier(rolled.modifierId()), random));
        }
        item.replaceExplicits(explicits);
        item.replaceImplicits(implicits);
        return item;
    }

    private static RolledModifier stretchedReroll(RolledModifier rolled, Modifier modifier, Random random) {
        if (modifier == null) {
            return rolled;
        }
        List<RolledEffect> lines = new ArrayList<>(rolled.lines().size());
        for (RolledEffect line : rolled.lines()) {
            ModifierLine stretchedLine = stretchedForCorruption(modifier.line(line.effectName()));
            lines.add(stretchedLine == null ? line : new RolledEffect(line.effectName(), stretchedLine.roll(random)));
        }
        return rolled.withLines(lines);
    }

    static ModifierLine stretchedForCorruption(ModifierLine lineDefinition) {
        if (lineDefinition == null || !lineDefinition.canVary()) {
            return null;
        }
        if (!lineDefinition.isScaled()) {
            return lineDefinition.widened(CORRUPTION_RANGE_STRETCH);
        }
        return lineDefinition.scalesWithTier() ? lineDefinition.stretchedRollPositions(CORRUPTION_RANGE_STRETCH) : null;
    }

    static Item randomiseModifiers(ItemStore store, Item item, Random random, Predicate<ItemDefinition> allowedUniques) {
        ItemDefinition definition = item.definition();
        if (definition == null) {
            return null;
        }
        if (definition.unique()) {
            return canTranspose(definition, item.tier(), allowedUniques) ? transpose(store, item, random, allowedUniques) : null;
        }
        if (definition.prefixes().isEmpty() && definition.suffixes().isEmpty()) {
            return null;
        }
        Set<String> previousIds = new HashSet<>();
        item.explicits().forEach(rolled -> previousIds.add(rolled.modifierId()));
        List<RolledModifier> best = null;
        long bestOverlap = Long.MAX_VALUE;
        for (int attempt = 0; attempt < CORRUPTION_REROLL_ATTEMPTS && bestOverlap > 0; attempt++) {
            List<RolledModifier> candidate = ItemRoller.rollAtTier(definition, random.nextLong(), item.tier());
            long overlap = candidate.stream().filter(rolled -> previousIds.contains(rolled.modifierId())).count();
            if (overlap < bestOverlap) {
                best = candidate;
                bestOverlap = overlap;
            }
        }
        if (best == null) {
            return null;
        }
        item.replaceExplicits(keepLocked(item.explicits(), best, AffixLayout.limits(definition.type()).maxPerSide(), random));
        item.refreezeName();
        return item;
    }

    private static List<RolledModifier> keepLocked(List<RolledModifier> previous, List<RolledModifier> rerolled, int maxPerSide, Random random) {
        RolledModifier locked = previous.stream().filter(RolledModifier::locked).findFirst().orElse(null);
        if (locked == null) {
            return rerolled;
        }
        List<RolledModifier> merged = new ArrayList<>(rerolled.stream().filter(rolled -> !rolled.modifierId().equals(locked.modifierId())).toList());
        List<RolledModifier> sameSide = merged.stream().filter(rolled -> rolled.slot() == locked.slot()).toList();
        if (sameSide.size() >= maxPerSide) {
            merged.remove(sameSide.get(random.nextInt(sameSide.size())));
        }
        merged.add(AffixLayout.insertIndex(merged, locked.slot()), locked);
        return merged;
    }

    static boolean canTranspose(ItemDefinition currentDefinition, int tier, Predicate<ItemDefinition> allowedUniques) {
        if (!currentDefinition.unique()) {
            return false;
        }
        for (ItemDefinition definition : transpositionCandidates(currentDefinition)) {
            if (otherUnique(definition, currentDefinition, tier, allowedUniques)) {
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
        Predicate<ItemDefinition> otherUniques = definition -> otherUnique(definition, currentDefinition, item.tier(), allowedUniques);
        ItemDefinition nextDefinition = ItemDefinitions.isOwnType(currentDefinition.type()) ? ItemDrops.pickOfType(random, currentDefinition.type(), otherUniques)
                : ItemDrops.pickUnique(random, otherUniques);
        if (nextDefinition == null) {
            return null;
        }
        long seed = random.nextLong();
        Item replacement = Item.create(item.id(), nextDefinition.id(), seed, ItemRoller.rollAtTier(nextDefinition, seed, item.tier()));
        replacement.copyTraitsFrom(item);
        replacement.freezeName();
        return store.replace(item, replacement) ? replacement : null;
    }

    private static Collection<ItemDefinition> transpositionCandidates(ItemDefinition currentDefinition) {
        return currentDefinition.type() == ItemType.STAR_CHART ? ItemDefinitions.ofType(ItemType.STAR_CHART) : ItemDefinitions.all();
    }

    private static boolean otherUnique(ItemDefinition definition, ItemDefinition currentDefinition, int tier,
                                       Predicate<ItemDefinition> allowedUniques) {
        boolean tierAllowed = ItemTypes.rollsAtTier(definition, tier);
        return definition.unique() && definition.type() == currentDefinition.type() && !isLockedFromCurrency(definition)
                && !definition.id().equals(currentDefinition.id()) && definition.rarity() > 0f && tierAllowed && allowedUniques.test(definition);
    }
}
