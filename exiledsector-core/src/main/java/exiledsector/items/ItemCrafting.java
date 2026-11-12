package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.TagWeights;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

public final class ItemCrafting {

    static final int MAX_TUNING_ATTEMPTS = 64;
    static final int MIN_IMMUTABLE_MODIFIERS = 2;
    static final float CORRUPTION_RANGE_STRETCH = 0.35f;
    static final int CORRUPTION_REROLL_ATTEMPTS = 16;
    static final int RARE_BASE_MIN_MODIFIERS = 3;
    static final int RARE_BASE_MAX_MODIFIERS = 4;

    private ItemCrafting() {
    }

    public static int feedstock(CraftingMaterials materials) {
        return materials.count(ItemDisassembly.FEEDSTOCK_ITEM_ID);
    }

    public static boolean grow(GrowableMaterial material, CraftingMaterials materials) {
        if (!material.growable() || !materials.take(ItemDisassembly.FEEDSTOCK_ITEM_ID, material.feedstockCost())) {
            return false;
        }
        materials.add(material.itemId(), 1);
        return true;
    }

    public static boolean canUse(CraftingMaterialFamily craftingItem, Item item, Predicate<ItemDefinition> allowedUniques) {
        return canUse(craftingItem, item, allowedUniques, Set.of());
    }

