package exiledsector.charts.atlas;

import exiledsector.Seeds;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class AtlasHash {

    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final double UNIT = 0x1.0p-52;

    private AtlasHash() {
    }

    static long of(long seed, long salt, int q, int r) {
        return Seeds.mix(Seeds.mix(Seeds.mix(seed ^ salt) ^ q) ^ ((long) r << 32));
    }

    static long of(long seed, long salt, String id, int q, int r) {
        return of(seed, salt ^ idSalt(id), q, r);
    }

    static double unit(long hash) {
        return ((hash >>> 12) + 0.5) * UNIT;
    }

    static boolean beats(double roll, String id, double bestRoll, String bestId) {
        return roll < bestRoll || roll == bestRoll && bestId != null && id.compareTo(bestId) < 0;
    }

    static long idSalt(String id) {
        long hash = FNV_OFFSET;
        for (byte b : id.getBytes(StandardCharsets.UTF_8)) {
            hash = (hash ^ (b & 0xff)) * FNV_PRIME;
        }
        return Seeds.mix(hash);
    }

    public static String race(long seed, long salt, int q, int r, Map<String, Float> weights) {
        String winner = null;
        double bestKey = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, Float> entry : weights.entrySet()) {
            float weight = entry.getValue();
            if (!(weight > 0f)) {
                continue;
            }
            double key = -Math.log(unit(of(seed, salt, entry.getKey(), q, r))) / weight;
            if (beats(key, entry.getKey(), bestKey, winner)) {
                bestKey = key;
                winner = entry.getKey();
            }
        }
        return winner;
    }
}
