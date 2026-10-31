package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.ChartIds;
import exiledsector.persistence.SaveData;

public final class ChartRuns {

    static final String DATA_KEY = "exiledSector_chartRun";

    private ChartRuns() {
    }

    public static ChartRun currentOrNull() {
        return SaveData.findOrNull(DATA_KEY, ChartRun.class);
    }

    public static ChartRun live() {
        ChartRun run = currentOrNull();
        return run != null && run.runState().isLive() ? run : null;
    }

    public static ChartRun live(String runId) {
        ChartRun run = live();
        return run != null && run.runId().equals(runId) ? run : null;
    }

    public static ChartRun liveIn(LocationAPI location) {
        ChartRun run = live();
        return run != null && location != null && run.systemId().equals(ChartIds.locationIdOf(location)) ? run : null;
    }

    public static ChartRun liveOwning(SectorEntityToken entity) {
        ChartRun run = live();
        return run != null && entity != null && ChartIds.belongsToRun(entity, run.runId()) ? run : null;
    }

    public static ChartRun liveWith(String anomalyId) {
        ChartRun run = live();
        return run != null && run.anomalyIds().contains(anomalyId) ? run : null;
    }

    public static ChartRun unclosed(String runId) {
        ChartRun run = currentOrNull();
        return run != null && run.runState() != ChartRunState.CLOSED && run.runId().equals(runId) ? run : null;
    }

    public static ChartRun unclosedOwning(SectorEntityToken entity) {
        ChartRun run = currentOrNull();
        return run != null && entity != null && run.runState() != ChartRunState.CLOSED && ChartIds.belongsToRun(entity, run.runId()) ? run : null;
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
