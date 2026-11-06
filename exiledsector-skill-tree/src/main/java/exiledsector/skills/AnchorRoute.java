package exiledsector.skills;

import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public final class AnchorRoute {

    public record Anchor(String itemId, String exitNodeId) {
    }

    public record Route(Anchor anchor, SkillNode socket) {
    }

    private AnchorRoute() {
    }

    public static Route cheapest(SkillNode target, List<Anchor> anchors, BiPredicate<SkillNode, Anchor> socketAccepts, AllocationCheck gate,
                                 SkillTreeTopology topology, Predicate<SkillNode> hidden) {
        if (target == null || anchors.isEmpty() || gate.data().isAllocated(target.getId()) || hidden.test(target)) {
            return null;
        }
        TreeSearch fromTree = AllocationPath.search(Set.of(), gate, topology, hidden);
        Route cheapestRoute = null;
        int cheapestCost = Integer.MAX_VALUE;
        for (Anchor anchor : anchors) {
            int beyondExit = stepsBeyondExit(target, anchor.exitNodeId(), gate, topology, hidden);
            SkillNode socket = beyondExit < 0 ? null : nearestSocket(fromTree, anchor, socketAccepts, gate.data(), topology);
            if (socket == null) {
                continue;
            }
            int cost = fromTree.distance(socket.getId()) + beyondExit;
            if (cost < cheapestCost) {
                cheapestRoute = new Route(anchor, socket);
                cheapestCost = cost;
            }
        }
        return cheapestRoute;
    }

    private static int stepsBeyondExit(SkillNode target, String exitNodeId, AllocationCheck gate, SkillTreeTopology topology, Predicate<SkillNode> hidden) {
        SkillNode exit = exitNodeId == null ? null : topology.node(exitNodeId);
        if (exit == null || gate.data().isAllocated(exitNodeId) || hidden.test(exit)) {
            return -1;
        }
        if (target.getId().equals(exitNodeId)) {
            return 0;
        }
        List<SkillNode> path = AllocationPath.route(target, Set.of(exitNodeId), gate, topology, hidden);
        return path.isEmpty() ? -1 : path.size();
    }

    private static SkillNode nearestSocket(TreeSearch fromTree, Anchor anchor, BiPredicate<SkillNode, Anchor> socketAccepts, ShipSkillData data,
                                           SkillTreeTopology topology) {
        for (String reachedId : fromTree.reached()) {
            SkillNode reached = topology.node(reachedId);
            if (reached != null && reached.getType().getTier() == SkillTier.SOCKET && data.getSocketedItem(reachedId) == null
                    && socketAccepts.test(reached, anchor)) {
                return reached;
            }
        }
        return null;
    }
}
