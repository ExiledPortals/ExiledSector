package exiledsector.charts.anomaly.relay;

import exiledsector.charts.run.ChartRun;

public final class RelayStates {

    public static final int PAID = 1;
    public static final int SYNCED = 2;
    public static final int WOKEN = 4;
    static final String KEY_PREFIX = RelayNetwork.ANOMALY_ID + ".";

    private RelayStates() {
    }

    public static String key(RelayKind kind) {
        return KEY_PREFIX + kind.key();
    }

    public static int flags(ChartRun run, RelayKind kind) {
        String stored = run == null ? null : run.anomalyState(key(kind));
        if (stored == null) {
            return 0;
        }
        try {
            return Integer.parseInt(stored);
        } catch (NumberFormatException e) {
            return 0;
        }
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
        run.recordAnomalyState(key(kind), Integer.toString(current | flag));
        return true;
    }

    static boolean readyToWake(ChartRun run, RelayKind kind) {
        int current = flags(run, kind);
        return (current & WOKEN) == 0 && (current & PAID) != 0 && (current & SYNCED) != 0;
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
