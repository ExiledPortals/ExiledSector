package exiledsector.charts.anomaly.convoy;

import exiledsector.charts.generation.ChartTiers;

public final class ConvoyRules {

    public static final String FACTION_ID = "exiledSector_convoy";
    public static final String LOOT_SCOPE = "convoy";
    public static final String CARGO_SITE = "exiledSector_chart_convoy";
    static final int MIN_WAYPOINTS = 2;
    static final int MAX_WAYPOINTS = 4;
    static final int MAX_CONVOYS = 3;
    static final float EDGE_MARGIN = 2000f;
    static final float DEPARTURE_DAYS = 5f;
    static final float STOP_DAYS = 1f;
    static final float UNLOAD_DAYS = 3f;
    static final float CHECK_DAYS = 0.05f;
    static final float SCREEN_LEAD = 1200f;
    static final float SPAWN_SPREAD = 300f;
    static final float SCREEN_SIZE_SHARE = 0.6f;
    static final float ESCORT_SIZE_SHARE = 0.9f;
    static final float FREIGHTER_POINTS = 40f;
    static final float FREIGHTER_POINTS_PER_TIER = 10f;
    static final float TANKER_SHARE = 0.25f;
    static final float FREIGHTER_GUARD_POINTS = 5f;
    static final float CARGO_STEPS_PER_FREIGHTER_POINT = 0.03f;
    static final float POD_SHARE = 1f;
    static final float ITEM_CHANCE_PER_TIER = 0.1f;
    static final float FREIGHTER_TERRAIN_BURN = 0.8f;
    static final float SCREEN_TERRAIN_BURN = 0.5f;
    static final float ESCORT_TERRAIN_BURN = 0.65f;
    static final float PULSE_REPEAT_DAYS = 0.15f;

    private ConvoyRules() {
    }

    static float freighterPoints(int tier) {
        return FREIGHTER_POINTS + FREIGHTER_POINTS_PER_TIER * (ChartTiers.clamp(tier) - 1);
    }

    static int convoyCount(float extraConvoys) {
        return Math.max(1, Math.min(MAX_CONVOYS, 1 + Math.round(extraConvoys)));
    }

    static float cargoSteps(float freighterPoints, float cargoMult) {
        return Math.max(0f, freighterPoints) * CARGO_STEPS_PER_FREIGHTER_POINT * Math.max(0f, cargoMult);
    }

    static float itemChanceMult(int tier, float cargoMult) {
        return (1f + ITEM_CHANCE_PER_TIER * (ChartTiers.clamp(tier) - 1)) * Math.max(0f, cargoMult);
    }

    static float terrainBurn(ConvoyRole role) {
        return switch (role) {
            case FREIGHTERS -> FREIGHTER_TERRAIN_BURN;
            case SCREEN -> SCREEN_TERRAIN_BURN;
            case ESCORT -> ESCORT_TERRAIN_BURN;
        };
    }
}
