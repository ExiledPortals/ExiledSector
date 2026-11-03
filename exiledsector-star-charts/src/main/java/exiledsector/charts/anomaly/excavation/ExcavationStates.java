package exiledsector.charts.anomaly.excavation;

import exiledsector.charts.anomaly.AnomalyState;
import exiledsector.charts.anomaly.StateFields;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;

public final class ExcavationStates {

    static final String SCOUTED_FIELD = "scouted";

    private ExcavationStates() {
    }

    static AnomalyState of(ChartRun run) {
        return AnomalyState.of(run, ExcavationSites.ANOMALY_ID);
    }

    static void record(AnomalyState state, ExcavationSite site) {
        state.put(Integer.toString(site.index()), site.encode());
    }

    public static List<ExcavationSite> all(ChartRun run) {
        AnomalyState state = of(run);
        List<ExcavationSite> sites = new ArrayList<>();
        for (int index = 0; ; index++) {
            String stored = state.text(Integer.toString(index));
            if (stored == null) {
                return sites;
            }
            ExcavationSite site = ExcavationSite.decode(index, stored);
            if (site != null) {
                sites.add(site);
            }
        }
    }

    public static List<ExcavationSite> standing(ChartRun run) {
        return all(run).stream().filter(site -> !site.cleared()).toList();
    }

    public static ExcavationSite at(ChartRun run, String planetId) {
        if (planetId == null) {
            return null;
        }
        for (ExcavationSite site : all(run)) {
            if (planetId.equals(site.planetId())) {
                return site;
            }
        }
        return null;
    }

    public static ExcavationSite standingAt(ChartRun run, String planetId) {
        ExcavationSite site = at(run, planetId);
        return site == null || site.cleared() ? null : site;
    }

    static boolean scout(ChartRun run, ExcavationSite site) {
        AnomalyState state = of(run);
        List<String> scouted = StateFields.list(state.text(SCOUTED_FIELD));
        if (scouted.contains(site.planetId())) {
            return false;
        }
        scouted.add(site.planetId());
        state.put(SCOUTED_FIELD, StateFields.join(scouted));
        return true;
    }

    static boolean clear(ChartRun run, ExcavationSite site) {
        ExcavationSite current = run == null || site == null ? null : at(run, site.planetId());
        if (current == null || current.cleared()) {
            return false;
        }
        record(of(run), current.asCleared());
        return true;
    }
}
