package exiledsector;

import java.util.Random;

public final class Seeds {

    public static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    private Seeds() {
    }

    public static Random random(String seedText) {
        return new Random(scramble(seedText.hashCode()));
    }

    public static Random random(long seed) {
        return new Random(scramble(seed));
    }

    public static Random derived(long seed, long salt) {
        return new Random(mix(seed ^ salt));
    }

    public static long scramble(long seed) {
        return mix(seed + GOLDEN_GAMMA);
    }

    public static long mix(long value) {
        long mixed = value;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }
}
