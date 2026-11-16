package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.LocationAPI;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;

public final class BreachLines {

    public static final String WARPED_KEY = ChartModifier.CHART_BREACH_WARPED_CHANCE_PERCENT.anomalyTuningKey();
    public static final String HOLD_KEY = ChartModifier.CHART_BREACH_HOLD_SECONDS.anomalyTuningKey();
    public static final String HELD_DRAIN_KEY = ChartModifier.CHART_BREACH_HELD_DRAIN_PERCENT.anomalyTuningKey();
    public static final String DRIFT_KEY = ChartModifier.CHART_BREACH_DRIFT.anomalyTuningKey();
    public static final String LIGHTNING_KEY = ChartModifier.CHART_BREACH_LIGHTNING.anomalyTuningKey();
    public static final String BATTLE_HULL_ITEM_KEY = ChartModifier.CHART_BREACH_BATTLE_HULL_ITEM_PERCENT.anomalyTuningKey();
    public static final String FLEET_WARPED_KEY = ChartModifier.CHART_BREACH_FLEET_WARPED_CHANCE_PERCENT.anomalyTuningKey();
    public static final String INCURSION_KEY = ChartModifier.CHART_BREACH_INCURSION.anomalyTuningKey();
    public static final String WAKES_ANOMALOUS_KEY = ChartModifier.CHART_BREACH_WAKES_ANOMALOUS_FLEETS.anomalyTuningKey();
    public static final String ANOMALOUS_WARPED_KEY = ChartModifier.CHART_BREACH_ANOMALOUS_FLEETS_WARPED.anomalyTuningKey();
    public static final String OFFICER_DUPLICATES_KEY = ChartModifier.CHART_BREACH_OFFICER_DUPLICATED_REWARDS.anomalyTuningKey();
    public static final String OFFICER_OFFERS_KEY = ChartModifier.CHART_BREACH_OFFICER_EXTRA_OFFERS.anomalyTuningKey();
    public static final String EVENT_HORIZON_KEY = ChartModifier.CHART_BLACK_HOLE_EVENT_HORIZON.anomalyTuningKey();
    public static final String WARPED_DERELICTS_KEY = ChartModifier.CHART_BLACK_HOLE_WARPED_DERELICTS.anomalyTuningKey();

    private BreachLines() {
    }

    static boolean on(ChartGenContext context, String tuningKey) {
        return context != null && context.anomalyPercent(tuningKey) > 0f;
    }

    static int count(ChartGenContext context, String tuningKey) {
        return context == null ? 0 : Math.max(0, Math.round(context.anomalyPercent(tuningKey)));
    }

    static ChartGenContext contextIn(LocationAPI location) {
        return ChartRunEffects.contextOf(ChartRuns.liveIn(location));
    }
}
