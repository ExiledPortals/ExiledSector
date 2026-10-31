package exiledsector.charts.anomaly.facility;

import exiledsector.charts.generation.ChartTiers;

public final class FacilityRules {

    public static final String LOOT_SITE = "exiledSector_chart_pre_collapse_facility";
    public static final String LOOT_SCOPE = "pre_collapse_facility";
    static final float GUARD_SIZE_SHARE = 0.75f;
    static final float LOOT_STEPS = 1.5f;
    static final float LOOT_STEPS_PER_TIER = 0.35f;
    static final float ITEM_MULT = 1f;
    static final int DATABANK_ROLLS = 10;
    static final int DATABANK_ROLLS_PER_TIER = 2;

    private FacilityRules() {
    }

    public static float lootSteps(int tier) {
        return LOOT_STEPS + LOOT_STEPS_PER_TIER * (ChartTiers.clamp(tier) - ChartTiers.LOWEST_TIER);
    }

    public static int databankRolls(int tier) {
        return DATABANK_ROLLS + DATABANK_ROLLS_PER_TIER * (ChartTiers.clamp(tier) - ChartTiers.LOWEST_TIER);
    }
}
