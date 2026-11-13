package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.TierRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class GrantedModifiers {

    private GrantedModifiers() {
    }

    public static boolean canGrant(ItemDefinition definition, String modifierId) {
        Modifier granted = ItemModifiers.get(modifierId);
        return definition != null && granted != null && !definition.unique() && !definition.keepsOneModifier()
                && granted.slot() != ModifierSlot.IMPLICIT && ItemModifiers.itemTypes(modifierId).contains(definition.type())
                && AffixLayout.rollableAt(definition.type(), List.of(new PoolEntry(granted, 1f)), TierRange.NO_TIER).size() == 1;
    }

    public static ItemData grant(ItemData item, String modifierId, Random random) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (!canGrant(definition, modifierId)) {
            return item;
        }
        List<RolledModifier> explicits = Engram.reroll(definition, ItemModifiers.get(modifierId), List.of(),
                AffixLayout.dropLimits(definition.type()), random);
        return new ItemData(item.definitionId(), item.seed(), explicits, ItemNames.freeze(definition, item.seed(), explicits), item.traits());
    }

    public static ItemData add(ItemData item, String modifierId, AffixLayout.Limits limits, Random random) {
        return add(item, modifierId, false, limits, random);
    }

    public static ItemData addWarped(ItemData item, String modifierId, AffixLayout.Limits limits, Random random) {
        return add(item, modifierId, true, limits, random);
    }

    private static ItemData add(ItemData item, String modifierId, boolean warped, AffixLayout.Limits limits, Random random) {
        ItemDefinition definition = item == null ? null : item.definition();
        List<RolledModifier> present = presentModifiers(item);
        if (!canGrant(definition, modifierId) || present.stream().anyMatch(rolled -> rolled.modifierId().equals(modifierId))) {
            return item;
        }
        Modifier granted = ItemModifiers.get(modifierId);
        Set<String> grantedEffects = granted.lines().stream().map(ModifierLine::effectName).collect(Collectors.toSet());
        if (present.stream().anyMatch(rolled -> rolled.warped() && displaces(rolled, modifierId, grantedEffects))) {
            return item;
        }
        List<RolledModifier> kept = new ArrayList<>(present.stream()
                .filter(rolled -> rolled.lines().stream().noneMatch(line -> grantedEffects.contains(line.effectName())))
                .filter(rolled -> !ModifierExclusivity.clashes(modifierId, List.of(rolled.modifierId())))
                .toList());
        List<RolledModifier> sameSide = kept.stream().filter(rolled -> rolled.slot() == granted.slot()).toList();
        if (sameSide.size() >= limits.maxPerSide()) {
            kept.remove(makingWay(sameSide, random));
        } else if (kept.size() >= limits.maxModifiers()) {
            kept.remove(makingWay(kept, random));
        }
        RolledModifier added = RolledModifier.roll(granted, random);
        kept.add(warped ? added.asWarped() : added);
        List<RolledModifier> explicits = new ArrayList<>(kept.stream().filter(rolled -> rolled.slot() == ModifierSlot.PREFIX).toList());
        explicits.addAll(kept.stream().filter(rolled -> rolled.slot() != ModifierSlot.PREFIX).toList());
        return new ItemData(item.definitionId(), item.seed(), explicits, ItemNames.freeze(definition, item.seed(), explicits), item.traits());
    }

    private static boolean displaces(RolledModifier rolled, String modifierId, Set<String> grantedEffects) {
        return rolled.lines().stream().anyMatch(line -> grantedEffects.contains(line.effectName()))
                || ModifierExclusivity.clashes(modifierId, List.of(rolled.modifierId()));
    }

    private static RolledModifier makingWay(List<RolledModifier> candidates, Random random) {
        List<RolledModifier> replaceable = candidates.stream().filter(rolled -> !rolled.warped() && !ModifierSources.isReserved(rolled.modifierId())).toList();
        List<RolledModifier> pool = replaceable.isEmpty() ? candidates : replaceable;
        return pool.get(random.nextInt(pool.size()));
    }

    private static List<RolledModifier> presentModifiers(ItemData item) {
        if (item == null) {
            return List.of();
        }
        if (item.explicits() != null) {
            return item.explicits();
        }
        Item preview = item.preview();
        return preview == null ? List.of() : preview.explicits();
    }
}
