package exiledsector.skills;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RespecPlan {

    private RespecPlan() {
    }

    public static List<SkillNode> of(ShipSkillData data, SkillTreeTopology topology, String startingRootId, SkillNode selected) {
        if (selected == null || !data.isAllocated(selected.getId())) {
            return List.of();
        }
        Set<String> removed = new LinkedHashSet<>();
        removed.add(selected.getId());
        String pairedId = ShipTreeLinks.wormholePartner(data, selected);
        if (pairedId != null && data.isAllocated(pairedId)) {
            removed.add(pairedId);
        }
        Set<String> dependents = dependents(data, topology, startingRootId, selected, removed);
        Set<String> grantedDependents = new LinkedHashSet<>(ShipTrees.grantOnlyNodeIds(data, topology, startingRootId));
        grantedDependents.retainAll(dependents);
        dependents.removeAll(grantedDependents);
        Map<String, Integer> distanceById = distancesAwayFrom(removed, dependents, topology, ShipTreeLinks.wormholePartners(data));

        List<String> ordered = new ArrayList<>(dependents);
        ordered.sort(Comparator.comparingInt((String id) -> distanceById.getOrDefault(id, Integer.MAX_VALUE)).reversed()
                .thenComparing(Comparator.naturalOrder()));
        List<SkillNode> plan = new ArrayList<>();
        Set<String> covered = new HashSet<>();
        for (String id : ordered) {
            SkillNode node = topology.node(id);
            if (node == null || !covered.add(id)) {
                continue;
            }
            String partnerId = ShipTreeLinks.wormholePartner(data, node);
            if (partnerId != null) {
                covered.add(partnerId);
            }
            plan.add(node);
        }
        List<SkillNode> grantedNodes = new ArrayList<>();
        for (String grantedId : grantedDependents) {
            SkillNode grantedNode = topology.node(grantedId);
            if (grantedNode != null) {
                grantedNodes.add(grantedNode);
            }
        }
        if (selected.getId().equals(startingRootId)) {
            plan.addAll(grantedNodes);
            plan.add(selected);
        } else {
            plan.add(selected);
            plan.addAll(grantedNodes);
        }
        return plan;
    }

    private static Set<String> dependents(ShipSkillData data, SkillTreeTopology topology, String startingRootId,
                                          SkillNode selected, Set<String> removed) {
        Set<String> dependents;
        if (selected.getId().equals(startingRootId)) {
            dependents = new HashSet<>(data.getAllocatedNodeIds());
        } else {
            dependents = ShipTrees.reachableAllocatedNodeIds(data, topology, startingRootId, Set.of());
            dependents.removeAll(ShipTrees.reachableAllocatedNodeIds(data, topology, startingRootId, removed));
        }
        dependents.removeAll(removed);
        return dependents;
    }

    private static Map<String, Integer> distancesAwayFrom(Set<String> removed, Set<String> dependents, SkillTreeTopology topology,
                                                          WormholePartners partners) {
        TreeSearch search = TreeSearch.from(topology, removed, child -> dependents.contains(child.getId()));
        Map<String, Integer> distanceById = new HashMap<>();
        search.reached().forEach(id -> distanceById.put(id, search.distance(id)));
        for (String id : dependents) {
            SkillNode node = topology.node(id);
            String partner = node == null ? null : partners.wormholePartner(node);
            if (partner != null && dependents.contains(partner)) {
                int nearer = Math.min(distanceById.getOrDefault(id, Integer.MAX_VALUE), distanceById.getOrDefault(partner, Integer.MAX_VALUE));
                distanceById.put(id, nearer);
                distanceById.put(partner, nearer);
            }
        }
        return distanceById;
    }
}
