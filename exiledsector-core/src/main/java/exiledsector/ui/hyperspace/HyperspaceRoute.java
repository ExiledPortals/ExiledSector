package exiledsector.ui.hyperspace;

import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.TreeTopology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record HyperspaceRoute(HyperspaceAnchor from, HyperspaceAnchor to) {

    public static List<HyperspaceRoute> between(List<HyperspaceAnchor> anchors, List<? extends TreeTopology.WormholePair<?>> pairs) {
        Map<String, HyperspaceAnchor> anchorByVolume = new HashMap<>();
        for (HyperspaceAnchor anchor : anchors) {
            anchorByVolume.putIfAbsent(anchor.volume(), anchor);
        }
        List<HyperspaceRoute> routes = new ArrayList<>();
        Set<String> seenRouteKeys = new HashSet<>();
        for (TreeTopology.WormholePair<?> pair : pairs) {
            HyperspaceAnchor from = anchorByVolume.get(pair.first().getVolume());
            HyperspaceAnchor to = anchorByVolume.get(pair.second().getVolume());
            if (from == null || to == null || from.equals(to)) {
                continue;
            }
            if (seenRouteKeys.add(ConnectorCurve.key(from.id(), to.id()))) {
                routes.add(new HyperspaceRoute(from, to));
            }
        }
        return routes;
    }
}
