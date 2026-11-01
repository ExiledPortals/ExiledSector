package exiledsector.charts.anomaly.relay;

import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.run.ChartRun;

public final class RelayStates {

    public static final int PAID = 1;
    public static final int SYNCED = 2;
    public static final int WOKEN = 4;
    static final String SETTLED_FIELD = "settled";

    private RelayStates() {
    }

    static AnomalyState of(ChartRun run) {
        return AnomalyState.of(run, RelayNetwork.ANOMALY_ID);
    }

    public static int flags(ChartRun run, RelayKind kind) {
        return of(run).integer(kind.key());
    }

    public static boolean has(ChartRun run, RelayKind kind, int flag) {
        return (flags(run, kind) & flag) != 0;
    }

    public static boolean isWoken(ChartRun run, RelayKind kind) {
        return has(run, kind, WOKEN);
    }

    static boolean add(ChartRun run, RelayKind kind, int flag) {
        int current = flags(run, kind);
        if (run == null || (current & flag) == flag) {
            return false;
        }
        of(run).put(kind.key(), current | flag);
        return true;
    }

    static boolean readyToWake(ChartRun run, RelayKind kind) {
        int current = flags(run, kind);
        return (current & WOKEN) == 0 && (current & PAID) != 0 && (current & SYNCED) != 0;
    }

    static boolean markSettled(ChartRun run) {
        AnomalyState state = of(run);
        if (state.has(SETTLED_FIELD)) {
            return false;
        }
        state.put(SETTLED_FIELD, Boolean.TRUE.toString());
        return true;
    }

    public static boolean allWoken(ChartRun run) {
        if (run == null || !run.anomalyIds().contains(RelayNetwork.ANOMALY_ID)) {
            return false;
        }
        for (RelayKind kind : RelayKind.values()) {
            if (!isWoken(run, kind)) {
                return false;
            }
        }
        return true;
    }
}
