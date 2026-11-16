package exiledsector.charts.anomaly.listeningpost;

import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;

record DepotTuning(boolean garrisonPickets, boolean instantAlarm, int unseenPrototypes, boolean raidOnly, int extraPrototypes, boolean huntingPickets,
                   boolean picketPrototypes, boolean armedPost, boolean purgeHeld, int ledgerRewards, int maxStolenCharts) {

    static final DepotTuning NONE = new DepotTuning(false, false, 0, false, 0, false, false, false, false, 0, 0);

    static DepotTuning of(ChartRun run) {
        return run == null ? NONE : of(ChartRunEffects.contextOf(run));
    }

    static DepotTuning of(ChartGenContext context) {
        if (context == null) {
            return NONE;
        }
        return new DepotTuning(
                flag(context, ChartModifier.CHART_LISTENING_POST_GARRISON_PICKETS),
                flag(context, ChartModifier.CHART_LISTENING_POST_INSTANT_ALARM),
                count(context, ChartModifier.CHART_LISTENING_POST_UNSEEN_PROTOTYPES),
                flag(context, ChartModifier.CHART_LISTENING_POST_RAID_ONLY),
                count(context, ChartModifier.CHART_LISTENING_POST_PROTOTYPES),
                flag(context, ChartModifier.CHART_LISTENING_POST_HUNTING_PICKETS),
                flag(context, ChartModifier.CHART_LISTENING_POST_PICKET_PROTOTYPES),
                flag(context, ChartModifier.CHART_LISTENING_POST_ARMED_STATION),
                flag(context, ChartModifier.CHART_LISTENING_POST_PURGE_HELD),
                count(context, ChartModifier.CHART_LISTENING_POST_OFFICER_LEDGER),
                count(context, ChartModifier.CHART_LISTENING_POST_STOLEN_CHARTS));
    }

    private static boolean flag(ChartGenContext context, ChartModifier line) {
        return context.anomalyPercent(line.anomalyTuningKey()) > 0f;
    }

    private static int count(ChartGenContext context, ChartModifier line) {
        return Math.max(0, Math.round(context.anomalyPercent(line.anomalyTuningKey())));
    }

    int prototypePicks(boolean reachedUnseen) {
        return reachedUnseen ? VaultPrototypes.PICKS + unseenPrototypes + extraPrototypes : 1 + extraPrototypes;
    }
}
