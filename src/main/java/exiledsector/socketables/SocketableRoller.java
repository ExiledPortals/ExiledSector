package exiledsector.socketables;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SocketableRoller {

    static final float TWO_EFFECTS_CHANCE = 0.55f;
    static final float THREE_EFFECTS_CHANCE = 0.35f;

    private SocketableRoller() {
    }

    public static List<RolledEffect> roll(SocketableDefinition definition, long seed) {
        Random random = new Random(scramble(seed));
        if (definition.unique()) {
            List<RolledEffect> rolledEffects = new ArrayList<>(definition.pool().size());
            for (SocketableDefinition.PoolEntry entry : definition.pool()) {
                rolledEffects.add(new RolledEffect(entry.effectName(), wholeNumberBetween(entry.min(), entry.max(), random)));
            }
            return rolledEffects;
        }
        return rollAffixes(definition, effectCount(random.nextFloat()), random);
    }

    static List<RolledEffect> rollCommon(SocketableDefinition definition, long seed) {
        return rollAffixes(definition, SocketableRarity.COMMON_MAX_EFFECTS, new Random(scramble(seed)));
    }

    private static List<RolledEffect> rollAffixes(SocketableDefinition definition, int rolledEffectCount, Random random) {
        int prefixCount = prefixCount(rolledEffectCount, random.nextBoolean());
        List<RolledEffect> rolledEffects = new ArrayList<>(rolledEffectCount);
        draw(definition.prefixes(), prefixCount, random, rolledEffects);
        draw(definition.suffixes(), rolledEffectCount - prefixCount, random, rolledEffects);
        return rolledEffects;
    }

    static int prefixCount(int effectCount, boolean morePrefixes) {
        int evenShare = effectCount / 2;
        if (effectCount % 2 == 0 || !morePrefixes) {
            return evenShare;
        }
        return evenShare + 1;
    }

    private static void draw(List<SocketableDefinition.PoolEntry> pool, int drawCount, Random random, List<RolledEffect> rolledEffects) {
        List<SocketableDefinition.PoolEntry> remainingPool = new ArrayList<>(pool);
        for (int drawsLeft = Math.min(drawCount, pool.size()); drawsLeft > 0; drawsLeft--) {
            int pickedIndex = pick(remainingPool, random.nextFloat());
            SocketableDefinition.PoolEntry entry = remainingPool.remove(pickedIndex);
            rolledEffects.add(new RolledEffect(entry.effectName(), wholeNumberBetween(entry.min(), entry.max(), random)));
        }
    }

    static float wholeNumberBetween(float min, float max, Random random) {
        int lowestWhole = (int) Math.ceil(min);
        int highestWhole = (int) Math.floor(max);
        if (highestWhole < lowestWhole) {
            return Math.round(min);
        }
        return (float) lowestWhole + random.nextInt(highestWhole - lowestWhole + 1);
    }

    static long scramble(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    static int effectCount(float roll) {
        if (roll < TWO_EFFECTS_CHANCE) {
            return 2;
        }
        return roll < TWO_EFFECTS_CHANCE + THREE_EFFECTS_CHANCE ? 3 : 4;
    }

    static int pick(List<SocketableDefinition.PoolEntry> entries, float roll) {
        float weightTotal = 0f;
        for (SocketableDefinition.PoolEntry entry : entries) {
            weightTotal += entry.weight();
        }
        float remainingWeight = roll * weightTotal;
        for (int i = 0; i < entries.size(); i++) {
            remainingWeight -= entries.get(i).weight();
            if (remainingWeight < 0f) {
                return i;
            }
        }
        return entries.size() - 1;
    }
}
