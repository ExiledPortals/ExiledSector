package exiledsector.charts.anomaly.forge;

import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;

public final class ForgeStates {

    static final String PROGRAMS_KEY = DerelictForge.ID + ".offered";
    static final String RUNS_KEY = DerelictForge.ID + ".runsLeft";
    static final String PROGRAM_SEPARATOR = ",";

    private ForgeStates() {
    }

    static void record(ChartRun run, List<ForgePrograms.Program> programs, int runs) {
        run.recordAnomalyState(PROGRAMS_KEY, String.join(PROGRAM_SEPARATOR, programs.stream().map(ForgePrograms.Program::id).toList()));
        run.recordAnomalyState(RUNS_KEY, Integer.toString(runs));
    }

    public static List<ForgePrograms.Program> programs(ChartRun run) {
        String stored = run == null ? null : run.anomalyState(PROGRAMS_KEY);
        List<ForgePrograms.Program> programs = new ArrayList<>();
        if (stored != null) {
            for (String id : stored.split(PROGRAM_SEPARATOR)) {
                ForgePrograms.Program program = ForgePrograms.byId(id);
                if (program != null) {
                    programs.add(program);
                }
            }
        }
        return programs;
    }

    public static int runsLeft(ChartRun run) {
        String stored = run == null ? null : run.anomalyState(RUNS_KEY);
        if (stored == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(stored));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static boolean spendRun(ChartRun run) {
        int left = runsLeft(run);
        if (left <= 0) {
            return false;
        }
        run.recordAnomalyState(RUNS_KEY, Integer.toString(left - 1));
        return true;
    }
}
