package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.tags.ThemeTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

final class InfusedCrafting {

    private InfusedCrafting() {
    }

    static boolean hasTag(Modifier modifier, String tag) {
        return modifier != null && ThemeTags.allWithAncestors(modifier.tags()).contains(tag);
    }

    private static boolean hasTag(Item item, RolledModifier rolled, String tag) {
        return hasTag(item.explicitModifier(rolled.modifierId()), tag);
    }

    private static boolean staysOnReforge(RolledModifier rolled) {
        return rolled.immutable() || ModifierSources.isReserved(rolled.modifierId());
    }

    private static boolean guarded(RolledModifier rolled, Set<ModifierSlot> inhibited) {
        return staysOnReforge(rolled) || inhibited.contains(rolled.slot());
    }

    private static List<RolledModifier> keptOnReforge(Item item, Set<ModifierSlot> inhibited) {
        return item.explicits().stream().filter(rolled -> guarded(rolled, inhibited)).toList();
    }

    static List<RolledModifier> changedByReforge(Item item) {
        return item.explicits().stream().filter(rolled -> !staysOnReforge(rolled)).toList();
    }

    private static List<List<PoolEntry>> sidesWhere(ItemDefinition definition, List<RolledModifier> modifiers, int tier, Predicate<PoolEntry> wanted) {
        return AffixLayout.openSides(definition, modifiers, tier).stream()
                .map(side -> side.stream().filter(wanted).toList())
                .filter(side -> !side.isEmpty()).toList();
    }

    private static Predicate<PoolEntry> tagged(String tag) {
        return entry -> hasTag(entry.modifier(), tag);
    }

    static boolean canReforge(Item item, String tag, Set<ModifierSlot> inhibited) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique()) {
            return false;
        }
        return !sidesWhere(definition, keptOnReforge(item, inhibited), item.tier(), tagged(tag)).isEmpty();
    }

    static boolean reforgeWithTag(Item item, String tag, Set<ModifierSlot> inhibited, Random random) {
        if (!canReforge(item, tag, inhibited)) {
            return false;
        }
        ItemDefinition definition = item.definition();
        List<RolledModifier> modifiers = new ArrayList<>(keptOnReforge(item, inhibited));
        int modifierCount = Math.max(AffixLayout.rolledModifierCount(AffixLayout.limits(definition.type()), random.nextFloat()), modifiers.size() + 1);
        AffixLayout.insert(modifiers, AffixLayout.rollFrom(sidesWhere(definition, modifiers, item.tier(), tagged(tag)), random, PoolEntry::weight));
        while (modifiers.size() < modifierCount) {
            List<List<PoolEntry>> openSides = AffixLayout.openSides(definition, modifiers, item.tier());
            if (openSides.isEmpty()) {
                break;
            }
            AffixLayout.insert(modifiers, AffixLayout.rollFrom(openSides, random, PoolEntry::weight));
        }
        item.replaceExplicits(modifiers);
        item.refreezeName();
        return true;
    }

    static List<RolledModifier> removableForTag(Item item, String tag, Set<ModifierSlot> inhibited) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique()) {
            return List.of();
        }
        return item.explicits().stream().filter(rolled -> !guarded(rolled, inhibited) && !hasTag(item, rolled, tag)
                && !sidesWhere(definition, without(item.explicits(), rolled), item.tier(), tagged(tag)).isEmpty()).toList();
    }

    static boolean addAndRemove(Item item, String tag, Set<ModifierSlot> inhibited, Random random) {
        List<RolledModifier> removable = removableForTag(item, tag, inhibited);
        if (removable.isEmpty()) {
            return false;
        }
        RolledModifier removed = removable.get(random.nextInt(removable.size()));
        List<List<PoolEntry>> taggedSides = sidesWhere(item.definition(), without(item.explicits(), removed), item.tier(), tagged(tag));
        item.swapExplicit(removed, AffixLayout.rollFrom(taggedSides, random, PoolEntry::weight));
        return true;
    }

    private static List<RolledModifier> without(List<RolledModifier> modifiers, RolledModifier removed) {
        List<RolledModifier> remaining = new ArrayList<>(modifiers);
        remaining.remove(removed);
        return remaining;
    }
}
