package exiledsector.charts.anomaly;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

public record SettlingDustTuning(float effectPercent, int extraMaxStacks, float quantityPercentPerStack, float fleetSizePercentPerStack,
                                 boolean ionized) {

    public static final SettlingDustTuning NONE = of(ChartGenContext.none());
    private static final float PERCENT = 100f;

    public static SettlingDustTuning of(ChartGenContext context) {
        ChartGenContext lines = ChartGenContext.orNone(context);
        return new SettlingDustTuning(percent(lines, ChartModifier.CHART_SETTLING_DUST_EFFECT_PERCENT),
                Math.round(percent(lines, ChartModifier.CHART_SETTLING_DUST_MAX_STACKS)), percent(lines, ChartModifier.CHART_SETTLING_DUST_QUANTITY_PERCENT),
                percent(lines, ChartModifier.CHART_SETTLING_DUST_FLEET_SIZE_PERCENT), percent(lines, ChartModifier.CHART_SETTLING_DUST_IONIZED) > 0f);
    }

    public static SettlingDustTuning ofRun(ChartRun run) {
        return run == null ? NONE : of(ChartRunEffects.contextOf(run));
    }

    public int maxStacks() {
        return Math.max(0, SettlingDust.BASE_MAX_STACKS + extraMaxStacks);
    }

    public float effectMult() {
        return Math.max(0f, 1f + effectPercent / PERCENT);
    }

    private static float percent(ChartGenContext context, ChartModifier line) {
        return context.anomalyPercent(line.anomalyTuningKey());
    }
}
