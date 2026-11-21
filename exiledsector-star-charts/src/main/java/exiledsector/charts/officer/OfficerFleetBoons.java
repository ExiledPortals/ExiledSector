package exiledsector.charts.officer;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;

public record OfficerFleetBoons(float sizePercent, float qualityPercent, int escortOfficerLevels, int flagshipLevels) {

    public static final OfficerFleetBoons NONE = new OfficerFleetBoons(0f, 0f, 0, 0);
    static final String SIZE_KEY = ChartModifier.CHART_OFFICER_FLEET_SIZE_PERCENT.anomalyTuningKey();
    static final String QUALITY_KEY = ChartModifier.CHART_OFFICER_FLEET_QUALITY_PERCENT.anomalyTuningKey();
    static final String ESCORT_LEVELS_KEY = ChartModifier.CHART_OFFICER_ESCORT_OFFICER_LEVELS.anomalyTuningKey();
    static final String FLAGSHIP_LEVELS_KEY = ChartModifier.CHART_OFFICER_LEVELS.anomalyTuningKey();
    static final String SKIRMISH_SIZE_KEY = ChartModifier.CHART_SKIRMISH_OFFICER_FLEET_SIZE_PERCENT.anomalyTuningKey();

    public static OfficerFleetBoons atCommandCentre(ChartGenContext context) {
        return context == null || context.anomalyData(AtlasOfficer.RUN_DATA_KEY) == null ? NONE : of(context, 0f);
    }

    public static OfficerFleetBoons inSkirmish(ChartGenContext context) {
        return context == null ? NONE : of(context, context.anomalyPercent(SKIRMISH_SIZE_KEY));
    }

    private static OfficerFleetBoons of(ChartGenContext context, float extraSizePercent) {
        return new OfficerFleetBoons(context.anomalyPercent(SIZE_KEY) + extraSizePercent, context.anomalyPercent(QUALITY_KEY),
                Math.round(context.anomalyPercent(ESCORT_LEVELS_KEY)), Math.round(context.anomalyPercent(FLAGSHIP_LEVELS_KEY)));
    }

    public ChartTier strengthen(ChartTier tier) {
        return tier.withStrongerBoss(sizePercent, qualityPercent, escortOfficerLevels);
    }
}
