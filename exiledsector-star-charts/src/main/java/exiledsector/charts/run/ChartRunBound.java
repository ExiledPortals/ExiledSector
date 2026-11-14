package exiledsector.charts.run;

public interface ChartRunBound {

    boolean belongsToRun(String runId);

    static boolean sameRun(String ownRunId, String wantedRunId) {
        return ownRunId != null && ownRunId.equals(wantedRunId);
    }
}