    public static boolean canUse(CraftingMaterialFamily craftingItem, Item item, Predicate<ItemDefinition> allowedUniques,
                                 Collection<CraftingInhibitor> inhibitors) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || !ItemTypes.takesCraftingItem(definition.type()) || item.mirrored() || item.corrupted() || isLockedFromCraftingItem(definition)
                || !CraftingMaterialFamilies.appliesTo(craftingItem, definition.type())) {
            return false;
        }
        return craftingItem.canUse(item, CraftingContext.withInhibitors(null, allowedUniques, inhibitors));
    }

    public static UseVerdict verdict(CraftingMaterialFamily craftingItem, Item item, Predicate<ItemDefinition> allowedUniques, Collection<CraftingInhibitor> inhibitors) {
        if (canUse(craftingItem, item, allowedUniques, inhibitors)) {
            return UseVerdict.ALLOW;
        }
        if (canUse(craftingItem, item, allowedUniques)) {
            return UseVerdict.refused(craftingItem == Nanoswarm.METAMORPH
                    ? "ui.craftingBench.notice.inhibitorBlocksMetamorph" : "ui.craftingBench.notice.inhibitorBlocks");
        }
        if (item == null) {
            return UseVerdict.refused("ui.craftingBench.notice.loadFirst");
        }
        ItemDefinition definition = item.definition();
        if (item.mirrored()) {
            return UseVerdict.refused("ui.craftingBench.notice.mirrored");
        }
        if (item.corrupted()) {
            return UseVerdict.refused("ui.craftingBench.notice.corruptedItem");
        }
        if (isLockedFromCraftingItem(definition)) {
            return UseVerdict.refused("ui.craftingBench.notice.uniqueSubroutine");
        }
        if (craftingItem == Nanoswarm.IMMUTABLE && item.hasImmutableModifier()) {
            return UseVerdict.refused("ui.craftingBench.notice.alreadyImmutable");
        }
        if (craftingItem == Nanoswarm.METAMORPH && definition != null && definition.unique()) {
            return UseVerdict.refused("ui.craftingBench.notice.noOtherUnique");
        }
        String refusalKey = craftingItem.refusalNoticeKey(item);
        return UseVerdict.refused(refusalKey != null ? refusalKey : "ui.craftingBench.notice.cannotUse");
    }

    public static boolean isLockedFromCraftingItem(ItemDefinition definition) {
        return definition != null && ItemTypes.lockedFromCraftingItem(definition);
    }

    public static boolean inhibitorBlocksMetamorph(Item item, Set<ModifierSlot> inhibited) {
        return item != null && item.explicits().stream().anyMatch(rolled -> inhibited.contains(rolled.slot()));
    }

    public static Set<CraftingInhibitor> inhibitorsSpentBy(CraftingMaterialFamily craftingItem, Item item, Collection<CraftingInhibitor> inhibitors) {
        Set<CraftingInhibitor> spent = EnumSet.noneOf(CraftingInhibitor.class);
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null) {
            return spent;
        }
        if (craftingItem.addsModifiers()) {
            spent.addAll(CraftingInhibitor.favouring(inhibitors));
        }
        if (!craftingItem.respectsInhibitors()) {
            return spent;
        }
        for (CraftingInhibitor inhibitor : inhibitors) {
            if (item.explicits().stream().anyMatch(rolled -> rolled.slot() == inhibitor.inhibitedSlot()
                    && !rolled.immutable() && craftingItem.wouldChange(item, rolled))) {
                spent.add(inhibitor);
            }
        }
        return spent;
    }

    static boolean canVary(Item item, RolledModifier rolled) {
        Modifier modifier = item.explicitModifier(rolled.modifierId());
        return modifier != null && rolled.lines().stream().map(line -> modifier.line(line.effectName()))
                .anyMatch(line -> line != null && line.canVary());
    }

    public static Item use(CraftingMaterialFamily craftingItem, Item item, CraftingMaterials materials, ItemStore store, Random random,
                                 Predicate<ItemDefinition> allowedUniques) {
        return use(craftingItem, item, materials, store, random, allowedUniques, Set.of());
    }

    public static Item use(CraftingMaterialFamily craftingItem, Item item, CraftingMaterials materials, ItemStore store, Random random,
                           Predicate<ItemDefinition> allowedUniques, Collection<CraftingInhibitor> inhibitors) {
        return Consumables.use(new CraftingItemUse(craftingItem, store, random, allowedUniques, inhibitors), item, materials);
    }

    private record CraftingItemUse(CraftingMaterialFamily craftingItem, ItemStore store, Random random, Predicate<ItemDefinition> allowedUniques,
                               Collection<CraftingInhibitor> inhibitors) implements Consumable<Item, Item> {

        @Override
        public String itemId() {
            return craftingItem.itemId();
        }

        @Override
        public UseVerdict check(Item item) {
            return verdict(craftingItem, item, allowedUniques, inhibitors);
        }

        @Override
        public Item apply(Item item) {
            return craftingItem.use(item, CraftingContext.withInhibitors(store, allowedUniques, inhibitors), random);
        }

        @Override
        public Collection<String> requiredItemIds() {
            return inhibitors.stream().map(CraftingInhibitor::itemId).toList();
        }

        @Override
        public Collection<String> spentAlongside(Item item) {
            return inhibitorsSpentBy(craftingItem, item, inhibitors).stream().map(CraftingInhibitor::itemId).toList();
        }
    }

    private static boolean guards(RolledModifier rolled, Set<ModifierSlot> inhibited) {
        return rolled.immutable() || inhibited.contains(rolled.slot());
    }

    static boolean canAugment(Item item) {
        ItemDefinition definition = item.definition();
        return definition != null && !AffixLayout.openSides(definition, item.explicits(), item.tier()).isEmpty();
    }

    static RolledModifier augment(Item item, Random random) {
        return augment(item, random, TagWeights.NONE);
    }

    static RolledModifier augment(Item item, Random random, TagWeights favouredTags) {
        ItemDefinition definition = item.definition();
        List<List<PoolEntry>> openSides = definition == null ? List.of() : AffixLayout.openSides(definition, item.explicits(), item.tier());
        if (definition == null || openSides.isEmpty()) {
            return null;
        }
        RolledModifier addedModifier = AffixLayout.rollFrom(openSides, random, favouredTags);
        item.addExplicit(addedModifier);
        return addedModifier;
    }

    static boolean canTune(Item item, ItemDefinition definition, Set<ModifierSlot> inhibited) {
        return definition != null && (item.hasTunableTrait() || item.explicits().stream().anyMatch(rolled -> !guards(rolled, inhibited) && canVary(item, rolled)));
    }

    static Item tune(Item item, Random random) {
        return tune(item, random, Set.of());
    }

    static Item tune(Item item, Random random, Set<ModifierSlot> inhibited) {
        ItemDefinition definition = item.definition();
        if (definition == null) {
            return null;
        }
        boolean traitTuned = item.tuneTrait(random);
        for (int attempt = 0; attempt < MAX_TUNING_ATTEMPTS; attempt++) {
            List<RolledModifier> rerolled = reroll(item, random, inhibited);
            if (!rerolled.equals(item.explicits())) {
                item.replaceExplicits(rerolled);
                return item;
            }
        }
        return traitTuned ? item : null;
    }

    private static List<RolledModifier> reroll(Item item, Random random, Set<ModifierSlot> inhibited) {
        List<RolledModifier> rerolled = new ArrayList<>(item.explicits().size());
        for (RolledModifier rolled : item.explicits()) {
            Modifier modifier = guards(rolled, inhibited) ? null : item.explicitModifier(rolled.modifierId());
            rerolled.add(modifier == null ? rolled : rolled.rerolled(modifier::line, random));
        }
        return rerolled;
    }

    static boolean canRerollFundamentals(Item item) {
        return item.implicits().stream().anyMatch(item::implicitCanVary);
    }

    static Item rerollFundamentals(Item item, Random random) {
        if (item.implicits().isEmpty()) {
            return null;
        }
        for (int attempt = 0; attempt < MAX_TUNING_ATTEMPTS; attempt++) {
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

    static boolean canReshape(Item item, ItemDefinition definition, Set<ModifierSlot> inhibited) {
        return definition != null && !definition.unique() && !removableForReshaping(item, definition, inhibited).isEmpty();
    }

    static boolean canReshapeAway(Item item, ItemDefinition definition, RolledModifier rolled) {
        return definition != null && !definition.unique() && !replacementSides(definition, item.explicits(), rolled, item.tier()).isEmpty();
    }

    private static List<RolledModifier> removableForReshaping(Item item, ItemDefinition definition, Set<ModifierSlot> inhibited) {
        return item.explicits().stream()
                .filter(rolled -> !guards(rolled, inhibited) && !replacementSides(definition, item.explicits(), rolled, item.tier()).isEmpty()).toList();
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

    static Item reshape(Item item, Random random) {
        return reshape(item, random, Set.of());
    }

    static Item reshape(Item item, Random random, Set<ModifierSlot> inhibited) {
        return reshape(item, random, inhibited, TagWeights.NONE);
    }

    static Item reshape(Item item, Random random, Set<ModifierSlot> inhibited, TagWeights favouredTags) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique()) {
            return null;
        }
        List<RolledModifier> removable = removableForReshaping(item, definition, inhibited);
        if (removable.isEmpty()) {
            return null;
        }
        RolledModifier removed = removable.get(random.nextInt(removable.size()));
        List<List<PoolEntry>> openSides = replacementSides(definition, item.explicits(), removed, item.tier());
        item.swapExplicit(removed, AffixLayout.rollFrom(openSides, random, favouredTags));
        return item;
    }

    static boolean canAnnul(Item item, Set<ModifierSlot> inhibited) {
        ItemDefinition definition = item.definition();
        return definition != null && !definition.unique() && !annullable(item.explicits(), inhibited).isEmpty();
    }

    static Item annul(Item item, Random random) {
        return annul(item, random, Set.of());
    }

    static Item annul(Item item, Random random, Set<ModifierSlot> inhibited) {
        if (!canAnnul(item, inhibited)) {
            return null;
        }
        List<RolledModifier> removable = annullable(item.explicits(), inhibited);
        item.removeExplicit(removable.get(random.nextInt(removable.size())));
        return item;
    }

    private static List<RolledModifier> annullable(List<RolledModifier> modifiers, Set<ModifierSlot> inhibited) {
        return modifiers.stream().filter(rolled -> !guards(rolled, inhibited)).toList();
    }

    static boolean canMakeImmutable(Item item, Set<ModifierSlot> inhibited) {
        return item.explicits().size() >= MIN_IMMUTABLE_MODIFIERS && !item.hasImmutableModifier() && !immutableCandidates(item.explicits(), inhibited).isEmpty();
    }

    static Item makeImmutable(Item item, Random random) {
        return makeImmutable(item, random, Set.of());
    }

    static Item makeImmutable(Item item, Random random, Set<ModifierSlot> inhibited) {
        List<Integer> candidates = immutableCandidates(item.explicits(), inhibited);
        if (item.explicits().size() < MIN_IMMUTABLE_MODIFIERS || item.hasImmutableModifier() || candidates.isEmpty()) {
            return null;
        }
        List<RolledModifier> modifiers = new ArrayList<>(item.explicits());
        int lockedIndex = candidates.get(random.nextInt(candidates.size()));
        modifiers.set(lockedIndex, modifiers.get(lockedIndex).asImmutable());
        item.replaceExplicits(modifiers);
        return item;
    }

    private static List<Integer> immutableCandidates(List<RolledModifier> modifiers, Set<ModifierSlot> inhibited) {
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < modifiers.size(); index++) {
            if (!inhibited.contains(modifiers.get(index).slot())) {
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
            case TIER_RAISED, MODIFIERS_EXPANDED -> ItemTypes.corrupt(outcome, item, random);
        };
        if (corrupted == null) {
            corrupted = item;
            outcome = CorruptionOutcome.NOTHING;
        }
        corrupted.corrupt(outcome);
        return corrupted;
    }

    static List<CorruptionOutcome> corruptionOutcomes(Item item) {
        ItemTypeSupport support = ItemTypes.of(item.type());
        return support == null ? CorruptionOutcome.NANOSWARM_OUTCOMES : support.corruptionOutcomes(item);
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
        return new RolledModifier(chosen.id(), ModifierSlot.IMPLICIT, RolledModifier.roll(chosen, random).lines());
    }

    static Item reforgeAsRareBase(ItemStore store, Item item, Random random) {
        ItemDefinition uniqueDefinition = item.definition();
        if (uniqueDefinition == null || !uniqueDefinition.unique()) {
            return null;
        }
        List<ItemDefinition> bases = ItemDefinitions.ofType(item.type()).stream()
                .filter(definition -> definition.kind() == uniqueDefinition.kind() && !definition.unique() && definition.rarity() > 0f).toList();
        ItemDefinition base = WeightedPick.pick(bases, ItemDefinition::rarity, random);
        List<Integer> modifierCounts = base == null ? List.of() : rareBaseModifierCounts(base);
        if (modifierCounts.isEmpty()) {
            return null;
        }
        long seed = random.nextLong();
        int modifierCount = modifierCounts.get(random.nextInt(modifierCounts.size()));
        Item replacement = Item.create(item.id(), base.id(), seed, ItemRoller.rollWithModifierCount(base, seed, modifierCount));
        replacement.replaceImplicits(item.implicits());
        replacement.copyTraitsFrom(item);
        replacement.freezeName();
        return store.replace(item, replacement) ? replacement : null;
    }

    private static List<Integer> rareBaseModifierCounts(ItemDefinition base) {
        List<Integer> counts = new ArrayList<>();
        for (int count = RARE_BASE_MIN_MODIFIERS; count <= RARE_BASE_MAX_MODIFIERS; count++) {
            if (ItemRoller.canRollModifierCount(base, count)) {
                counts.add(count);
            }
        }
        return counts;
    }

    static Item randomiseValues(Item item, Random random) {
        if (item.explicits().isEmpty() && item.implicits().isEmpty()) {
            return null;
        }
        List<RolledModifier> explicits = new ArrayList<>(item.explicits().size());
        for (RolledModifier rolled : item.explicits()) {
            explicits.add(stretchedReroll(rolled, rolled.immutable() ? null : item.explicitModifier(rolled.modifierId()), random));
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
        return modifier == null ? rolled : rolled.rerolled(effectName -> stretchedForCorruption(modifier.line(effectName)), random);
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
            return canMetamorph(definition, item.tier(), allowedUniques) ? metamorph(store, item, random, allowedUniques) : null;
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
        item.replaceExplicits(keepImmutable(item.explicits(), best, AffixLayout.limits(definition.type()).maxPerSide(), random));
        item.refreezeName();
        return item;
    }

    private static List<RolledModifier> keepImmutable(List<RolledModifier> previous, List<RolledModifier> rerolled, int maxPerSide, Random random) {
        RolledModifier locked = previous.stream().filter(RolledModifier::immutable).findFirst().orElse(null);
        if (locked == null) {
            return rerolled;
        }
        Set<String> lockedEffects = new HashSet<>();
        locked.lines().forEach(line -> lockedEffects.add(line.effectName()));
        List<RolledModifier> merged = new ArrayList<>(rerolled.stream().filter(rolled -> !rolled.modifierId().equals(locked.modifierId()))
                .filter(rolled -> rolled.lines().stream().noneMatch(line -> lockedEffects.contains(line.effectName())))
                .filter(rolled -> !ModifierExclusivity.clashes(rolled.modifierId(), List.of(locked.modifierId()))).toList());
        List<RolledModifier> sameSide = merged.stream().filter(rolled -> rolled.slot() == locked.slot()).toList();
        if (sameSide.size() >= maxPerSide) {
            merged.remove(sameSide.get(random.nextInt(sameSide.size())));
        }
        merged.add(AffixLayout.insertIndex(merged, locked.slot()), locked);
        return merged;
    }

    static boolean canMetamorph(ItemDefinition currentDefinition, int tier, Predicate<ItemDefinition> allowedUniques) {
        if (!currentDefinition.unique()) {
            return false;
        }
        for (ItemDefinition definition : ItemDefinitions.all()) {
            if (otherUnique(definition, currentDefinition, tier, allowedUniques)) {
                return true;
            }
        }
        return false;
    }

    static Item metamorph(ItemStore store, Item item, Random random, Predicate<ItemDefinition> allowedUniques) {
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

    private static boolean otherUnique(ItemDefinition definition, ItemDefinition currentDefinition, int tier,
                                       Predicate<ItemDefinition> allowedUniques) {
        boolean tierAllowed = ItemTypes.rollsAtTier(definition, tier);
        return definition.unique() && definition.type() == currentDefinition.type() && !isLockedFromCraftingItem(definition)
                && !definition.id().equals(currentDefinition.id()) && definition.rarity() > 0f && tierAllowed && allowedUniques.test(definition);
    }
}
