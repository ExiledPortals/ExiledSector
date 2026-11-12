package exiledsector.charts.officer.reward;

import java.util.Random;

public enum CraftingTier {
    LOW("low", 7),
    MID("mid", 3),
    HIGH("high", 1);

    private final String id;
    private final int maxCount;

    CraftingTier(String id, int maxCount) {
        this.id = id;
        this.maxCount = maxCount;
    }

    public String id() {
        return id;
    }

    public int maxCount() {
        return maxCount;
    }

    public int rollCount(Random random) {
        return 1 + random.nextInt(maxCount);
    }

    public static CraftingTier byId(String id) {
        for (CraftingTier tier : values()) {
            if (tier.id.equals(id)) {
                return tier;
            }
        }
        return null;
    }
}
