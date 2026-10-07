package exiledsector.skills.template;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TemplateCapture {

    private TemplateCapture() {
    }

    public static List<TemplateStep> capture(ShipSkillData shipData, String startingRootId, Map<String, SkillNode> nodesById) {
        List<String> unplacedNodeIds = new ArrayList<>();
        for (String nodeId : shipData.getAllocatedNodeIds()) {
            if (!nodeId.equals(startingRootId) && nodesById.containsKey(nodeId)) {
                unplacedNodeIds.add(nodeId);
            }
        }
        Set<String> placedNodeIds = new HashSet<>();
        if (startingRootId != null) {
            placedNodeIds.add(startingRootId);
        }
        List<TemplateStep> steps = new ArrayList<>();
        while (!unplacedNodeIds.isEmpty()) {
            SkillNode nextNode = firstPlaceable(unplacedNodeIds, placedNodeIds, nodesById);
            if (nextNode == null) {
                break;
            }
            unplacedNodeIds.remove(nextNode.getId());
            place(nextNode, shipData, placedNodeIds, steps);
            String partnerId = nextNode.getPairedNodeId();
            if (partnerId != null && unplacedNodeIds.remove(partnerId)) {
                place(nodesById.get(partnerId), shipData, placedNodeIds, steps);
            }
        }
        for (String nodeId : unplacedNodeIds) {
            place(nodesById.get(nodeId), shipData, placedNodeIds, steps);
        }
        return steps;
    }

    private static SkillNode firstPlaceable(List<String> unplacedNodeIds, Set<String> placedNodeIds, Map<String, SkillNode> nodesById) {
        for (String nodeId : unplacedNodeIds) {
            SkillNode node = nodesById.get(nodeId);
            if (isPlaceable(node, placedNodeIds)) {
                return node;
            }
        }
        return null;
    }

    private static boolean isPlaceable(SkillNode node, Set<String> placedNodeIds) {
        List<String> connectedIds = node.getConnectedNodeIds();
        if (connectedIds.isEmpty()) {
            return true;
        }
        for (String connectedId : connectedIds) {
            if (placedNodeIds.contains(connectedId)) {
                return true;
            }
        }
        return false;
    }

    private static void place(SkillNode node, ShipSkillData shipData, Set<String> placedNodeIds, List<TemplateStep> steps) {
        placedNodeIds.add(node.getId());
        String optionTypeId = node.getType().isOptional() ? shipData.getOptionalSelection(node.getId()) : null;
        steps.add(new TemplateStep(node.getId(), optionTypeId));
    }
}
