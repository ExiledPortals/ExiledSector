package exiledsector.socketables;

import exiledsector.ExiledSectorModPlugin;
import exiledsector.ModSettings;
import lunalib.lunaSettings.LunaSettingsListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SocketableRoller {

    public static final String AVERAGE_ROLLS_FIELD_ID = "exiledSector_averageSocketableRolls";
    public static final boolean DEFAULT_AVERAGE_ROLLS = false;

    private static volatile boolean averageRollsSetting = DEFAULT_AVERAGE_ROLLS;

    public static final class SettingsListener implements LunaSettingsListener {

        @Override
        public void settingsChanged(String modId) {
            if (ExiledSectorModPlugin.MOD_ID.equals(modId)) {
                reloadSettings();
            }
        }
    }

    private SocketableRoller() {
    }

    public static void reloadSettings() {
        averageRollsSetting = ModSettings.booleanOr(AVERAGE_ROLLS_FIELD_ID, DEFAULT_AVERAGE_ROLLS);
    }

    public static boolean averageRolls() {
        return averageRollsSetting;
    }

    public static List<RolledEffect> roll(SocketableDefinition definition, long seed) {
        return roll(definition, seed, averageRolls());
    }

    static List<RolledEffect> roll(SocketableDefinition definition, long seed, boolean averageRolls) {
        Random random = new Random(scramble(seed));
        if (definition.unique()) {
            List<RolledEffect> rolledEffects = new ArrayList<>(definition.pool().size());
            for (PoolEntry entry : definition.pool()) {
                rolledEffects.add(rollEffect(entry, random, averageRolls));
            }
            return rolledEffects;
        }
        return rollAffixes(definition, AffixLayout.rolledEffectCount(random.nextFloat()), random, averageRolls);
    }

    static List<RolledEffect> rollCommon(SocketableDefinition definition, long seed) {
        return rollCommon(definition, seed, averageRolls());
    }

    static List<RolledEffect> rollCommon(SocketableDefinition definition, long seed, boolean averageRolls) {
        return rollAffixes(definition, AffixLayout.COMMON_MAX_EFFECTS, new Random(scramble(seed)), averageRolls);
    }

    private static List<RolledEffect> rollAffixes(SocketableDefinition definition, int rolledEffectCount, Random random, boolean averageRolls) {
        int prefixCount = AffixLayout.prefixCount(rolledEffectCount, random.nextBoolean());
        List<RolledEffect> rolledEffects = new ArrayList<>(rolledEffectCount);
        draw(definition.prefixes(), prefixCount, random, averageRolls, rolledEffects);
        draw(definition.suffixes(), rolledEffectCount - prefixCount, random, averageRolls, rolledEffects);
        return rolledEffects;
    }

    private static void draw(List<PoolEntry> pool, int drawCount, Random random, boolean averageRolls, List<RolledEffect> rolledEffects) {
        List<PoolEntry> remainingPool = new ArrayList<>(pool);
        for (int drawsLeft = Math.min(drawCount, pool.size()); drawsLeft > 0; drawsLeft--) {
            PoolEntry entry = remainingPool.remove(WeightedPick.index(remainingPool, PoolEntry::weight, random.nextFloat()));
            rolledEffects.add(rollEffect(entry, random, averageRolls));
        }
    }

    private static RolledEffect rollEffect(PoolEntry entry, Random random, boolean averageRolls) {
        float rolledMagnitude = entry.roll(random);
        return new RolledEffect(entry.effectName(), averageRolls ? entry.average() : rolledMagnitude);
    }

    static long scramble(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
