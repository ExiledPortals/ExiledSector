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
        Set<String> disabledNodeIds = new HashSet<>();
        if (disabledRegions.isEmpty()) {
            return disabledNodeIds;
        }
        for (SkillNode node : nodes) {
            if (isDisabled(node, disabledRegions)) {
                disabledNodeIds.add(node.getId());
            }
        }
        for (SkillNode node : nodes) {
            if (node.getPairedNodeId() != null && disabledNodeIds.contains(node.getPairedNodeId())) {
                disabledNodeIds.add(node.getId());
            }
        }
        return disabledNodeIds;
    }

    public static SkillTreeLoader.ParsedTree apply(SkillTreeLoader.ParsedTree parsedTree, Set<String> disabledRegions) {
        if (disabledRegions.isEmpty()) {
            return parsedTree;
        }
        Set<String> removedNodeIds = disabledNodeIds(parsedTree.nodes, disabledRegions);
        List<SkillNode> keptNodes = parsedTree.nodes.stream()
                .filter(node -> !removedNodeIds.contains(node.getId()))
                .map(node -> node.withoutConnectionsTo(removedNodeIds))
                .toList();
        Map<String, ConnectorCurve> keptCurves = new LinkedHashMap<>();
        parsedTree.connectorCurves.forEach((connectorKey, curve) -> {
            if (keepsConnector(connectorKey, removedNodeIds)) {
                keptCurves.put(connectorKey, curve);
            }
        });
        Set<String> keptHiddenConnectors = new HashSet<>();
        for (String connectorKey : parsedTree.hiddenConnectors) {
            if (keepsConnector(connectorKey, removedNodeIds)) {
                keptHiddenConnectors.add(connectorKey);
            }
        }
        return new SkillTreeLoader.ParsedTree(keptNodes, parsedTree.declaredNodeCount, keptCurves, keptHiddenConnectors,
                visible(parsedTree.staticImages, disabledRegions), visible(parsedTree.ringBelts, disabledRegions),
                visible(parsedTree.stars, disabledRegions));
    }

    private static boolean isDisabled(SkillTreeObject treeObject, Set<String> disabledRegions) {
        String region = treeObject.getRegion();
        return region != null && disabledRegions.contains(region);
    }

    private static boolean keepsConnector(String connectorKey, Set<String> removedNodeIds) {
        int separator = connectorKey.indexOf('|');
        return separator < 0 || !removedNodeIds.contains(connectorKey.substring(0, separator)) && !removedNodeIds.contains(connectorKey.substring(separator + 1));
    }

    private static <T extends SkillTreeObject> List<T> visible(List<T> objects, Set<String> disabledRegions) {
        return objects.stream().filter(treeObject -> !isDisabled(treeObject, disabledRegions)).toList();
    }
}
