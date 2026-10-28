package exiledsector.charts.anomaly.heist;

import exiledsector.charts.run.ChartRun;

import java.util.Locale;

public record PostState(float suspicion, boolean alarm, float vaultShare, boolean taken, long seed) {

    static final String KEY = ListeningPost.ID;
    static final String SEPARATOR = ",";
    static final int SEED_RADIX = Character.MAX_RADIX;

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
        return String.join(SEPARATOR, format(suspicion), alarm ? "1" : "0", format(vaultShare), taken ? "1" : "0", Long.toString(seed, SEED_RADIX));
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    static PostState decode(String encoded) {
        String[] parts = encoded == null ? new String[0] : encoded.split(SEPARATOR, -1);
        if (parts.length != 5) {
            return null;
        }
        try {
            return new PostState(Float.parseFloat(parts[0]), "1".equals(parts[1]), Float.parseFloat(parts[2]), "1".equals(parts[3]),
                    Long.parseLong(parts[4], SEED_RADIX));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static PostState of(ChartRun run) {
        return run == null ? null : decode(run.anomalyState(KEY));
    }

    static void record(ChartRun run, PostState state) {
        if (run != null && state != null) {
            run.recordAnomalyState(KEY, state.encode());
        }
    }
}
