package exiledsector.items;

import exiledsector.Seeds;
import exiledsector.data.WeightedPick;
import exiledsector.modifiers.TierRange;
import exiledsector.skills.tags.TagWeights;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ItemRoller {

    private ItemRoller() {
    }

    public static List<RolledModifier> roll(ItemDefinition definition, long seed) {
        return roll(definition, seed, ItemRoll.PLAIN);
    }

    public static List<RolledModifier> roll(ItemDefinition definition, long seed, ItemRoll roll) {
        return rollResolved(definition, seed, roll.resolvedFor(definition));
    }

    public static List<RolledModifier> rollPastLimits(ItemDefinition definition, long seed, int tier, int modifierCount, int maxPerSide) {
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

    static List<RolledModifier> rollResolved(ItemDefinition definition, long seed, ItemRoll resolvedRoll) {
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
        AffixLayout.Limits limits = resolvedRoll.limits().of(definition.type());
        float modifierCountRoll = random.nextFloat();
        int modifierCount = resolvedRoll.fixesModifierCount() ? resolvedRoll.modifierCount()
                : AffixLayout.rolledModifierCount(limits, modifierCountRoll, resolvedRoll.rarity());
        return rollAffixes(definition, limits.maxPerSide(), modifierCount, random, resolvedRoll.affixTier(), TagWeights.NONE);
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
        List<String> rolledIds = rolledModifiers.stream().map(RolledModifier::modifierId).toList();
        ModifierPool.draw(pool, drawCount, rolledIds, tagWeights, random, entry -> rolledModifiers.add(RolledModifier.roll(entry.modifier(), random)));
    }
}
