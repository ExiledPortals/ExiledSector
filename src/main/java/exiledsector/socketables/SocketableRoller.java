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
            List<RolledEffect> rolled = new ArrayList<>(definition.pool().size());
            for (SocketableDefinition.PoolEntry entry : definition.pool()) {
                rolled.add(new RolledEffect(entry.effectName(), wholeNumberBetween(entry.min(), entry.max(), random)));
            }
            return rolled;
        }
        return rollAffixes(definition, effectCount(random.nextFloat()), random);
    }

    static List<RolledEffect> rollCommon(SocketableDefinition definition, long seed) {
        return rollAffixes(definition, SocketableRarity.COMMON_MAX_EFFECTS, new Random(scramble(seed)));
    }

    private static List<RolledEffect> rollAffixes(SocketableDefinition definition, int rolledEffectCount, Random random) {
        int prefixCount = prefixCount(rolledEffectCount, random.nextBoolean());
        List<RolledEffect> rolled = new ArrayList<>(rolledEffectCount);
        draw(definition.prefixes(), prefixCount, random, rolled);
        draw(definition.suffixes(), rolledEffectCount - prefixCount, random, rolled);
        return rolled;
    }

    static int prefixCount(int effectCount, boolean morePrefixes) {
        int even = effectCount / 2;
        if (effectCount % 2 == 0 || !morePrefixes) {
            return even;
        }
        return even + 1;
    }

    private static void draw(List<SocketableDefinition.PoolEntry> pool, int drawCount, Random random, List<RolledEffect> rolled) {
        List<SocketableDefinition.PoolEntry> remaining = new ArrayList<>(pool);
        for (int left = Math.min(drawCount, pool.size()); left > 0; left--) {
            int index = pick(remaining, random.nextFloat());
            SocketableDefinition.PoolEntry entry = remaining.remove(index);
            rolled.add(new RolledEffect(entry.effectName(), wholeNumberBetween(entry.min(), entry.max(), random)));
        }
    }

    static float wholeNumberBetween(float min, float max, Random random) {
        int low = (int) Math.ceil(min);
        int high = (int) Math.floor(max);
        if (high < low) {
            return Math.round(min);
        }
        return (float) low + random.nextInt(high - low + 1);
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
