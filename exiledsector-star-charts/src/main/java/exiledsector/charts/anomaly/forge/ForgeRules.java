package exiledsector.charts.anomaly.forge;

import exiledsector.charts.generation.ChartTier;

public final class ForgeRules {

    static final int BASE_RUNS = 1;
    static final float ORBIT_MIN = 2500f;
    static final float ORBIT_BAND_WIDTH = 2000f;
    static final int ORBIT_BANDS = 6;
    static final float ESCORT_BOSS_SHARE = 0.8f;
    static final int ESCORT_MAX_SHIP_SIZE = 3;

    public record Cost(int feedstock, int infusedFeedstockEach) {
    }

    private ForgeRules() {
    }

    static int runCount(float bonusRuns) {
        return Math.max(1, BASE_RUNS + Math.round(bonusRuns));
    }

    static float escortSize(ChartTier adjustedTier) {
        return Math.max(adjustedTier.bossSize(), adjustedTier.fleetSize()) * ESCORT_BOSS_SHARE;
    }

    public static Cost costFor(ForgePrograms.Program program, float costMult) {
        float scale = Math.max(0f, costMult);
        return new Cost(Math.round(program.feedstock() * scale), Math.round(program.infusedFeedstockEach() * scale));
    }
}
