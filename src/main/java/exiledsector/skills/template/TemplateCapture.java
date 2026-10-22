package exiledsector.skills.template;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.WormholePartners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class TemplateCapture {

    private TemplateCapture() {
    }

    public static List<TemplateStep> capture(ShipSkillData shipData, String startingRootId, Map<String, SkillNode> nodesById) {
        return capture(pathAllocatedNodeIds(shipData, shipData.getAllocatedNodeIds(), startingRootId), shipData::getOptionalSelection, ShipTrees.wormholePartners(shipData),
                startingRootId, nodesById);
    }

    public static List<String> pathAllocatedNodeIds(ShipSkillData shipData, Collection<String> orderedNodeIds, String startingRootId) {
        Set<String> grantOnlyNodeIds = ShipTrees.grantOnlyNodeIds(shipData, SkillTree.topology(), startingRootId);
        List<String> pathNodeIds = new ArrayList<>(orderedNodeIds);
        pathNodeIds.removeAll(grantOnlyNodeIds);
        return pathNodeIds;
    }

    public static List<TemplateStep> capture(Collection<String> orderedNodeIds, UnaryOperator<String> optionOf, WormholePartners partners,
                                             String startingRootId, Map<String, SkillNode> nodesById) {
        List<String> unplacedNodeIds = new ArrayList<>();
        for (String nodeId : orderedNodeIds) {
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
            place(nextNode, optionOf, placedNodeIds, steps);
            String partnerId = partners.wormholePartner(nextNode);
            if (partnerId != null && unplacedNodeIds.remove(partnerId)) {
                place(nodesById.get(partnerId), optionOf, placedNodeIds, steps);
            }
        }
        for (String nodeId : unplacedNodeIds) {
            place(nodesById.get(nodeId), optionOf, placedNodeIds, steps);
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

    private static void place(SkillNode node, UnaryOperator<String> optionOf, Set<String> placedNodeIds, List<TemplateStep> steps) {
        placedNodeIds.add(node.getId());
        String optionTypeId = node.getType().isOptional() ? optionOf.apply(node.getId()) : null;
        steps.add(new TemplateStep(node.getId(), optionTypeId));
    }
}
