package exiledsector.items;

import exiledsector.Seeds;
import exiledsector.modifiers.TierRange;
import exiledsector.skills.tags.TagWeights;

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
        return rollAffixes(definition, limits.maxPerSide(), AffixLayout.rolledModifierCount(limits, random.nextFloat(), rarity), random,
                TierRange.NO_TIER, TagWeights.NONE);
    }

    public static List<RolledModifier> rollWithModifierCount(ItemDefinition definition, long seed, int modifierCount) {
        if (definition.unique() || ItemTypes.tiered(definition.type())) {
            return roll(definition, seed);
        }
        Random random = Seeds.random(seed);
        random.nextFloat();
        return rollAffixes(definition, AffixLayout.limits(definition.type()).maxPerSide(), modifierCount, random, TierRange.NO_TIER, TagWeights.NONE);
    }

    public static List<RolledModifier> rollAtTierWithModifierCount(ItemDefinition definition, long seed, int tier, int modifierCount, int maxPerSide) {
        return rollAffixes(definition, maxPerSide, modifierCount, Seeds.random(seed), ItemTypes.rolledTier(definition, tier), TagWeights.NONE);
    }

    public static boolean canRollModifierCount(ItemDefinition definition, int modifierCount) {
        if (definition.unique() || ItemTypes.tiered(definition.type())) {
            return false;
        }
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        int prefixRoom = Math.min(limits.maxPerSide(), AffixLayout.rollableAt(definition.type(), definition.prefixes(), TierRange.NO_TIER).size());
        int suffixRoom = Math.min(limits.maxPerSide(), AffixLayout.rollableAt(definition.type(), definition.suffixes(), TierRange.NO_TIER).size());
        return modifierCount >= 1 && modifierCount <= limits.maxModifiers() && modifierCount <= prefixRoom + suffixRoom;
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
        return rollAffixes(definition, limits.maxPerSide(), AffixLayout.rolledModifierCount(limits, random.nextFloat()), random, tier, TagWeights.NONE);
    }

    private static List<RolledModifier> rollAffixes(ItemDefinition definition, int maxPerSide, int modifierCount, Random random, int tier,
                                                    TagWeights tagWeights) {
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
        draw(prefixes, prefixCount, random, tagWeights, rolledModifiers);
        draw(suffixes, suffixCount, random, tagWeights, rolledModifiers);
        return rolledModifiers;
    }

    private static void draw(List<PoolEntry> pool, int drawCount, Random random, TagWeights tagWeights, List<RolledModifier> rolledModifiers) {
        List<PoolEntry> remainingPool = new ArrayList<>(pool);
        remainingPool.removeIf(entry -> ModifierExclusivity.clashesWithRolled(entry.modifier().id(), rolledModifiers));
        for (int drawsLeft = Math.min(drawCount, pool.size()); drawsLeft > 0 && !remainingPool.isEmpty(); drawsLeft--) {
            PoolEntry entry = remainingPool.remove(WeightedPick.index(remainingPool, candidate -> candidate.biasedWeight(tagWeights), random.nextFloat()));
            rolledModifiers.add(RolledModifier.roll(entry.modifier(), random));
            remainingPool.removeIf(candidate -> ModifierExclusivity.clashesWithRolled(candidate.modifier().id(), rolledModifiers));
        }
    }
}
