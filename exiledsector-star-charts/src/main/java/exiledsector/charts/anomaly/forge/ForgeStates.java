package exiledsector.charts.anomaly.forge;

import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.anomaly.StateFields;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;

public final class ForgeStates {

    static final String PROGRAMS_FIELD = "offered";
    static final String RUNS_FIELD = "runsLeft";
    static final String REACHED_FIELD = "reached";

    private ForgeStates() {
    }

    static AnomalyState of(ChartRun run) {
        return AnomalyState.of(run, DerelictForge.ANOMALY_ID);
    }

    static void record(AnomalyState state, List<ForgePrograms.Program> programs, int runs) {
        state.put(PROGRAMS_FIELD, StateFields.join(programs.stream().map(ForgePrograms.Program::id).toList()));
        state.put(RUNS_FIELD, runs);
    }

    public static List<ForgePrograms.Program> programs(ChartRun run) {
        List<ForgePrograms.Program> programs = new ArrayList<>();
        for (String id : StateFields.list(of(run).text(PROGRAMS_FIELD))) {
            ForgePrograms.Program program = ForgePrograms.byId(id);
            if (program != null) {
                programs.add(program);
            }
        }
        return programs;
    }

    public static int runsLeft(ChartRun run) {
        return Math.max(0, of(run).integer(RUNS_FIELD));
    }

    static boolean reachControlRoom(ChartRun run, ForgePrograms.Program program) {
        AnomalyState state = of(run);
        List<String> reached = StateFields.list(state.text(REACHED_FIELD));
        if (reached.contains(program.id())) {
            return false;
        }
        reached.add(program.id());
        state.put(REACHED_FIELD, StateFields.join(reached));
        return true;
    }

    static boolean spendRun(ChartRun run) {
        int left = runsLeft(run);
        if (left <= 0) {
            return false;
        }
        of(run).put(RUNS_FIELD, left - 1);
        return true;
    }
}
