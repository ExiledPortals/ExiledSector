package exiledsector.items;

import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierEffects;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class WarpedModifiers {

    private WarpedModifiers() {
    }

    public static ItemData warp(ItemData item, Random random) {
        ItemDefinition definition = item == null ? null : item.definition();
        if (definition == null || definition.type() != ItemType.HULL_ITEM || definition.unique() || definition.kind() == null) {
            return item;
        }
        Item preview = item.preview();
        if (preview == null || preview.isWarped()) {
            return item;
        }
        List<RolledModifier> present = preview.explicits();
        Set<String> presentEffects = new HashSet<>();
        present.forEach(rolled -> rolled.lines().forEach(line -> presentEffects.add(line.effectName())));
        List<PoolEntry> pool = poolFor(definition).stream()
                .filter(entry -> entry.modifier().lines().stream().map(ModifierLine::effectName).noneMatch(presentEffects::contains))
                .filter(entry -> !ModifierExclusivity.clashesWithRolled(entry.modifier().id(), present))
                .toList();
        PoolEntry picked = WeightedPick.pick(pool, PoolEntry::weight, random);
        return picked == null ? item : GrantedModifiers.addWarped(item, picked.modifier().id(), AffixLayout.dropLimits(definition.type()), random);
    }

    public static List<PoolEntry> poolFor(ItemDefinition definition) {
        ItemKind part = definition == null ? null : definition.kind();
        if (part == null) {
            return List.of();
        }
        Set<String> ownEffects = effectsOf(part);
        Map<String, PoolEntry> warpable = new LinkedHashMap<>();
        for (ItemDefinition other : ItemDefinitions.all()) {
            if (other.kind() == null || other.kind() == part || !other.kind().fitsSection() || other.unique()) {
                continue;
            }
            for (PoolEntry entry : other.pool()) {
                Modifier modifier = entry.modifier();
                if (!warpable.containsKey(modifier.id()) && canWarp(definition, modifier, ownEffects)) {
                    warpable.put(modifier.id(), entry);
                }
            }
        }
        return List.copyOf(warpable.values());
    }

    static boolean partBound(Modifier modifier) {
        ModifierEffects effects = ItemEffects.forType(ItemType.HULL_ITEM);
        return modifier.lines().stream()
                .anyMatch(line -> effects.byName(line.effectName()) instanceof SkillEffect skillEffect && skillEffect.socketedItemScaling() != null);
    }

    private static boolean canWarp(ItemDefinition definition, Modifier modifier, Set<String> ownEffects) {
        return !ModifierSources.isReserved(modifier.id()) && !partBound(modifier)
                && modifier.lines().stream().map(ModifierLine::effectName).noneMatch(ownEffects::contains)
                && GrantedModifiers.canGrant(definition, modifier.id());
    }

    private static Set<String> effectsOf(ItemKind part) {
        Set<String> effects = new HashSet<>();
        for (ItemDefinition definition : ItemDefinitions.all()) {
            if (definition.kind() == part && !definition.unique()) {
                definition.pool().forEach(entry -> entry.modifier().lines().forEach(line -> effects.add(line.effectName())));
            }
        }
        return effects;
    }
}
