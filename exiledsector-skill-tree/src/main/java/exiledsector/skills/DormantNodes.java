package exiledsector.skills;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DormantNodes {

    private DormantNodes() {
    }

    public interface WakeCheck {
        boolean allows(SkillNode dormantNode);
    }

    public static boolean hasReturningDormantNodes(ShipSkillData data, Map<String, SkillNode> tree) {
        for (String nodeId : data.getDormantNodeIds()) {
            if (tree.containsKey(nodeId)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> wakeDormantNodes(ShipSkillData data, Map<String, SkillNode> tree, int maxAllocatedNodes, WakeCheck wakeCheck) {
        if (data.getDormantNodeIds().isEmpty()) {
            return List.of();
        }
        data.revision++;
        ItemChargeLedger.ensure(data);
        String satisfiedRootId = ShipTrees.resolveStartingRootId(data, tree.values());
        SkillTreeTopology wakeTopology = ShipTreeLinks.grantedNodesBySocket(data).isEmpty() ? null : SkillTreeTopology.of(tree.values());
        boolean wokeAny = true;
        while (wokeAny) {
            wokeAny = false;
            Set<String> grantOnlyNodeIds = wakeTopology == null ? Set.of() : grantHeldOffPath(data, wakeTopology, satisfiedRootId);
            for (String nodeId : List.copyOf(data.dormantNodeIds)) {
                SkillNode node = tree.get(nodeId);
                if (node != null && (!data.isGrantFree(nodeId) || ShipTreeLinks.grantingSocket(data, nodeId) != null)
                        && isReconnected(data, node, satisfiedRootId, grantOnlyNodeIds)) {
                    wokeAny |= wake(data, node, satisfiedRootId, maxAllocatedNodes, wakeCheck);
                }
            }
        }
        for (String nodeId : List.copyOf(data.dormantNodeIds)) {
            SkillNode node = tree.get(nodeId);
            if (node != null && node.getType().getTier() != SkillTier.WORMHOLE_EXIT && ShipTreeLinks.grantingSocket(data, nodeId) != null) {
                wake(data, node, satisfiedRootId, maxAllocatedNodes, wakeCheck);
            }
        }
        List<String> released = new ArrayList<>();
        SkillTreeTopology topology = null;
        for (String nodeId : List.copyOf(data.dormantNodeIds)) {
            SkillNode node = tree.get(nodeId);
            if (node == null) {
                continue;
            }
            String partnerId = ShipTreeLinks.wormholePartner(data, node);
            boolean freePartnerOfAllocatedEnd = data.pairedFreeNodeIds().contains(nodeId) && data.allocatedNodeIds.contains(partnerId)
                    && ShipTreeLinks.anchorPartner(data, nodeId) == null;
            data.dormantNodeIds.remove(nodeId);
            data.release(nodeId);
            released.add(nodeId);
            SkillNode allocatedEnd = freePartnerOfAllocatedEnd ? tree.get(partnerId) : null;
            if (allocatedEnd != null) {
                topology = topology != null ? topology : SkillTreeTopology.of(tree.values());
                if (ShipTrees.canDeallocate(data, allocatedEnd, topology, satisfiedRootId)) {
                    ShipTrees.deallocate(data, allocatedEnd);
                    released.add(allocatedEnd.getId());
                }
            }
        }
        return released;
    }

    private static boolean wake(ShipSkillData data, SkillNode node, String satisfiedRootId, int maxAllocatedNodes, WakeCheck wakeCheck) {
        if (!data.isGrantFree(node.getId()) && data.usedNodeSlots() >= maxAllocatedNodes || !wakeCheck.allows(node)) {
            return false;
        }
        String nodeId = node.getId();
        data.revision++;
        data.dormantNodeIds.remove(nodeId);
        data.allocatedNodeIds.add(nodeId);
        if (!data.isFreeNode(nodeId) && !nodeId.equals(satisfiedRootId)) {
            data.charge(nodeId, 1);
        }
        return true;
    }

    private static Set<String> grantHeldOffPath(ShipSkillData data, SkillTreeTopology topology, String satisfiedRootId) {
        Set<String> pathReached = ShipTrees.pathReachableAllocatedNodeIds(data, topology, satisfiedRootId);
        Set<String> grantHeld = new HashSet<>();
        for (String grantedNodeId : ShipTreeLinks.grantedNodeIds(data)) {
            if (data.isAllocated(grantedNodeId) && !pathReached.contains(grantedNodeId)) {
                grantHeld.add(grantedNodeId);
            }
        }
        return grantHeld;
    }

    private static boolean isReconnected(ShipSkillData data, SkillNode node, String satisfiedRootId, Set<String> grantOnlyNodeIds) {
        if (node.getType().getTier() == SkillTier.WORMHOLE_EXIT) {
            String socketId = ShipTreeLinks.anchorSocket(data, node.getId());
            return socketId != null && data.isAllocated(socketId);
        }
        for (String connectedId : node.getConnectedNodeIds()) {
            if (data.isSatisfied(connectedId, satisfiedRootId) && !grantOnlyNodeIds.contains(connectedId)) {
                return true;
            }
        }
        return false;
    }
}
