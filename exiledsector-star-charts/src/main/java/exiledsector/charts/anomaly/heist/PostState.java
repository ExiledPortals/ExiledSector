package exiledsector.charts.anomaly.heist;

import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.anomaly.StateFields;
import exiledsector.charts.run.ChartRun;

public record PostState(float suspicion, boolean alarm, float vaultShare, boolean taken, long seed) {

    static final String FIELD = "state";
    static final int FIELDS = 5;

    public PostState {
        suspicion = Math.max(0f, Math.min(1f, suspicion));
        vaultShare = Math.max(0f, Math.min(1f, vaultShare));
    }

    static PostState fresh(long seed) {
        return new PostState(0f, false, 1f, false, seed);
    }

    public boolean standing() {
        return !taken;
    }

    public boolean quiet() {
        return !alarm && !taken;
    }

    public boolean clean() {
        return taken && !alarm;
    }

    PostState withSuspicion(float value) {
        return new PostState(value, alarm, vaultShare, taken, seed);
    }

    PostState raised() {
        return new PostState(1f, true, vaultShare, taken, seed);
    }

    PostState withVault(float value) {
        return new PostState(suspicion, alarm, value, taken, seed);
    }

    PostState asTaken() {
        return new PostState(suspicion, alarm, vaultShare, true, seed);
    }

    String encode() {
        return StateFields.join(StateFields.decimal(suspicion), StateFields.flag(alarm), StateFields.decimal(vaultShare), StateFields.flag(taken),
                StateFields.seed(seed));
    }

    static PostState decode(String encoded) {
        return StateFields.decode(encoded, FIELDS, fields -> new PostState(fields.decimal(0), fields.flag(1), fields.decimal(2), fields.flag(3),
                fields.seed(4)));
    }

    public static PostState of(ChartRun run) {
        return decode(AnomalyState.of(run, ListeningPost.ANOMALY_ID).text(FIELD));
    }

    static void record(ChartRun run, PostState state) {
        if (run != null) {
            record(AnomalyState.of(run, ListeningPost.ANOMALY_ID), state);
        }
    }

    static void record(AnomalyState anomalyState, PostState state) {
        if (state != null) {
            anomalyState.put(FIELD, state.encode());
        }
    }
}
