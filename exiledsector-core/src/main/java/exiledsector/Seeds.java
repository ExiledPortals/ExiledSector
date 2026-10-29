package exiledsector;

import java.util.Random;

public final class Seeds {

    private Seeds() {
    }

    public static Random random(String seedText) {
        return new Random(scramble(seedText.hashCode()));
    }

    public static Random random(long seed) {
        return new Random(scramble(seed));
    }

    public static long scramble(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
