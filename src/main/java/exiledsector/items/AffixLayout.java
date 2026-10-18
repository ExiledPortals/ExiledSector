package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.TierRange;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

final class AffixLayout {

    static final int COMMON_MAX_EFFECTS = 2;
    static final int MAX_EFFECTS = 4;
    static final int MAX_PER_SIDE = 2;
    static final float ONE_EFFECT_CHANCE = 0.35f;
    static final float TWO_EFFECTS_CHANCE = 0.2f;
    static final float THREE_EFFECTS_CHANCE = 0.35f;
    static final Limits DEFAULT_LIMITS = new Limits(COMMON_MAX_EFFECTS, MAX_EFFECTS, MAX_PER_SIDE,
            List.of(ONE_EFFECT_CHANCE, TWO_EFFECTS_CHANCE, THREE_EFFECTS_CHANCE));
    private static final AtomicReference<Map<ItemType, Limits>> LIMITS_BY_TYPE = new AtomicReference<>(Map.of());

    record Limits(int commonMaxEffects, int maxEffects, int maxPerSide, List<Float> effectCountChances) {

        Limits {
            effectCountChances = List.copyOf(effectCountChances);
            if (maxEffects < 1 || commonMaxEffects < 0 || commonMaxEffects > maxEffects || maxPerSide < 1) {
                throw new IllegalArgumentException("affix limits need at least one effect and one per side, and a common cap within the maximum");
            }
            if (effectCountChances.size() != maxEffects - 1) {
                throw new IllegalArgumentException("affix limits need a chance for every effect count below the maximum of " + maxEffects);
            }
        }
    }

    private AffixLayout() {
    }

    static Limits limits(ItemType type) {
        Limits limits = type == null ? null : LIMITS_BY_TYPE.get().get(type);
        return limits == null ? DEFAULT_LIMITS : limits;
    }

    static Limits register(ItemType type, Limits limits) {
        Map<ItemType, Limits> previous;
        Map<ItemType, Limits> updated;
        do {
            previous = LIMITS_BY_TYPE.get();
            Map<ItemType, Limits> byType = new EnumMap<>(ItemType.class);
            byType.putAll(previous);
            if (limits == null) {
                byType.remove(type);
            } else {
                byType.put(type, limits);
            }
            updated = Map.copyOf(byType);
        } while (!LIMITS_BY_TYPE.compareAndSet(previous, updated));
        return previous.get(type);
    }

    static ItemRarity rarityFor(ItemDefinition definition, int effectCount) {
        if (definition != null && definition.unique()) {
            return ItemRarity.UNIQUE;
        }
        int commonMaxEffects = limits(definition == null ? null : definition.type()).commonMaxEffects();
        return effectCount <= commonMaxEffects ? ItemRarity.COMMON : ItemRarity.RARE;
    }

    static int rolledEffectCount(float roll) {
        return rolledEffectCount(DEFAULT_LIMITS, roll);
    }

    static int rolledEffectCount(Limits limits, float roll) {
        float cumulativeChance = 0f;
        List<Float> chances = limits.effectCountChances();
        for (int index = 0; index < chances.size(); index++) {
            cumulativeChance += chances.get(index);
            if (roll < cumulativeChance) {
                return index + 1;
            }
        }
        return limits.maxEffects();
    }

    static int prefixCount(int effectCount, boolean morePrefixes) {
        int evenShare = effectCount / 2;
        if (effectCount % 2 == 0 || !morePrefixes) {
            return evenShare;
        }
        return evenShare + 1;
    }

    static List<List<PoolEntry>> openSides(ItemDefinition definition, List<RolledModifier> modifiers) {
        return openSides(definition, modifiers, TierRange.NO_TIER);
    }

    static List<List<PoolEntry>> openSides(ItemDefinition definition, List<RolledModifier> modifiers, int tier) {
        Limits limits = limits(definition.type());
        if (definition.unique() || modifiers.size() >= limits.maxEffects()) {
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
        addOpenSide(sides, definition.prefixes(), takenPrefixes < limits.maxPerSide(), presentModifiers, tier);
        addOpenSide(sides, definition.suffixes(), takenSuffixes < limits.maxPerSide(), presentModifiers, tier);
        return sides;
    }

    static List<PoolEntry> rollableAt(List<PoolEntry> pool, int tier) {
        if (tier == TierRange.NO_TIER) {
            return pool;
        }
        return pool.stream().filter(entry -> entry.rollsAtTier(tier)).toList();
    }

    static int insertIndex(List<RolledModifier> modifiers, ModifierSlot addedSlot) {
        if (addedSlot != ModifierSlot.PREFIX) {
            return modifiers.size();
        }
        return (int) modifiers.stream().filter(modifier -> modifier.slot() == ModifierSlot.PREFIX).count();
    }

    private static void addOpenSide(List<List<PoolEntry>> sides, List<PoolEntry> sidePool, boolean hasRoom, Set<String> presentModifiers,
                                    int tier) {
        if (!hasRoom) {
            return;
        }
        List<PoolEntry> openEntries = sidePool.stream()
                .filter(entry -> !presentModifiers.contains(entry.modifier().id()) && entry.rollsAtTier(tier)).toList();
        if (!openEntries.isEmpty()) {
            sides.add(openEntries);
        }
    }
}
