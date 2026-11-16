package exiledsector.charts.anomaly.relay;

import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;

public final class RelayStates {

    public static final int PAID = 1;
    public static final int SYNCED = 2;
    public static final int WOKEN = 4;
    static final String SETTLED_FIELD = "settled";
    static final String EXTRA_RELAY_FIELD = "extraRelay";

    private RelayStates() {
    }

    static AnomalyState of(ChartRun run) {
        return AnomalyState.of(run, RelayNetwork.ANOMALY_ID);
    }

    public static List<RelaySlot> slots(ChartRun run) {
        List<RelaySlot> slots = new ArrayList<>();
        for (RelayKind kind : RelayKind.values()) {
            slots.add(RelaySlot.of(kind));
        }
        RelayKind extraKind = RelayKind.parse(of(run).text(EXTRA_RELAY_FIELD));
        if (extraKind != null) {
            slots.add(RelaySlot.extraOf(extraKind));
        }
        return slots;
    }

    static void recordExtraRelay(AnomalyState state, RelayKind kind) {
        state.put(EXTRA_RELAY_FIELD, kind.key());
    }

    public static int flags(ChartRun run, RelaySlot slot) {
        return of(run).integer(slot.key());
    }

    public static boolean has(ChartRun run, RelaySlot slot, int flag) {
        return (flags(run, slot) & flag) != 0;
    }

    public static boolean isWoken(ChartRun run, RelaySlot slot) {
        return has(run, slot, WOKEN);
    }

    static boolean add(ChartRun run, RelaySlot slot, int flag) {
        int current = flags(run, slot);
        if (run == null || (current & flag) == flag) {
            return false;
        }
        of(run).put(slot.key(), current | flag);
        return true;
    }

    static boolean readyToWake(ChartRun run, RelaySlot slot) {
        int current = flags(run, slot);
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

    static int awake(ChartRun run) {
        int awake = 0;
        for (RelaySlot slot : slots(run)) {
            if (isWoken(run, slot)) {
                awake++;
            }
        }
        return awake;
    }

    public static boolean allWoken(ChartRun run) {
        if (run == null || !run.anomalyIds().contains(RelayNetwork.ANOMALY_ID)) {
            return false;
        }
        return awake(run) == slots(run).size();
    }
}
