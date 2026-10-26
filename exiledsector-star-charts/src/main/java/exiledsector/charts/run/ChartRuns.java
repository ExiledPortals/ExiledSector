package exiledsector.charts.run;

import exiledsector.persistence.SaveData;

public final class ChartRuns {

    static final String DATA_KEY = "exiledSector_chartRun";

    private ChartRuns() {
    }

    public static ChartRun currentOrNull() {
        return SaveData.findOrNull(DATA_KEY, ChartRun.class);
    }

    public static boolean hasRun() {
        return currentOrNull() != null;
    }

    public static boolean store(ChartRun run) {
        if (hasRun()) {
            return false;
        }
        SaveData.put(DATA_KEY, run);
        return true;
    }

    public static void replace(ChartRun run) {
        SaveData.put(DATA_KEY, run);
    }

    public static void delete(String runId) {
        ChartRun current = currentOrNull();
        if (current == null || current.runId().equals(runId)) {
            SaveData.remove(DATA_KEY);
        }
    }
}
