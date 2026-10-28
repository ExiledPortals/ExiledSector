package exiledsector.charts.anomaly.forge;

import exiledsector.charts.generation.ChartTiers;

public final class ForgeRules {

    static final int BASE_PROGRAMS = 3;
    static final int BASE_RUNS = 1;
    static final int MAX_PROGRAMS = 5;
    static final float ORBIT_MIN = 2500f;
    static final float ORBIT_BAND_WIDTH = 2000f;
    static final int ORBIT_BANDS = 6;
    static final float COST_PER_TIER = 0.15f;

    public record Cost(int metals, int rareMetals, int feedstock) {
    }

    private ForgeRules() {
    }

    static int programCount(float bonusPrograms) {
        return Math.max(1, Math.min(MAX_PROGRAMS, BASE_PROGRAMS + Math.round(bonusPrograms)));
    }

    static int runCount(float bonusRuns) {
        return Math.max(1, BASE_RUNS + Math.round(bonusRuns));
    }

    public static Cost costFor(ForgePrograms.Program program, int tier, float costMult) {
        float scale = (1f + COST_PER_TIER * (ChartTiers.clamp(tier) - ChartTiers.LOWEST_TIER)) * Math.max(0f, costMult);
        return new Cost(Math.round(program.metals() * scale), Math.round(program.rareMetals() * scale),
                Math.round(program.feedstock() * scale));
    }
}
