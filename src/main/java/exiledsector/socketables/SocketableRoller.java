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
        int count = effectCount(random.nextFloat());
        int prefixCount = prefixCount(count, random.nextBoolean());
        List<RolledEffect> rolled = new ArrayList<>(count);
        draw(definition.prefixes(), prefixCount, random, rolled);
        draw(definition.suffixes(), count - prefixCount, random, rolled);
        return rolled;
    }

    static int prefixCount(int effectCount, boolean morePrefixes) {
        int even = effectCount / 2;
        return effectCount % 2 == 0 ? even : even + (morePrefixes ? 1 : 0);
    }

    private static void draw(List<SocketableDefinition.PoolEntry> pool, int count, Random random, List<RolledEffect> rolled) {
        List<SocketableDefinition.PoolEntry> remaining = new ArrayList<>(pool);
        for (int i = 0; i < Math.min(count, pool.size()); i++) {
            SocketableDefinition.PoolEntry entry = remaining.remove(pick(remaining, random.nextFloat()));
            rolled.add(new RolledEffect(entry.effectName(), wholeNumberBetween(entry.min(), entry.max(), random)));
        }
    }

    static float wholeNumberBetween(float min, float max, Random random) {
        int low = (int) Math.ceil(min);
        int high = (int) Math.floor(max);
        if (high < low) {
            return Math.round(min);
        }
        return low + random.nextInt(high - low + 1);
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

    private static int pick(List<SocketableDefinition.PoolEntry> entries, float roll) {
        float total = 0f;
        for (SocketableDefinition.PoolEntry entry : entries) {
            total += entry.weight();
        }
        float target = roll * total;
        for (int i = 0; i < entries.size(); i++) {
            target -= entries.get(i).weight();
            if (target < 0f) {
                return i;
            }
        }
        return entries.size() - 1;
    }
}
