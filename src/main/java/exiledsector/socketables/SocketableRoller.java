package exiledsector.socketables;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SocketableRoller {

    private SocketableRoller() {
    }

    public static List<RolledEffect> roll(SocketableDefinition definition, long seed) {
        Random random = new Random(scramble(seed));
        if (definition.unique()) {
            List<RolledEffect> rolledEffects = new ArrayList<>(definition.pool().size());
            for (PoolEntry entry : definition.pool()) {
                rolledEffects.add(new RolledEffect(entry.effectName(), entry.roll(random)));
            }
            return rolledEffects;
        }
        return rollAffixes(definition, AffixLayout.rolledEffectCount(random.nextFloat()), random);
    }

    static List<RolledEffect> rollCommon(SocketableDefinition definition, long seed) {
        return rollAffixes(definition, AffixLayout.COMMON_MAX_EFFECTS, new Random(scramble(seed)));
    }

    private static List<RolledEffect> rollAffixes(SocketableDefinition definition, int rolledEffectCount, Random random) {
        int prefixCount = AffixLayout.prefixCount(rolledEffectCount, random.nextBoolean());
        List<RolledEffect> rolledEffects = new ArrayList<>(rolledEffectCount);
        draw(definition.prefixes(), prefixCount, random, rolledEffects);
        draw(definition.suffixes(), rolledEffectCount - prefixCount, random, rolledEffects);
        return rolledEffects;
    }

    private static void draw(List<PoolEntry> pool, int drawCount, Random random, List<RolledEffect> rolledEffects) {
        List<PoolEntry> remainingPool = new ArrayList<>(pool);
        for (int drawsLeft = Math.min(drawCount, pool.size()); drawsLeft > 0; drawsLeft--) {
            PoolEntry entry = remainingPool.remove(WeightedPick.index(remainingPool, PoolEntry::weight, random.nextFloat()));
            rolledEffects.add(new RolledEffect(entry.effectName(), entry.roll(random)));
        }
    }

    static long scramble(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
