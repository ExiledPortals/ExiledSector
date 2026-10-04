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
        List<SocketableDefinition.PoolEntry> remaining = new ArrayList<>(definition.pool());
        int count = Math.min(remaining.size(), effectCount(random.nextFloat()));
        List<RolledEffect> rolled = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            SocketableDefinition.PoolEntry entry = remaining.remove(pick(remaining, random.nextFloat()));
            float magnitude = Math.round(entry.min() + random.nextFloat() * (entry.max() - entry.min()));
            rolled.add(new RolledEffect(entry.effectName(), magnitude));
        }
        return rolled;
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
