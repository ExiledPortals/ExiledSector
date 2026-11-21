package exiledsector.charts.anomaly.magneticstorm;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;

public record MagneticStormTuning(boolean ionFrontAuroras, float ionFrontCraftingItemPercent, boolean flareAuroras, int extraLuckyModifiers) {

    public static final MagneticStormTuning NONE = new MagneticStormTuning(false, 0f, false, 0);

    public static MagneticStormTuning of(ChartGenContext context) {
        if (context == null) {
            return NONE;
        }
        return new MagneticStormTuning(
                flag(context, ChartModifier.CHART_MAGNETIC_STORM_ION_FRONT),
                context.anomalyPercent(ChartModifier.CHART_MAGNETIC_STORM_ION_FRONT_CRAFTING_ITEM_PERCENT.anomalyTuningKey()),
                flag(context, ChartModifier.CHART_MAGNETIC_STORM_FLARE_AURORAS),
                count(context, ChartModifier.CHART_MAGNETIC_STORM_LUCKY_MODIFIERS));
    }

    private static boolean flag(ChartGenContext context, ChartModifier line) {
        return context.anomalyPercent(line.anomalyTuningKey()) > 0f;
    }

    private static int count(ChartGenContext context, ChartModifier line) {
        return Math.max(0, Math.round(context.anomalyPercent(line.anomalyTuningKey())));
    }

    public boolean setsOffAuroras() {
        return ionFrontAuroras || flareAuroras;
    }
}
