package exiledsector.items;

import exiledsector.ModSettings;
import exiledsector.ModSettingsListener;
import exiledsector.Seeds;

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

    static List<RolledModifier> roll(ItemDefinition definition, long seed, boolean averageRolls) {
        Random random = Seeds.random(seed);
        if (definition.unique()) {
            List<RolledModifier> rolledModifiers = new ArrayList<>(definition.pool().size());
            for (PoolEntry entry : definition.pool()) {
                rolledModifiers.add(RolledModifier.roll(entry.modifier(), random, averageRolls));
            }
            return rolledModifiers;
        }
        return rollAffixes(definition, AffixLayout.rolledEffectCount(random.nextFloat()), random, averageRolls);
    }

    static List<RolledModifier> rollCommon(ItemDefinition definition, long seed) {
        return rollCommon(definition, seed, averageRolls());
    }

    static List<RolledModifier> rollCommon(ItemDefinition definition, long seed, boolean averageRolls) {
        return rollAffixes(definition, AffixLayout.COMMON_MAX_EFFECTS, Seeds.random(seed), averageRolls);
    }

    private static List<RolledModifier> rollAffixes(ItemDefinition definition, int modifierCount, Random random, boolean averageRolls) {
        int prefixCount = AffixLayout.prefixCount(modifierCount, random.nextBoolean());
        List<RolledModifier> rolledModifiers = new ArrayList<>(modifierCount);
        draw(definition.prefixes(), prefixCount, random, averageRolls, rolledModifiers);
        draw(definition.suffixes(), modifierCount - prefixCount, random, averageRolls, rolledModifiers);
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
