package exiledsector.items;

import exiledsector.ExiledSector;
import exiledsector.ModSettings;
import exiledsector.ModSettingsListener;
import exiledsector.Seeds;
import exiledsector.modifiers.TierRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ItemRoller {

    public static final boolean DEFAULT_AVERAGE_ROLLS = false;

    private static volatile boolean averageRollsSetting = DEFAULT_AVERAGE_ROLLS;

    public static final class SettingsListener extends ModSettingsListener {

        @Override
        protected void reload() {
            reloadSettings();
        }
    }

    private ItemRoller() {
    }

    public static String averageRollsFieldId() {
        return ExiledSector.key("averageSocketableRolls");
    }

    public static void reloadSettings() {
        averageRollsSetting = ModSettings.booleanOr(averageRollsFieldId(), DEFAULT_AVERAGE_ROLLS);
    }

    public static boolean averageRolls() {
        return averageRollsSetting;
    }

    public static List<RolledModifier> roll(ItemDefinition definition, long seed) {
        return roll(definition, seed, averageRolls());
    }

    public static List<RolledModifier> rollAtTier(ItemDefinition definition, long seed, int tier) {
        int rolledTier = ItemTypes.rolledTier(definition, tier);
        return roll(definition, seed, averageRolls(), rolledTier);
    }

    public static List<RolledModifier> rollWithRarity(ItemDefinition definition, long seed, float rarity) {
        if (rarity == 1f || definition.unique() || definition.type() == ItemType.STAR_CHART) {
            return roll(definition, seed);
        }
        Random random = Seeds.random(seed);
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        return rollAffixes(definition, limits.maxPerSide(), AffixLayout.rolledEffectCount(limits, random.nextFloat(), rarity), random, averageRolls(),
                TierRange.NO_TIER);
    }

    public static List<RolledModifier> rollWithEffectCount(ItemDefinition definition, long seed, int effectCount) {
        if (definition.unique() || definition.type() == ItemType.STAR_CHART) {
            return roll(definition, seed);
        }
        Random random = Seeds.random(seed);
        random.nextFloat();
        return rollAffixes(definition, AffixLayout.limits(definition.type()).maxPerSide(), effectCount, random, averageRolls(), TierRange.NO_TIER);
    }

    public static List<RolledModifier> rollAtTierWithEffectCount(ItemDefinition definition, long seed, int tier, int effectCount, int maxPerSide) {
        return rollAffixes(definition, maxPerSide, effectCount, Seeds.random(seed), averageRolls(), ItemTypes.rolledTier(definition, tier));
    }

    public static boolean canRollEffectCount(ItemDefinition definition, int effectCount) {
        if (definition.unique() || definition.type() == ItemType.STAR_CHART) {
            return false;
        }
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        int prefixRoom = Math.min(limits.maxPerSide(), AffixLayout.rollableAt(definition.type(), definition.prefixes(), TierRange.NO_TIER).size());
        int suffixRoom = Math.min(limits.maxPerSide(), AffixLayout.rollableAt(definition.type(), definition.suffixes(), TierRange.NO_TIER).size());
        return effectCount >= 1 && effectCount <= limits.maxEffects() && effectCount <= prefixRoom + suffixRoom;
    }

    public static List<RolledModifier> roll(ItemDefinition definition, long seed, boolean averageRolls) {
        return roll(definition, seed, averageRolls, TierRange.NO_TIER);
    }

    public static List<RolledModifier> roll(ItemDefinition definition, long seed, boolean averageRolls, int tier) {
        Random random = Seeds.random(seed);
        if (definition.keepsOneModifier() && !definition.pool().isEmpty()) {
            PoolEntry kept = definition.pool().get(random.nextInt(definition.pool().size()));
            return List.of(RolledModifier.roll(kept.modifier(), random, averageRolls));
        }
        if (definition.unique()) {
            List<RolledModifier> rolledModifiers = new ArrayList<>(definition.pool().size());
            for (PoolEntry entry : definition.pool()) {
                rolledModifiers.add(RolledModifier.roll(entry.modifier(), random, averageRolls));
            }
            return rolledModifiers;
        }
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        return rollAffixes(definition, limits.maxPerSide(), AffixLayout.rolledEffectCount(limits, random.nextFloat()), random, averageRolls, tier);
    }

    private static List<RolledModifier> rollAffixes(ItemDefinition definition, int maxPerSide, int modifierCount, Random random,
                                                    boolean averageRolls, int tier) {
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
        draw(prefixes, prefixCount, random, averageRolls, rolledModifiers);
        draw(suffixes, suffixCount, random, averageRolls, rolledModifiers);
        return rolledModifiers;
    }

    private static void draw(List<PoolEntry> pool, int drawCount, Random random, boolean averageRolls, List<RolledModifier> rolledModifiers) {
        List<PoolEntry> remainingPool = new ArrayList<>(pool);
        for (int drawsLeft = Math.min(drawCount, pool.size()); drawsLeft > 0; drawsLeft--) {
            PoolEntry entry = remainingPool.remove(WeightedPick.index(remainingPool, PoolEntry::weight, random.nextFloat()));
            rolledModifiers.add(RolledModifier.roll(entry.modifier(), random, averageRolls));
        }
    }
}
