package exiledsector.charts.layout.feature;

import java.util.List;
import java.util.Random;

public record FeatureRolls(Random random, float outerRadius) {

    public float between(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    public float orbitBeyond(float gap, float spread) {
        return outerRadius + gap + between(0f, spread);
    }

    public float angle() {
        return random.nextFloat() * 360f;
    }

    public int below(int bound) {
        return random.nextInt(bound);
    }

    public <T> T pick(List<T> options) {
        return options.get(random.nextInt(options.size()));
    }

    public static float orbitDays(float orbitRadius) {
        return orbitRadius / 15f;
    }
}
