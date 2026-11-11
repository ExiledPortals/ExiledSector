package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.skills.tags.ThemeTags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

final class InfusedCrafting {

    static final float MORE_ALIKE_WEIGHT = 10f;
    static final float LESS_ALIKE_WEIGHT = 0.1f;

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

    private static List<RolledModifier> keptOnReforge(Item item) {
        return item.explicits().stream().filter(InfusedCrafting::staysOnReforge).toList();
    }

    private static List<List<PoolEntry>> sidesWhere(ItemDefinition definition, List<RolledModifier> modifiers, int tier, Predicate<PoolEntry> wanted) {
        return AffixLayout.openSides(definition, modifiers, tier).stream()
                .map(side -> side.stream().filter(wanted).toList())
                .filter(side -> !side.isEmpty()).toList();
    }

    private static Predicate<PoolEntry> tagged(String tag) {
        return entry -> hasTag(entry.modifier(), tag);
    }

    static boolean canReforge(Item item, String guaranteedTag) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique()) {
            return false;
        }
        Predicate<PoolEntry> wanted = guaranteedTag == null ? entry -> true : tagged(guaranteedTag);
        return !sidesWhere(definition, keptOnReforge(item), item.tier(), wanted).isEmpty();
    }

    static boolean reforgeWithTag(Item item, String tag, Random random) {
        return reforge(item, tag, PoolEntry::weight, random);
    }

    static boolean reforgeAlike(Item item, float previousModifierWeight, Random random) {
        Set<String> previousIds = new HashSet<>();
        item.explicits().stream().filter(rolled -> !staysOnReforge(rolled)).forEach(rolled -> previousIds.add(rolled.modifierId()));
        return reforge(item, null, entry -> previousIds.contains(entry.modifier().id()) ? entry.weight() * previousModifierWeight : entry.weight(), random);
    }

    private static boolean reforge(Item item, String guaranteedTag, ToDoubleFunction<PoolEntry> weightOf, Random random) {
        if (!canReforge(item, guaranteedTag)) {
            return false;
        }
        ItemDefinition definition = item.definition();
        List<RolledModifier> modifiers = new ArrayList<>(keptOnReforge(item));
        int modifierCount = Math.max(AffixLayout.rolledModifierCount(AffixLayout.limits(definition.type()), random.nextFloat()), modifiers.size() + 1);
        if (guaranteedTag != null) {
            AffixLayout.insert(modifiers, AffixLayout.rollFrom(sidesWhere(definition, modifiers, item.tier(), tagged(guaranteedTag)), random, weightOf));
        }
        while (modifiers.size() < modifierCount) {
            List<List<PoolEntry>> openSides = AffixLayout.openSides(definition, modifiers, item.tier());
            if (openSides.isEmpty()) {
                break;
            }
            AffixLayout.insert(modifiers, AffixLayout.rollFrom(openSides, random, weightOf));
        }
        item.replaceExplicits(modifiers);
        item.refreezeName();
        return true;
    }

    static List<RolledModifier> rerollableWithTag(Item item, String tag) {
        return item.explicits().stream().filter(rolled -> !rolled.immutable() && hasTag(item, rolled, tag) && ItemCrafting.canVary(item, rolled)).toList();
    }

    static boolean rerollTagValues(Item item, String tag, Random random) {
        List<RolledModifier> targets = rerollableWithTag(item, tag);
        if (targets.isEmpty()) {
            return false;
        }
        for (int attempt = 0; attempt < ItemCrafting.MAX_TUNING_ATTEMPTS; attempt++) {
            List<RolledModifier> rerolled = item.explicits().stream()
                    .map(rolled -> targets.contains(rolled) ? rolled.rerolled(item.explicitModifier(rolled.modifierId())::line, random) : rolled).toList();
            if (!rerolled.equals(item.explicits())) {
                item.replaceExplicits(rerolled);
                return true;
            }
        }
        return false;
    }

    static List<RolledModifier> swappable(Item item, String damageType, Set<String> damageTypes) {
        return item.explicits().stream().filter(rolled -> sibling(item, rolled, damageType, damageTypes) != null).toList();
    }

    static boolean swapDamageType(Item item, RolledModifier source, String damageType, Set<String> damageTypes) {
        Modifier sibling = sibling(item, source, damageType, damageTypes);
        if (sibling == null) {
            return false;
        }
        item.swapExplicit(source, atSameRollPosition(source, item.explicitModifier(source.modifierId()), sibling));
        return true;
    }

    private static Modifier sibling(Item item, RolledModifier source, String damageType, Set<String> damageTypes) {
        ItemDefinition definition = item.definition();
        Modifier sourceModifier = item.explicitModifier(source.modifierId());
        if (definition == null || sourceModifier == null || staysOnReforge(source)) {
            return null;
        }
        List<String> sourceTypes = sourceModifier.tags().stream().filter(damageTypes::contains).toList();
        if (sourceTypes.size() != 1 || sourceTypes.contains(damageType)) {
            return null;
        }
        Set<String> siblingTags = new LinkedHashSet<>(sourceModifier.tags());
        siblingTags.remove(sourceTypes.get(0));
        siblingTags.add(damageType);
        List<RolledModifier> remaining = new ArrayList<>(item.explicits());
        remaining.remove(source);
        return sidesWhere(definition, remaining, item.tier(), entry -> entry.slot() == source.slot()
                && entry.modifier().lines().size() == sourceModifier.lines().size() && entry.modifier().tags().equals(siblingTags))
                .stream().flatMap(List::stream).map(PoolEntry::modifier)
                .max(Comparator.comparingInt(candidate -> sharedEndingLength(candidate.id(), source.modifierId()))).orElse(null);
    }

    private static int sharedEndingLength(String first, String second) {
        int length = 0;
        while (length < first.length() && length < second.length()
                && first.charAt(first.length() - 1 - length) == second.charAt(second.length() - 1 - length)) {
            length++;
        }
        return length;
    }

    private static RolledModifier atSameRollPosition(RolledModifier source, Modifier sourceModifier, Modifier sibling) {
        List<RolledLine> lines = new ArrayList<>(sibling.lines().size());
        for (int index = 0; index < sibling.lines().size(); index++) {
            ModifierLine sourceLine = sourceModifier.lines().get(index);
            ModifierLine siblingLine = sibling.lines().get(index);
            float sourceSpan = sourceLine.max() - sourceLine.min();
            float rollPosition = sourceSpan == 0f ? 0f : (source.lines().get(index).magnitude() - sourceLine.min()) / sourceSpan;
            lines.add(new RolledLine(siblingLine.effectName(), siblingLine.min() + rollPosition * (siblingLine.max() - siblingLine.min())));
        }
        return new RolledModifier(sibling.id(), sibling.slot(), lines);
    }

    static List<RolledModifier> removableForTag(Item item, String tag) {
        ItemDefinition definition = item.definition();
        if (definition == null || definition.unique()) {
            return List.of();
        }
        return item.explicits().stream().filter(rolled -> !staysOnReforge(rolled) && !hasTag(item, rolled, tag)
                && !sidesWhere(definition, without(item.explicits(), rolled), item.tier(), tagged(tag)).isEmpty()).toList();
    }

    static boolean addAndRemove(Item item, String tag, Random random) {
        List<RolledModifier> removable = removableForTag(item, tag);
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
