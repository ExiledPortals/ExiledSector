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

    static AnomalyState of(ChartRun run, ForgeSite site) {
        return AnomalyState.of(run, site.anomalyId());
    }

    static void record(AnomalyState state, List<ForgePrograms.Program> programs, int runs) {
        state.put(PROGRAMS_FIELD, StateFields.join(programs.stream().map(ForgePrograms.Program::id).toList()));
        state.put(RUNS_FIELD, runs);
    }

    public static List<ForgePrograms.Program> programs(ChartRun run, ForgeSite site) {
        List<ForgePrograms.Program> programs = new ArrayList<>();
        for (String id : StateFields.list(of(run, site).text(PROGRAMS_FIELD))) {
            ForgePrograms.Program program = ForgePrograms.byId(id);
            if (program != null) {
                programs.add(program);
            }
        }
        return programs;
    }

    public static int runsLeft(ChartRun run, ForgeSite site) {
        return Math.max(0, of(run, site).integer(RUNS_FIELD));
    }

    static boolean reachControlRoom(ChartRun run, ForgeSite site, ForgePrograms.Program program) {
        AnomalyState state = of(run, site);
        List<String> reached = StateFields.list(state.text(REACHED_FIELD));
        if (reached.contains(program.id())) {
            return false;
        }
        reached.add(program.id());
        state.put(REACHED_FIELD, StateFields.join(reached));
        return true;
    }

    static boolean spendRun(ChartRun run, ForgeSite site) {
        int left = runsLeft(run, site);
        if (left <= 0) {
            return false;
        }
        of(run, site).put(RUNS_FIELD, left - 1);
        return true;
    }
}
