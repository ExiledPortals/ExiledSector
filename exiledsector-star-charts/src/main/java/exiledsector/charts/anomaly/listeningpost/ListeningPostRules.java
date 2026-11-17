package exiledsector.charts.anomaly.listeningpost;

import exiledsector.charts.content.ChartContentPlanner;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.loot.DropType;

import java.util.List;
import java.util.Random;

public final class ListeningPostRules {

    public static final String LOOT_SITE = "exiledSector_chart_listening_post";
    public static final String LOOT_SCOPE = "listening_post";
    static final int MIN_PICKETS = 3;
    static final int MAX_PICKETS = 5;
    static final int MAX_EXTRA_PICKETS = 5;
    static final float PICKET_SIZE_SHARE = 0.3f;
    static final float GARRISON_PICKET_BOSS_SHARE = 0.5f;
    static final float PICKET_SENSOR_MULT = 1.5f;
    static final float RING_RADIUS = 1800f;
    static final float RING_JITTER = 300f;
    static final float POST_MIN_RADIUS = 2500f;
    static final float POST_MAX_RADIUS = 8000f;
    static final float POST_MIN_ENTRY_GAP = 4000f;
    static final int POST_ATTEMPTS = 6;
    static final float CHECK_DAYS = 0.1f;
    static final float SUSPICION_PER_DAY = 0.25f;
    static final float SUSPICION_DRAIN_PER_DAY = 0.1f;
    static final float REPORT_DAYS = 0.5f;
    static final float PURGE_PER_DAY = 0.2f;
    static final float VAULT_FLOOR = 0.25f;
    static final float GUARD_MERGE_RANGE = 300f;
    static final float LOOT_STEPS = 1.5f;
    static final float LOOT_STEPS_PER_TIER = 0.35f;
    static final float VAULT_QUANTITY_PERCENT = 50f;
    static final float LEAN_PERCENT = 100f;
    static final List<DropType> LEANS = List.of(DropType.STAR_CHART, DropType.BLUEPRINT, DropType.SUBROUTINE);
    static final String PICKET_PROTOTYPE_RATE = "prototype";
    static final int STOLEN_CHART_TIER_SPREAD = 2;
    static final int MIN_STOLEN_CHARTS = 1;
    static final float ARMED_POST_GAP = 250f;
    static final float ARMED_POST_ORBIT_DAYS = 30f;

    private ListeningPostRules() {
    }

    public static int picketCount(Random random, int extraPickets) {
        return MIN_PICKETS + random.nextInt(MAX_PICKETS - MIN_PICKETS + 1) + Math.max(0, Math.min(MAX_EXTRA_PICKETS, extraPickets));
    }

    public static float suspicionAfter(float suspicion, boolean inContact, float days) {
        float change = inContact ? SUSPICION_PER_DAY * days : -SUSPICION_DRAIN_PER_DAY * days;
        return Math.max(0f, Math.min(1f, suspicion + change));
    }

    public static float purgeRate(float purgeReductionPercent) {
        return PURGE_PER_DAY * Math.max(0f, 1f - purgeReductionPercent / 100f);
    }

    public static float vaultAfter(float vaultShare, float days, float purgeReductionPercent) {
        if (vaultShare <= VAULT_FLOOR) {
            return vaultShare;
        }
        return Math.max(VAULT_FLOOR, vaultShare - purgeRate(purgeReductionPercent) * Math.max(0f, days));
    }

    public static float picketSize(ChartTier adjustedTier, boolean garrison) {
        if (garrison) {
            return Math.max(adjustedTier.bossSize(), adjustedTier.fleetSize()) * GARRISON_PICKET_BOSS_SHARE;
        }
        return adjustedTier.fleetSize() * PICKET_SIZE_SHARE;
    }

    public static float picketSizeCap(boolean garrison) {
        return garrison ? ChartContentPlanner.BOSS_SIZE_CAP : ChartContentPlanner.FLEET_SIZE_CAP;
    }

    public static int stolenChartCount(Random random, int maxCharts) {
        if (maxCharts < MIN_STOLEN_CHARTS) {
            return 0;
        }
        return MIN_STOLEN_CHARTS + random.nextInt(maxCharts - MIN_STOLEN_CHARTS + 1);
    }

    public static int stolenChartTier(Random random, int runTier) {
        int offset = random.nextInt(2 * STOLEN_CHART_TIER_SPREAD + 1) - STOLEN_CHART_TIER_SPREAD;
        return Math.max(ChartTiers.LOWEST_TIER, Math.min(ChartTiers.HIGHEST_DROP_TIER, runTier + offset));
    }
}
