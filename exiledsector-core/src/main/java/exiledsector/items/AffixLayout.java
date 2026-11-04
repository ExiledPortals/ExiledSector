package exiledsector.items;

import exiledsector.modifiers.ModifierEffects;
import exiledsector.modifiers.ModifierLine;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.TierRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class AffixLayout {

    static final int COMMON_MAX_EFFECTS = 2;
    static final int MAX_EFFECTS = 4;
    public static final int MAX_PER_SIDE = 2;
    static final float ONE_EFFECT_CHANCE = 0.35f;
    static final float TWO_EFFECTS_CHANCE = 0.2f;
    static final float THREE_EFFECTS_CHANCE = 0.35f;
    static final Limits DEFAULT_LIMITS = new Limits(COMMON_MAX_EFFECTS, MAX_EFFECTS, MAX_PER_SIDE,
            List.of(ONE_EFFECT_CHANCE, TWO_EFFECTS_CHANCE, THREE_EFFECTS_CHANCE));

    public record Limits(int commonMaxModifiers, int maxModifiers, int maxPerSide, List<Float> modifierCountChances) {

        public Limits {
            modifierCountChances = List.copyOf(modifierCountChances);
            if (maxModifiers < 1 || commonMaxModifiers < 0 || commonMaxModifiers > maxModifiers || maxPerSide < 1) {
                throw new IllegalArgumentException("affix limits need at least one modifier and one per side, and a common cap within the maximum");
            }
            if (modifierCountChances.size() != maxModifiers - 1) {
                throw new IllegalArgumentException("affix limits need a chance for every effect count below the maximum of " + maxModifiers);
            }
        }
    }

    private AffixLayout() {
    }

    public static Limits limits(ItemType type) {
        ItemTypeSupport support = ItemTypes.of(type);
        return support == null ? DEFAULT_LIMITS : support.affixLimits();
    }

    public static Limits dropLimits(ItemType type) {
        ItemTypeSupport support = ItemTypes.of(type);
        return support == null ? DEFAULT_LIMITS : support.dropLimits();
    }

    public static ItemRarity rarityFor(ItemDefinition definition, int modifierCount) {
        if (definition != null && definition.hasUniqueRarity()) {
            return ItemRarity.UNIQUE;
        }
        int commonMaxModifiers = limits(definition == null ? null : definition.type()).commonMaxModifiers();
        return modifierCount <= commonMaxModifiers ? ItemRarity.COMMON : ItemRarity.RARE;
    }

    public static int rolledModifierCount(float roll) {
        return rolledModifierCount(DEFAULT_LIMITS, roll);
    }

    public static int rolledModifierCount(Limits limits, float roll) {
        float cumulativeChance = 0f;
        List<Float> chances = limits.modifierCountChances();
        for (int index = 0; index < chances.size(); index++) {
            cumulativeChance += chances.get(index);
            if (roll < cumulativeChance) {
                return index + 1;
            }
        }
        return limits.maxModifiers();
    }

    public static int rolledModifierCount(Limits limits, float roll, float rarity) {
        if (rarity == 1f) {
            return rolledModifierCount(limits, roll);
        }
        List<Float> chances = limits.modifierCountChances();
        float[] weights = new float[limits.maxModifiers()];
        float remainingChance = 1f;
        for (int index = 0; index < chances.size(); index++) {
            weights[index] = chances.get(index);
            remainingChance -= chances.get(index);
        }
        weights[limits.maxModifiers() - 1] = Math.max(0f, remainingChance);
        float totalWeight = 0f;
        for (int index = 0; index < weights.length; index++) {
            if (index + 1 > limits.commonMaxModifiers()) {
                weights[index] *= Math.max(0f, rarity);
            }
            totalWeight += weights[index];
        }
        float target = roll * totalWeight;
        for (int index = 0; index < weights.length; index++) {
            target -= weights[index];
            if (target < 0f) {
                return index + 1;
            }
        }
        return limits.maxModifiers();
    }

    public static int prefixCount(int modifierCount, boolean morePrefixes) {
        int evenShare = modifierCount / 2;
        if (modifierCount % 2 == 0 || !morePrefixes) {
            return evenShare;
        }
        return evenShare + 1;
    }

    public static List<List<PoolEntry>> openSides(ItemDefinition definition, List<RolledModifier> modifiers) {
        return openSides(definition, modifiers, TierRange.NO_TIER);
    }

    public static List<List<PoolEntry>> openSides(ItemDefinition definition, List<RolledModifier> modifiers, int tier) {
        Limits limits = limits(definition.type());
        if (definition.unique() || modifiers.size() >= limits.maxModifiers()) {
            return List.of();
        }
        Set<String> presentModifiers = new HashSet<>();
        Set<String> reservedEffects = new HashSet<>();
        int takenPrefixes = 0;
        int takenSuffixes = 0;
        for (RolledModifier modifier : modifiers) {
            presentModifiers.add(modifier.modifierId());
            if (ModifierSources.isReserved(modifier.modifierId())) {
                modifier.lines().forEach(line -> reservedEffects.add(line.effectName()));
            }
            if (modifier.slot() == ModifierSlot.PREFIX) {
                takenPrefixes++;
            } else {
                takenSuffixes++;
            }
        }
        List<List<PoolEntry>> sides = new ArrayList<>();
        addOpenSide(sides, rollableAt(definition.type(), definition.prefixes(), tier), takenPrefixes < limits.maxPerSide(), presentModifiers,
                reservedEffects);
        addOpenSide(sides, rollableAt(definition.type(), definition.suffixes(), tier), takenSuffixes < limits.maxPerSide(), presentModifiers,
                reservedEffects);
        return sides;
    }

    public static List<PoolEntry> rollableAt(ItemType type, List<PoolEntry> pool, int tier) {
        ModifierEffects effects = ItemEffects.forType(type);
        return pool.stream().filter(entry -> rollable(entry, effects, tier)).toList();
    }

    private static boolean rollable(PoolEntry entry, ModifierEffects effects, int tier) {
        if (tier != TierRange.NO_TIER && !entry.rollsAtTier(tier)) {
            return false;
        }
        for (ModifierLine line : entry.modifier().lines()) {
            if (!effects.canRoll(line.effectName())) {
                return false;
            }
        }
        return true;
    }

    public static void insert(List<RolledModifier> modifiers, RolledModifier added) {
        modifiers.add(insertIndex(modifiers, added.slot()), added);
    }

    static RolledModifier rollFrom(List<List<PoolEntry>> sides, Random random) {
        List<PoolEntry> side = sides.get(random.nextInt(sides.size()));
        PoolEntry poolEntry = side.get(WeightedPick.index(side, PoolEntry::weight, random.nextFloat()));
        return RolledModifier.roll(poolEntry.modifier(), random);
    }

    public static int insertIndex(List<RolledModifier> modifiers, ModifierSlot addedSlot) {
        if (addedSlot != ModifierSlot.PREFIX) {
            return modifiers.size();
        }
        return (int) modifiers.stream().filter(modifier -> modifier.slot() == ModifierSlot.PREFIX).count();
    }

    private static void addOpenSide(List<List<PoolEntry>> sides, List<PoolEntry> rollablePool, boolean hasRoom, Set<String> presentModifiers,
                                    Set<String> reservedEffects) {
        if (!hasRoom) {
            return;
        }
        List<PoolEntry> openEntries = rollablePool.stream()
                .filter(entry -> !presentModifiers.contains(entry.modifier().id()))
                .filter(entry -> !ModifierExclusivity.clashes(entry.modifier().id(), presentModifiers))
                .filter(entry -> entry.modifier().lines().stream().noneMatch(line -> reservedEffects.contains(line.effectName()))).toList();
        if (!openEntries.isEmpty()) {
            sides.add(openEntries);
        }
    }
}
