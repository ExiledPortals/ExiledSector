package exiledsector.charts;

import exiledsector.charts.content.ChartPoi;
import exiledsector.charts.objective.ChartObjective;

public record ChartTier(int tier, float fleetSize, int fleetsMin, int fleetsMax, int officerLevelBonus, int officerCountBonus,
                        float fleetQuality, int levelMin, int levelMax, float cacheBudget, float domainBudget,
                        float stationBudget, float derelictBudget, boolean boss, float bossSize, float clearWeight, float bossWeight) {

    public static final float FLEET_SIZE_BASE_POINTS = 50f;
    public static final float PERCENT = 100f;
    static final float FALLBACK_FIRST_TIER_FLEET_SIZE = 100f;
    static final float FALLBACK_FLEET_SIZE_PER_TIER = 32f;
    static final float FALLBACK_FIRST_TIER_BOSS_SIZE = 180f;
    static final float FALLBACK_BOSS_SIZE_PER_TIER = 65f;
    static final float FALLBACK_POI_BUDGET_PER_TIER = 3f;
    static final float FALLBACK_FIRST_TIER_POI_BUDGET = 6f;
    static final float FALLBACK_CACHE_SHARE = 0.35f;
    static final float FALLBACK_DOMAIN_SHARE = 0.15f;
    static final float FALLBACK_STATION_SHARE = 0.2f;
    static final float FALLBACK_DERELICT_SHARE = 0.3f;
    static final int FALLBACK_FIRST_DOMAIN_TIER = 2;
    static final int FALLBACK_FIRST_STATION_TIER = 3;
    static final int FALLBACK_FIRST_TIER_LEVEL = 26;
    static final float FALLBACK_LEVELS_PER_TIER = 1.5f;
    static final int FALLBACK_LEVEL_SPREAD = 3;

    public ChartTier {
        fleetSize = Math.max(0f, fleetSize);
        fleetsMin = Math.max(1, fleetsMin);
        fleetsMax = Math.max(fleetsMin, fleetsMax);
        officerLevelBonus = Math.max(0, officerLevelBonus);
        officerCountBonus = Math.max(0, officerCountBonus);
        fleetQuality = Math.max(0f, fleetQuality);
        int lowerLevel = Math.max(0, Math.min(levelMin, levelMax));
        int upperLevel = Math.max(0, Math.max(levelMin, levelMax));
        levelMin = lowerLevel;
        levelMax = upperLevel;
        cacheBudget = Math.max(0f, cacheBudget);
        domainBudget = Math.max(0f, domainBudget);
        stationBudget = Math.max(0f, stationBudget);
        derelictBudget = Math.max(0f, derelictBudget);
        bossSize = Math.max(0f, bossSize);
        boss = boss && bossSize > 0f;
        clearWeight = Math.max(0f, clearWeight);
        bossWeight = Math.max(0f, bossWeight);
    }

    public static float fleetPoints(float sizePercent) {
        return FLEET_SIZE_BASE_POINTS * Math.max(0f, sizePercent) / PERCENT;
    }

    public float siteBudget(ChartPoi.Kind kind) {
        return switch (kind) {
            case CACHE -> cacheBudget;
            case DOMAIN -> domainBudget;
            case STATION -> stationBudget;
            case DERELICT -> derelictBudget;
        };
    }

    public float poiBudget() {
        return cacheBudget + domainBudget + stationBudget + derelictBudget;
    }

    public float weightFor(ChartObjective objective) {
        return switch (objective) {
            case CLEAR -> clearWeight;
            case BOSS -> boss ? bossWeight : 0f;
        };
    }

    public static ChartTier fallback(int tier) {
        int steps = Math.max(0, tier - 1);
        int levelMin = FALLBACK_FIRST_TIER_LEVEL + Math.round(FALLBACK_LEVELS_PER_TIER * steps);
        float poiBudget = FALLBACK_FIRST_TIER_POI_BUDGET + FALLBACK_POI_BUDGET_PER_TIER * steps;
        return new ChartTier(tier, FALLBACK_FIRST_TIER_FLEET_SIZE + FALLBACK_FLEET_SIZE_PER_TIER * steps, 2 + steps / 3,
                4 + steps / 3,
                steps / 3, steps / 3, Math.min(100f, 30f + 5f * steps), levelMin, levelMin + FALLBACK_LEVEL_SPREAD,
                poiBudget * FALLBACK_CACHE_SHARE, tier >= FALLBACK_FIRST_DOMAIN_TIER ? poiBudget * FALLBACK_DOMAIN_SHARE : 0f,
                tier >= FALLBACK_FIRST_STATION_TIER ? poiBudget * FALLBACK_STATION_SHARE : 0f, poiBudget * FALLBACK_DERELICT_SHARE, true,
                FALLBACK_FIRST_TIER_BOSS_SIZE + FALLBACK_BOSS_SIZE_PER_TIER * steps, 1f, 1f);
    }
}
