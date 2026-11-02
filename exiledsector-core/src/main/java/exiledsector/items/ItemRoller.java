package exiledsector.items;

import exiledsector.Seeds;
import exiledsector.modifiers.TierRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ItemRoller {

    private ItemRoller() {
    }

    public static List<RolledModifier> roll(ItemDefinition definition, long seed) {
        return roll(definition, seed, TierRange.NO_TIER);
    }

    public static List<RolledModifier> rollAtTier(ItemDefinition definition, long seed, int tier) {
        int rolledTier = ItemTypes.rolledTier(definition, tier);
        return roll(definition, seed, rolledTier);
    }

    public static List<RolledModifier> rollWithRarity(ItemDefinition definition, long seed, float rarity) {
        if (definition.unique() || definition.keepsOneModifier() || ItemTypes.tiered(definition.type())) {
            return roll(definition, seed);
        }
        Random random = Seeds.random(seed);
        AffixLayout.Limits limits = AffixLayout.dropLimits(definition.type());
        return rollAffixes(definition, limits.maxPerSide(), AffixLayout.rolledEffectCount(limits, random.nextFloat(), rarity), random,
                TierRange.NO_TIER);
    }

    public static List<RolledModifier> rollWithEffectCount(ItemDefinition definition, long seed, int effectCount) {
        if (definition.unique() || ItemTypes.tiered(definition.type())) {
            return roll(definition, seed);
        }
        Random random = Seeds.random(seed);
        random.nextFloat();
        return rollAffixes(definition, AffixLayout.limits(definition.type()).maxPerSide(), effectCount, random, TierRange.NO_TIER);
    }

    public static List<RolledModifier> rollAtTierWithEffectCount(ItemDefinition definition, long seed, int tier, int effectCount, int maxPerSide) {
        return rollAffixes(definition, maxPerSide, effectCount, Seeds.random(seed), ItemTypes.rolledTier(definition, tier));
    }

    public static boolean canRollEffectCount(ItemDefinition definition, int effectCount) {
        if (definition.unique() || ItemTypes.tiered(definition.type())) {
            return false;
        }
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        int prefixRoom = Math.min(limits.maxPerSide(), AffixLayout.rollableAt(definition.type(), definition.prefixes(), TierRange.NO_TIER).size());
        int suffixRoom = Math.min(limits.maxPerSide(), AffixLayout.rollableAt(definition.type(), definition.suffixes(), TierRange.NO_TIER).size());
        return effectCount >= 1 && effectCount <= limits.maxEffects() && effectCount <= prefixRoom + suffixRoom;
    }

    public static List<RolledModifier> roll(ItemDefinition definition, long seed, int tier) {
        Random random = Seeds.random(seed);
        if (definition.keepsOneModifier() && !definition.pool().isEmpty()) {
            PoolEntry kept = definition.pool().get(random.nextInt(definition.pool().size()));
            return List.of(RolledModifier.roll(kept.modifier(), random));
        }
        if (definition.unique()) {
            List<RolledModifier> rolledModifiers = new ArrayList<>(definition.pool().size());
            for (PoolEntry entry : definition.pool()) {
                rolledModifiers.add(RolledModifier.roll(entry.modifier(), random));
            }
            return rolledModifiers;
        }
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        return rollAffixes(definition, limits.maxPerSide(), AffixLayout.rolledEffectCount(limits, random.nextFloat()), random, tier);
    }

    private static List<RolledModifier> rollAffixes(ItemDefinition definition, int maxPerSide, int modifierCount, Random random, int tier) {
        List<PoolEntry> prefixes = AffixLayout.rollableAt(definition.type(), definition.prefixes(), tier);
        List<PoolEntry> suffixes = AffixLayout.rollableAt(definition.type(), definition.suffixes(), tier);
        int prefixCount = AffixLayout.prefixCount(modifierCount, random.nextBoolean());
        int suffixCount = modifierCount - prefixCount;
        int prefixRoom = Math.min(maxPerSide, prefixes.size());
        int suffixRoom = Math.min(maxPerSide, suffixes.size());
        if (prefixCount > prefixRoom) {
            suffixCount += prefixCount - prefixRoom;
            prefixCount = prefixRoom;
        }
        if (suffixCount > suffixRoom) {
            prefixCount = Math.min(prefixRoom, prefixCount + suffixCount - suffixRoom);
            suffixCount = suffixRoom;
        }
        List<RolledModifier> rolledModifiers = new ArrayList<>(modifierCount);
        draw(prefixes, prefixCount, random, rolledModifiers);
        draw(suffixes, suffixCount, random, rolledModifiers);
        return rolledModifiers;
    }

    private static void draw(List<PoolEntry> pool, int drawCount, Random random, List<RolledModifier> rolledModifiers) {
        List<PoolEntry> remainingPool = new ArrayList<>(pool);
        remainingPool.removeIf(entry -> ModifierExclusivity.clashesWithRolled(entry.modifier().id(), rolledModifiers));
        for (int drawsLeft = Math.min(drawCount, pool.size()); drawsLeft > 0 && !remainingPool.isEmpty(); drawsLeft--) {
            PoolEntry entry = remainingPool.remove(WeightedPick.index(remainingPool, PoolEntry::weight, random.nextFloat()));
            rolledModifiers.add(RolledModifier.roll(entry.modifier(), random));
            remainingPool.removeIf(candidate -> ModifierExclusivity.clashesWithRolled(candidate.modifier().id(), rolledModifiers));
        }
    }
}
