package exiledsector.skills.loader;

import exiledsector.skills.SkillNode;
import exiledsector.skills.layout.ConnectorCurve;
import exiledsector.skills.layout.SkillTreeObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TreeRegionFilter {

    private TreeRegionFilter() {
    }

    public static Set<String> disabledNodeIds(Collection<SkillNode> nodes, Set<String> disabledRegions) {
        Set<String> disabled = new HashSet<>();
        if (disabledRegions.isEmpty()) {
            return disabled;
        }
        for (SkillNode node : nodes) {
            if (isDisabled(node, disabledRegions)) {
                disabled.add(node.getId());
            }
        }
        for (SkillNode node : nodes) {
            if (node.getPairedNodeId() != null && disabled.contains(node.getPairedNodeId())) {
                disabled.add(node.getId());
            }
        }
        return disabled;
    }

    public static SkillTreeLoader.ParsedTree apply(SkillTreeLoader.ParsedTree tree, Set<String> disabledRegions) {
        if (disabledRegions.isEmpty()) {
            return tree;
        }
        Set<String> removed = disabledNodeIds(tree.nodes, disabledRegions);
        List<SkillNode> nodes = tree.nodes.stream()
                .filter(node -> !removed.contains(node.getId()))
                .map(node -> node.withoutConnectionsTo(removed))
                .toList();
        Map<String, ConnectorCurve> curves = new LinkedHashMap<>();
        tree.connectorCurves.forEach((key, curve) -> {
            if (keepsConnector(key, removed)) {
                curves.put(key, curve);
            }
        });
        Set<String> hiddenConnectors = new HashSet<>();
        for (String key : tree.hiddenConnectors) {
            if (keepsConnector(key, removed)) {
                hiddenConnectors.add(key);
            }
        }
        return new SkillTreeLoader.ParsedTree(nodes, tree.declaredNodeCount, curves, hiddenConnectors,
                visible(tree.staticImages, disabledRegions), visible(tree.ringBelts, disabledRegions),
                visible(tree.stars, disabledRegions));
    }

    private static boolean isDisabled(SkillTreeObject object, Set<String> disabledRegions) {
        String region = object.getRegion();
        return region != null && disabledRegions.contains(region);
    }

    private static boolean keepsConnector(String key, Set<String> removed) {
        int separator = key.indexOf('|');
        return separator < 0 || !removed.contains(key.substring(0, separator)) && !removed.contains(key.substring(separator + 1));
    }

    private static <T extends SkillTreeObject> List<T> visible(List<T> objects, Set<String> disabledRegions) {
        return objects.stream().filter(object -> !isDisabled(object, disabledRegions)).toList();
    }
}
