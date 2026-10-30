package exiledsector.charts.anomaly.excavation;

import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;

public final class ExcavationStates {

    static final String KEY_PREFIX = ExcavationSites.ID + ".";

    private ExcavationStates() {
    }

    static String key(int index) {
        return KEY_PREFIX + index;
    }

    public static void record(ChartRun run, ExcavationSite site) {
        run.recordAnomalyState(key(site.index()), site.encode());
    }

    public static List<ExcavationSite> all(ChartRun run) {
        List<ExcavationSite> sites = new ArrayList<>();
        if (run == null) {
            return sites;
        }
        for (int index = 0; run.anomalyState(key(index)) != null; index++) {
            ExcavationSite site = ExcavationSite.decode(index, run.anomalyState(key(index)));
            if (site != null) {
                sites.add(site);
            }
        }
        return sites;
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

    static boolean clear(ChartRun run, ExcavationSite site) {
        ExcavationSite current = run == null || site == null ? null : at(run, site.planetId());
        if (current == null || current.cleared()) {
            return false;
        }
        record(run, current.asCleared());
        return true;
    }
}
