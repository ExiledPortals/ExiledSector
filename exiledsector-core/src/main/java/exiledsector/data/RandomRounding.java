package exiledsector.data;

import java.util.Random;

public final class RandomRounding {

    private RandomRounding() {
    }

    public static int round(float expected, Random random) {
        if (!(expected > 0f)) {
            return 0;
        }
        return roundWithOneDraw(expected, random);
    }

    public static int roundWithOneDraw(float expected, Random random) {
        int wholePart = (int) expected;
        return wholePart + (random.nextFloat() < expected - wholePart ? 1 : 0);
    }
}
