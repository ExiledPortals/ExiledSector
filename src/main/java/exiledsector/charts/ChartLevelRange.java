package exiledsector.charts;

import java.util.Random;

public record ChartLevelRange(int min, int max) {

    public ChartLevelRange {
        min = Math.max(0, min);
        max = Math.max(min, max);
    }

    public int roll(Random random) {
        return min + random.nextInt(max - min + 1);
    }
}
