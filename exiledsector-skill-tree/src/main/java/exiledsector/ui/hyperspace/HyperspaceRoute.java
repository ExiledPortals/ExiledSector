package exiledsector.ui.hyperspace;

import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record HyperspaceRoute(HyperspaceAnchor from, HyperspaceAnchor to) {

    public static List<HyperspaceRoute> between(List<HyperspaceAnchor> anchors, List<SkillTreeTopology.WormholePair> pairs) {
        Map<String, HyperspaceAnchor> anchorByRegion = new HashMap<>();
        for (HyperspaceAnchor anchor : anchors) {
            anchorByRegion.putIfAbsent(anchor.region(), anchor);
        }
        List<HyperspaceRoute> routes = new ArrayList<>();
        Set<String> seenRouteKeys = new HashSet<>();
        for (SkillTreeTopology.WormholePair pair : pairs) {
            HyperspaceAnchor from = anchorByRegion.get(pair.first().getRegion());
            HyperspaceAnchor to = anchorByRegion.get(pair.second().getRegion());
            if (from == null || to == null || from.equals(to)) {
                continue;
            }
            if (seenRouteKeys.add(SkillTree.curveKey(from.id(), to.id()))) {
                routes.add(new HyperspaceRoute(from, to));
            }
        }
        return routes;
    }
}
