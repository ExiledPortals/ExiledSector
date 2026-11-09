package exiledsector.charts.tree;

import exiledsector.charts.atlas.AtlasState;

import java.util.List;

public final class AtlasTree {

    private AtlasTree() {
    }

    public static AtlasTreeAllocation allocation(AtlasState state) {
        return AtlasTreeAllocation.of(AtlasTreeData.get().topology(), state.atlasTreeNodeIds());
    }

    public static int unspentPoints(AtlasState state, AtlasTreeAllocation allocation) {
        return Math.max(0, state.atlasPoints() - allocation.spentPoints());
    }

    public static boolean allocate(AtlasState state, AtlasTreeNode node) {
        AtlasTreeAllocation allocation = allocation(state);
        if (!allocation.canAllocate(node) || unspentPoints(state, allocation) < 1) {
            return false;
        }
        state.setAtlasTreeNodeIds(allocation.with(List.of(node)).storedNodeIds());
        return true;
    }

    public static boolean allocatePath(AtlasState state, AtlasTreeNode target) {
        AtlasTreeAllocation allocation = allocation(state);
        List<AtlasTreeNode> path = allocation.pathTo(target);
        if (path.isEmpty() || unspentPoints(state, allocation) < path.size()) {
            return false;
        }
        state.setAtlasTreeNodeIds(allocation.with(path).storedNodeIds());
        return true;
    }

    public static boolean refund(AtlasState state, AtlasTreeNode node) {
        AtlasTreeAllocation allocation = allocation(state);
        if (!allocation.canRefund(node)) {
            return false;
        }
        state.setAtlasTreeNodeIds(allocation.without(node).storedNodeIds());
        return true;
    }
}
