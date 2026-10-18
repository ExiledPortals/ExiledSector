package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AffixLayout {

    static final int COMMON_MAX_EFFECTS = 2;
    static final int MAX_EFFECTS = 4;
    static final int MAX_PER_SIDE = 2;
    static final float ONE_EFFECT_CHANCE = 0.35f;
    static final float TWO_EFFECTS_CHANCE = 0.2f;
    static final float THREE_EFFECTS_CHANCE = 0.35f;

    private AffixLayout() {
    }

    static ItemRarity rarityFor(ItemDefinition definition, int effectCount) {
        if (definition != null && definition.unique()) {
            return ItemRarity.UNIQUE;
        }
        return effectCount <= COMMON_MAX_EFFECTS ? ItemRarity.COMMON : ItemRarity.RARE;
    }

    static int rolledEffectCount(float roll) {
        if (roll < ONE_EFFECT_CHANCE) {
            return 1;
        }
        float upToTwo = ONE_EFFECT_CHANCE + TWO_EFFECTS_CHANCE;
        if (roll < upToTwo) {
            return 2;
        }
        return roll < upToTwo + THREE_EFFECTS_CHANCE ? 3 : 4;
    }

    static int prefixCount(int effectCount, boolean morePrefixes) {
        int evenShare = effectCount / 2;
        if (effectCount % 2 == 0 || !morePrefixes) {
            return evenShare;
        }
        return evenShare + 1;
    }

    static List<List<PoolEntry>> openSides(ItemDefinition definition, List<RolledModifier> modifiers) {
        if (definition.unique() || modifiers.size() >= MAX_EFFECTS) {
            return List.of();
        }
        Set<String> presentModifiers = new HashSet<>();
        int takenPrefixes = 0;
        int takenSuffixes = 0;
        for (RolledModifier modifier : modifiers) {
            presentModifiers.add(modifier.modifierId());
            if (modifier.slot() == ModifierSlot.PREFIX) {
                takenPrefixes++;
            } else {
                takenSuffixes++;
            }
        }
        List<List<PoolEntry>> sides = new ArrayList<>();
        addOpenSide(sides, definition.prefixes(), takenPrefixes, presentModifiers);
        addOpenSide(sides, definition.suffixes(), takenSuffixes, presentModifiers);
        return sides;
    }

    static int insertIndex(List<RolledModifier> modifiers, ModifierSlot addedSlot) {
        if (addedSlot != ModifierSlot.PREFIX) {
            return modifiers.size();
        }
        return (int) modifiers.stream().filter(modifier -> modifier.slot() == ModifierSlot.PREFIX).count();
    }

    private static void addOpenSide(List<List<PoolEntry>> sides, List<PoolEntry> sidePool, int taken, Set<String> presentModifiers) {
        if (taken >= MAX_PER_SIDE) {
            return;
        }
        List<PoolEntry> openEntries = sidePool.stream().filter(entry -> !presentModifiers.contains(entry.modifier().id())).toList();
        if (!openEntries.isEmpty()) {
            sides.add(openEntries);
        }
    }
}
