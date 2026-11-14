package exiledsector.charts.table;

import exiledsector.charts.generation.ChartTiers;
import exiledsector.data.CsvRows;
import org.json.JSONObject;

public record TierCurve(float lowestTierAmount, float highestDropTierAmount, float highestTierAmount) {

    public static final String LOWEST_TIER_COLUMN = "tier_" + ChartTiers.LOWEST_TIER;
    public static final String HIGHEST_DROP_TIER_COLUMN = "tier_" + ChartTiers.HIGHEST_DROP_TIER;
    public static final String HIGHEST_TIER_COLUMN = "tier_" + ChartTiers.HIGHEST_TIER;

    public static TierCurve parse(CsvRows csv, JSONObject row, String id) {
        float lowestTierAmount = csv.number(row, LOWEST_TIER_COLUMN, 0f, id);
        float highestDropTierAmount = csv.number(row, HIGHEST_DROP_TIER_COLUMN, 0f, id);
        float extrapolated = linearAt(lowestTierAmount, highestDropTierAmount, ChartTiers.HIGHEST_TIER);
        return new TierCurve(lowestTierAmount, highestDropTierAmount, csv.number(row, HIGHEST_TIER_COLUMN, extrapolated, id));
    }

    public float at(int tier) {
        int clampedTier = ChartTiers.clamp(tier);
        float amount = clampedTier > ChartTiers.HIGHEST_DROP_TIER ? highestTierAmount
                : linearAt(lowestTierAmount, highestDropTierAmount, clampedTier);
        return Math.max(0f, amount);
    }

    private static float linearAt(float lowestTierAmount, float highestDropTierAmount, int tier) {
        float perTier = (highestDropTierAmount - lowestTierAmount) / (ChartTiers.HIGHEST_DROP_TIER - ChartTiers.LOWEST_TIER);
        return lowestTierAmount + perTier * (tier - ChartTiers.LOWEST_TIER);
    }
}
