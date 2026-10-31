package exiledsector.charts.anomaly.facility;

import exiledsector.charts.run.ChartRun;

public final class FacilityStates {

    static final String PLANET_KEY = PreCollapseFacility.ANOMALY_ID + ".planet";
    static final String STAGE_KEY = PreCollapseFacility.ANOMALY_ID + ".stage";

    public enum Stage {
        GUARDED,
        OPEN,
        SALVAGED
    }

    private FacilityStates() {
    }

    static void record(ChartRun run, String planetId) {
        run.recordAnomalyState(PLANET_KEY, planetId);
        run.recordAnomalyState(STAGE_KEY, Stage.GUARDED.name());
    }

    public static String planetId(ChartRun run) {
        return run == null ? null : run.anomalyState(PLANET_KEY);
    }

    public static boolean holds(ChartRun run, String planetId) {
        return planetId != null && planetId.equals(planetId(run));
    }

    public static Stage stage(ChartRun run) {
        String stored = planetId(run) == null ? null : run.anomalyState(STAGE_KEY);
        if (stored == null) {
            return null;
        }
        try {
            return Stage.valueOf(stored);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean standing(ChartRun run) {
        Stage stage = stage(run);
        return stage != null && stage != Stage.SALVAGED;
    }

    static boolean advance(ChartRun run, Stage from, Stage to) {
        if (run == null || stage(run) != from) {
            return false;
        }
        run.recordAnomalyState(STAGE_KEY, to.name());
        return true;
    }
}
