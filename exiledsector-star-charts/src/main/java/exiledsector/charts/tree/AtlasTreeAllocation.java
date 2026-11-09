package exiledsector.charts.tree;

import exiledsector.skills.SkillTier;
import exiledsector.skills.TreeSearch;
import exiledsector.skills.layout.TreeTopology;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class AtlasTreeAllocation {

    private final TreeTopology<AtlasTreeNode> topology;
    private final Set<String> allocatedIds;

    private AtlasTreeAllocation(TreeTopology<AtlasTreeNode> topology, Set<String> allocatedIds) {
        this.topology = topology;
        this.allocatedIds = allocatedIds;
    }

    public static AtlasTreeAllocation of(TreeTopology<AtlasTreeNode> topology, Collection<String> storedNodeIds) {
        Set<String> allocated = new LinkedHashSet<>(topology.rootIds());
        for (String nodeId : storedNodeIds) {
            AtlasTreeNode node = topology.node(nodeId);
            if (node != null && node.getTier() != SkillTier.ROOT) {
                allocated.add(nodeId);
            }
        }
        AtlasTreeAllocation allocation = new AtlasTreeAllocation(topology, allocated);
        allocated.retainAll(allocation.connectedToRoots(allocated, null));
        return allocation;
    }

    public boolean isAllocated(String nodeId) {
        return allocatedIds.contains(nodeId);
    }

    public Set<String> storedNodeIds() {
        Set<String> stored = new TreeSet<>(allocatedIds);
        stored.removeAll(topology.rootIds());
        return stored;
    }

    public int spentPoints() {
        return storedNodeIds().size();
    }

    public boolean canAllocate(AtlasTreeNode node) {
        if (node == null || isAllocated(node.getId()) || node.getTier() == SkillTier.ROOT) {
            return false;
        }
        for (AtlasTreeNode neighbour : topology.dependents(node.getId())) {
            if (isAllocated(neighbour.getId())) {
                return true;
            }
        }
        return false;
    }

    public List<AtlasTreeNode> pathTo(AtlasTreeNode target) {
        if (target == null || isAllocated(target.getId()) || target.getTier() == SkillTier.ROOT) {
            return List.of();
        }
        TreeSearch search = TreeSearch.from(topology, allocatedIds, node -> !isAllocated(node.getId()) && node.getTier() != SkillTier.ROOT);
        if (!search.reaches(target.getId())) {
            return List.of();
        }
        Deque<AtlasTreeNode> path = new ArrayDeque<>();
        for (String nodeId = target.getId(); !search.isSeed(nodeId); nodeId = search.parent(nodeId)) {
            path.addFirst(topology.node(nodeId));
        }
        return List.copyOf(path);
    }

    public boolean canRefund(AtlasTreeNode node) {
        if (node == null || !isAllocated(node.getId()) || node.getTier() == SkillTier.ROOT) {
            return false;
        }
        Set<String> remaining = new LinkedHashSet<>(allocatedIds);
        remaining.remove(node.getId());
        return connectedToRoots(remaining, node.getId()).containsAll(remaining);
    }

    public AtlasTreeAllocation with(Collection<AtlasTreeNode> added) {
        Set<String> allocated = new LinkedHashSet<>(allocatedIds);
        added.forEach(node -> allocated.add(node.getId()));
        return new AtlasTreeAllocation(topology, allocated);
    }

    public AtlasTreeAllocation without(AtlasTreeNode removed) {
        Set<String> allocated = new LinkedHashSet<>(allocatedIds);
        allocated.remove(removed.getId());
        return new AtlasTreeAllocation(topology, allocated);
    }

    private Set<String> connectedToRoots(Set<String> allocated, String excludedNodeId) {
        TreeSearch search = TreeSearch.from(topology, topology.rootIds(),
                node -> allocated.contains(node.getId()) && !node.getId().equals(excludedNodeId));
        return search.reached();
    }
}
