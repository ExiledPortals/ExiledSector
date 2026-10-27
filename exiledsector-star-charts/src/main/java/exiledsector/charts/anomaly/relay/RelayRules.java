package exiledsector.charts.anomaly.relay;

import exiledsector.charts.generation.ChartTiers;

public final class RelayRules {

    static final float FIRST_BAND_RADIUS = 2000f;
    static final float BAND_WIDTH = 2000f;
    static final float GUARDED_CHANCE = 0.5f;
    static final float GUARD_GAP_MIN = 150f;
    static final float GUARD_GAP_MAX = 350f;
    public static final float WAKE_RANGE = 2000f;
    public static final float STATION_KEEPING_RANGE = 300f;
    public static final float STATION_KEEPING_BURN = 2f;
    public static final float STATION_KEEPING_DAYS = 0.25f;
    public static final float HANDSHAKE_DAYS = 0.25f;
    static final float HOLD_GAP_DAYS = 0.2f;
    static final int SUPPLIES_BASE = 10;
    static final int SUPPLIES_PER_TIER = 4;
    static final int MACHINERY_BASE = 5;
    static final int MACHINERY_PER_TIER = 2;
    static final float REVEAL_RANGE_MULT = 1000f;
    static final float REVEAL_EXTENDED_RANGE = 100000f;
    public static final int MIN_ONWARD_LANES = 2;
    public static final int MAX_ONWARD_LANES = 3;
    public static final int MIN_STAGES = 3;
    public static final int MAX_STAGES = 4;

    public record Cost(int supplies, int machinery) {
    }

    private RelayRules() {
    }

    public static Cost costFor(int tier, float costMult) {
        int clamped = ChartTiers.clamp(tier);
        float mult = Math.max(0f, costMult);
        return new Cost(Math.round((SUPPLIES_BASE + SUPPLIES_PER_TIER * clamped) * mult), Math.round((MACHINERY_BASE + MACHINERY_PER_TIER * clamped) * mult));
    }
}
