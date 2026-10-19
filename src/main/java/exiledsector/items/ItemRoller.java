package exiledsector.items;

import exiledsector.ModSettings;
import exiledsector.ModSettingsListener;
import exiledsector.Seeds;
import exiledsector.modifiers.TierRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ItemRoller {

    public static final String AVERAGE_ROLLS_FIELD_ID = "exiledSector_averageSocketableRolls";
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

    public static void reloadSettings() {
        averageRollsSetting = ModSettings.booleanOr(AVERAGE_ROLLS_FIELD_ID, DEFAULT_AVERAGE_ROLLS);
    }

    public static boolean averageRolls() {
        return averageRollsSetting;
    }

    public static List<RolledModifier> roll(ItemDefinition definition, long seed) {
        return roll(definition, seed, averageRolls());
    }

    public static List<RolledModifier> rollAtTier(ItemDefinition definition, long seed, int tier) {
        int rolledTier = definition.type() == ItemType.STAR_CHART ? StarChartDefinitions.tierAtLeastMinimum(definition.id(), tier) : tier;
        return roll(definition, seed, averageRolls(), rolledTier);
    }

    static List<RolledModifier> roll(ItemDefinition definition, long seed, boolean averageRolls) {
        return roll(definition, seed, averageRolls, TierRange.NO_TIER);
    }

    static List<RolledModifier> roll(ItemDefinition definition, long seed, boolean averageRolls, int tier) {
        Random random = Seeds.random(seed);
        if (definition.unique()) {
            List<RolledModifier> rolledModifiers = new ArrayList<>(definition.pool().size());
            for (PoolEntry entry : definition.pool()) {
                rolledModifiers.add(RolledModifier.roll(entry.modifier(), random, averageRolls));
            }
            return rolledModifiers;
        }
        AffixLayout.Limits limits = AffixLayout.limits(definition.type());
        return rollAffixes(definition, limits, AffixLayout.rolledEffectCount(limits, random.nextFloat()), random, averageRolls, tier);
    }

    private static List<RolledModifier> rollAffixes(ItemDefinition definition, AffixLayout.Limits limits, int modifierCount, Random random,
                                                    boolean averageRolls, int tier) {
        List<PoolEntry> prefixes = AffixLayout.rollableAt(definition.type(), definition.prefixes(), tier);
        List<PoolEntry> suffixes = AffixLayout.rollableAt(definition.type(), definition.suffixes(), tier);
        int prefixCount = AffixLayout.prefixCount(modifierCount, random.nextBoolean());
        int suffixCount = modifierCount - prefixCount;
        int prefixRoom = Math.min(limits.maxPerSide(), prefixes.size());
        int suffixRoom = Math.min(limits.maxPerSide(), suffixes.size());
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
