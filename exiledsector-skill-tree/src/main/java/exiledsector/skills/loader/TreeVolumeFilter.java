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

public final class TreeVolumeFilter {

    private TreeVolumeFilter() {
    }

    public static Set<String> disabledNodeIds(Collection<SkillNode> nodes, Set<String> disabledVolumes) {
        Set<String> disabledNodeIds = new HashSet<>();
        if (disabledVolumes.isEmpty()) {
            return disabledNodeIds;
        }
        for (SkillNode node : nodes) {
            if (isDisabled(node, disabledVolumes)) {
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

    public static SkillTreeLoader.ParsedTree apply(SkillTreeLoader.ParsedTree parsedTree, Set<String> disabledVolumes) {
        if (disabledVolumes.isEmpty()) {
            return parsedTree;
        }
        Set<String> removedNodeIds = disabledNodeIds(parsedTree.nodes, disabledVolumes);
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
                parsedTree.decorations.outsideVolumes(disabledVolumes));
    }

    private static boolean isDisabled(SkillTreeObject treeObject, Set<String> disabledVolumes) {
        String volume = treeObject.getVolume();
        return volume != null && disabledVolumes.contains(volume);
    }

    private static boolean keepsConnector(String connectorKey, Set<String> removedNodeIds) {
        int separator = connectorKey.indexOf('|');
        return separator < 0 || !removedNodeIds.contains(connectorKey.substring(0, separator)) && !removedNodeIds.contains(connectorKey.substring(separator + 1));
    }
}
