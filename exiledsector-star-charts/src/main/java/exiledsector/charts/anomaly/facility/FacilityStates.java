package exiledsector.charts.anomaly.facility;

import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.run.ChartRun;

public final class FacilityStates {

    static final String PLANET_FIELD = "planet";
    static final String STAGE_FIELD = "stage";

    public enum Stage {
        GUARDED,
        OPEN,
        SALVAGED
    }

    private FacilityStates() {
    }

    static AnomalyState of(ChartRun run) {
        return AnomalyState.of(run, PreCollapseFacility.ANOMALY_ID);
    }

    static void record(AnomalyState state, String planetId) {
        state.put(PLANET_FIELD, planetId);
        state.put(STAGE_FIELD, Stage.GUARDED);
    }

    public static String planetId(ChartRun run) {
        return of(run).text(PLANET_FIELD);
    }

    public static boolean holds(ChartRun run, String planetId) {
        return planetId != null && planetId.equals(planetId(run));
    }

    public static Stage stage(ChartRun run) {
        AnomalyState state = of(run);
        return state.has(PLANET_FIELD) ? state.enumValue(STAGE_FIELD, Stage.class) : null;
    }

    public static boolean standing(ChartRun run) {
        Stage stage = stage(run);
        return stage != null && stage != Stage.SALVAGED;
    }

    static boolean advance(ChartRun run, Stage from, Stage to) {
        if (run == null || stage(run) != from) {
            return false;
        }
        of(run).put(STAGE_FIELD, to);
        return true;
    }
}
