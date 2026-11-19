package exiledsector.charts.anomaly;

import exiledsector.charts.anomaly.stormfront.StormFront;
import exiledsector.charts.run.ChartRun;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Message;
import exiledsector.i18n.Translation;

import java.util.List;
import java.util.Set;

public final class SettlingDust {

    public static final int BASE_MAX_STACKS = 10;
    public static final float STAR_CHART_PERCENT_PER_STACK = 10f;
    static final Set<String> IONIZED_ANOMALY_IDS = Set.of(MagneticStorm.ANOMALY_ID, StormFront.ANOMALY_ID);
    static final String STATE_ID = "settling_dust";
    static final String STACKS_FIELD = "stacks";
    static final String STACKS_KEY = STATE_ID + AnomalyState.FIELD_SEPARATOR + STACKS_FIELD;
    private static final String INTEL_KEY = "chart.anomaly.dense_nebula.intel";

    private SettlingDust() {
    }

    public static boolean gathersIn(ChartRun run, SettlingDustTuning tuning) {
        if (run == null) {
            return false;
        }
        List<String> anomalyIds = run.anomalyIds();
        return anomalyIds.contains(DenseNebula.ANOMALY_ID) || tuning.ionized() && anomalyIds.stream().anyMatch(IONIZED_ANOMALY_IDS::contains);
    }

    public static int stacks(ChartRun run) {
        return stacks(run, SettlingDustTuning.ofRun(run));
    }

    public static int stacks(ChartRun run, SettlingDustTuning tuning) {
        return gathersIn(run, tuning) ? Math.max(0, Math.min(tuning.maxStacks(), storedStacks(run))) : 0;
    }

    public static int storedStacks(ChartRun run) {
        String stored = run == null ? null : run.anomalyState(STACKS_KEY);
        try {
            return stored == null ? 0 : Integer.parseInt(stored);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void battleWon(AnomalyRun anomalyRun) {
        ChartRun run = anomalyRun.run();
        SettlingDustTuning tuning = SettlingDustTuning.ofRun(run);
        int stacks = stacks(run, tuning);
        if (gathersIn(run, tuning) && stacks < tuning.maxStacks()) {
            AnomalyState.of(run, STATE_ID).put(STACKS_FIELD, stacks + 1);
            SettlingDustFleets.grow(anomalyRun, tuning.fleetSizePercentPerStack() * (stacks + 1));
        }
    }

    public static float bossStarChartPercent(ChartRun run) {
        SettlingDustTuning tuning = SettlingDustTuning.ofRun(run);
        return stacks(run, tuning) * STAR_CHART_PERCENT_PER_STACK * tuning.effectMult();
    }

    public static float quantityPercent(ChartRun run, SettlingDustTuning tuning) {
        return stacks(run, tuning) * tuning.quantityPercentPerStack() * tuning.effectMult();
    }

    public static String intelText(ChartRun run) {
        SettlingDustTuning tuning = SettlingDustTuning.ofRun(run);
        if (!gathersIn(run, tuning)) {
            return null;
        }
        int stacks = stacks(run, tuning);
        int starChartPercent = Math.round(stacks * STAR_CHART_PERCENT_PER_STACK * tuning.effectMult());
        int quantityPercent = Math.round(quantityPercent(run, tuning));
        return I18n.forGameText(() -> {
            Message message = Translation.msg(quantityPercent > 0 ? INTEL_KEY + ".quantity" : INTEL_KEY);
            return message.arg("count", stacks).arg("max", tuning.maxStacks()).arg("value", starChartPercent).arg("quantity", quantityPercent).text();
        });
    }
}
