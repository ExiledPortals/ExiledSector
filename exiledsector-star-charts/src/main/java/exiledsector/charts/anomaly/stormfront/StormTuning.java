package exiledsector.charts.anomaly.stormfront;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

record StormTuning(float heldSpeedReducedPercent, int redactedFrigatesMost, boolean captured, float capturedImmutableChancePercent,
                   boolean luckyBiomeModifiers, int officerExtraRewards) {

    static final StormTuning NONE = new StormTuning(0f, 0, false, 0f, false, 0);

    static StormTuning of(ChartRun run) {
        return run == null ? NONE : of(ChartRunEffects.contextOf(run));
    }

    static StormTuning of(ChartGenContext context) {
        if (context == null) {
            return NONE;
        }
        return new StormTuning(
                percent(context, ChartModifier.CHART_STORM_FRONT_HELD_SPEED_PERCENT),
                count(context, ChartModifier.CHART_STORM_FRONT_REDACTED_FRIGATES),
                flag(context, ChartModifier.CHART_STORM_FRONT_CAPTURED),
                percent(context, ChartModifier.CHART_STORM_FRONT_CAPTURED_IMMUTABLE_CHANCE_PERCENT),
                flag(context, ChartModifier.CHART_STORM_FRONT_LUCKY_BIOME_MODIFIERS),
                count(context, ChartModifier.CHART_STORM_FRONT_OFFICER_EXTRA_REWARDS));
    }

    private static float percent(ChartGenContext context, ChartModifier line) {
        return context.anomalyPercent(line.anomalyTuningKey());
    }

    private static boolean flag(ChartGenContext context, ChartModifier line) {
        return percent(context, line) > 0f;
    }

    private static int count(ChartGenContext context, ChartModifier line) {
        return Math.max(0, Math.round(percent(context, line)));
    }

    int redactedFrigatesFewest() {
        return StormRules.redactedFrigatesFewest(redactedFrigatesMost);
    }
}
