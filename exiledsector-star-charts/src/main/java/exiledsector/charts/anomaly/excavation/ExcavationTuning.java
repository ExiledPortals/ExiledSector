package exiledsector.charts.anomaly.excavation;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

public record ExcavationTuning(boolean openShafts, boolean collapsedShafts, boolean twinShafts, int guardianInhibitors, int raidInhibitors,
                               boolean secondCavern, float secondCavernFleetSizePercent, int secondCavernInhibitors, boolean frigatesOnly,
                               boolean cruisersAllowed, float sightRadiusPercent, float openShaftInhibitorPercent, boolean corruptibleGuardians,
                               int corruptedGuardianInhibitors, float inhibitorPercentPerDust) {

    public static final ExcavationTuning NONE = of(ChartGenContext.none());

    public static ExcavationTuning of(ChartGenContext context) {
        ChartGenContext lines = ChartGenContext.orNone(context);
        return new ExcavationTuning(raised(lines, ChartModifier.CHART_EXCAVATION_OPEN_SHAFTS), raised(lines, ChartModifier.CHART_EXCAVATION_COLLAPSED_SHAFTS),
                raised(lines, ChartModifier.CHART_EXCAVATION_TWIN_SHAFTS), count(lines, ChartModifier.CHART_EXCAVATION_GUARDIAN_INHIBITORS),
                count(lines, ChartModifier.CHART_EXCAVATION_RAID_INHIBITORS), raised(lines, ChartModifier.CHART_EXCAVATION_SECOND_CAVERN),
                percent(lines, ChartModifier.CHART_EXCAVATION_SECOND_CAVERN_FLEET_SIZE_MULT), count(lines, ChartModifier.CHART_EXCAVATION_SECOND_CAVERN_INHIBITORS),
                raised(lines, ChartModifier.CHART_EXCAVATION_FRIGATES_ONLY), raised(lines, ChartModifier.CHART_EXCAVATION_CRUISERS_ALLOWED),
                percent(lines, ChartModifier.CHART_EXCAVATION_SIGHT_RADIUS_PERCENT), percent(lines, ChartModifier.CHART_EXCAVATION_OPEN_SHAFT_INHIBITOR_PERCENT),
                raised(lines, ChartModifier.CHART_EXCAVATION_CORRUPTIBLE_GUARDIANS), count(lines, ChartModifier.CHART_EXCAVATION_CORRUPTED_GUARDIAN_INHIBITORS),
                percent(lines, ChartModifier.CHART_EXCAVATION_INHIBITOR_PERCENT_PER_DUST));
    }

    public static ExcavationTuning ofRun(ChartRun run) {
        return run == null ? NONE : of(ChartRunEffects.contextOf(run));
    }

    private static boolean raised(ChartGenContext context, ChartModifier line) {
        return context.anomalyPercent(line.anomalyTuningKey()) > 0f;
    }

    private static int count(ChartGenContext context, ChartModifier line) {
        return Math.max(0, Math.round(context.anomalyPercent(line.anomalyTuningKey())));
    }

    private static float percent(ChartGenContext context, ChartModifier line) {
        return context.anomalyPercent(line.anomalyTuningKey());
    }
}
