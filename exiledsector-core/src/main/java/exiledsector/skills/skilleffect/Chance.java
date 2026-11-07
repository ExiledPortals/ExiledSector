package exiledsector.skills.skilleffect;

import java.util.Random;

public final class Chance {

    private Chance() {
    }

    public static boolean roll(Random random, float chancePercent) {
        return chancePercent > 0f && random.nextFloat() * 100f < chancePercent;
    }
}
