package exiledsector.charts.anomaly.heist;

import exiledsector.charts.loot.DropType;

import java.util.List;
import java.util.Random;

public final class HeistRules {

    public static final String LOOT_SITE = "exiledSector_chart_listening_post";
    public static final String LOOT_SCOPE = "listening_post";
    static final int MIN_PICKETS = 3;
    static final int MAX_PICKETS = 5;
    static final int MAX_EXTRA_PICKETS = 5;
    static final float PICKET_SIZE_SHARE = 0.3f;
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
    static final float ITEM_MULT = 1.5f;
    static final float LEAN_PERCENT = 100f;
    static final List<DropType> LEANS = List.of(DropType.STAR_CHART, DropType.BLUEPRINT, DropType.SUBROUTINE);

    private HeistRules() {
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
}
