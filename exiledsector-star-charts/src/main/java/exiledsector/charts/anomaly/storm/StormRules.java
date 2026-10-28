package exiledsector.charts.anomaly.storm;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.generation.ChartGenContext;

public final class StormRules {

    static final float BAND_WIDTH = 3000f;
    static final float CROSSING_DAYS = 30f;
    static final float EDGE_MARGIN = 1500f;
    static final float MIN_EDGE_RADIUS = 6000f;
    static final float SENSOR_RANGE_MULT = 0.5f;
    static final float STRIKE_SECONDS = 6f;
    static final float STRIKE_DAMAGE = 200f;
    static final float STRIKE_EMP = 800f;
    static final float TIER_STEP = 0.1f;
    static final float LOOT_QUANTITY_PERCENT = 50f;
    static final float STORM_MODIFIER_CHANCE = 0.25f;

    private StormRules() {
    }

    static float bandWidth(float sizeMult) {
        return BAND_WIDTH * Math.max(0f, sizeMult);
    }

    static float strikeSeconds(float frequencyMult) {
        return frequencyMult > 0f ? STRIKE_SECONDS / frequencyMult : Float.MAX_VALUE;
    }

    static float tierMult(int tier) {
        return 1f + TIER_STEP * Math.max(0, tier - 1);
    }

    static float strikeDamage(int tier) {
        return STRIKE_DAMAGE * tierMult(tier);
    }

    static float strikeEmp(int tier) {
        return STRIKE_EMP * tierMult(tier);
    }

    static float hullWeight(HullSize hullSize) {
        if (hullSize == null) {
            return 0f;
        }
        return switch (hullSize) {
            case FRIGATE -> 1f;
            case DESTROYER -> 2f;
            case CRUISER -> 3f;
            case CAPITAL_SHIP -> 4f;
            default -> 0f;
        };
    }

    public static float lootQuantityMult(ChartGenContext context) {
        float bonusPercent = context == null ? 0f : context.anomalyPercent(StormFront.LOOT_KEY);
        return Math.max(0f, 1f + (LOOT_QUANTITY_PERCENT + bonusPercent) / ChartGenContext.PERCENT);
    }
}
