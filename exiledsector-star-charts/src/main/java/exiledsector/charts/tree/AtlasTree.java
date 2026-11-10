package exiledsector.charts.tree;

import exiledsector.charts.atlas.AtlasEffects;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.effect.ChartModifier;
import exiledsector.charts.generation.ChartGenContext.AppliedEffect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AtlasTree {

    private AtlasTree() {
    }

    public static AtlasTreeAllocation allocation(AtlasState state) {
        return AtlasTreeAllocation.of(AtlasTreeData.get().topology(), state.atlasTreeNodeIds());
    }

    public static List<AppliedEffect> chartLines(AtlasState state) {
        Map<ChartModifier, Float> totals = new LinkedHashMap<>();
        for (AtlasTreeType type : allocatedTypes(state)) {
            type.effects().forEach(line -> totals.merge(line.effect(), line.value(), Float::sum));
        }
        List<AppliedEffect> lines = new ArrayList<>();
        totals.forEach((effect, value) -> lines.add(new AppliedEffect(effect, value)));
        return lines;
    }

    public static AtlasEffects atlasEffects(AtlasState state) {
        List<AtlasEffects.Line> lines = new ArrayList<>();
        allocatedTypes(state).forEach(type -> lines.addAll(type.atlasEffects()));
        return AtlasEffects.of(lines);
    }

    private static List<AtlasTreeType> allocatedTypes(AtlasState state) {
        AtlasTreeData data = AtlasTreeData.get();
        Set<String> allocatedIds = new LinkedHashSet<>(data.topology().rootIds());
        allocatedIds.addAll(allocation(state).storedNodeIds());
        List<AtlasTreeType> types = new ArrayList<>();
        for (String nodeId : allocatedIds) {
            AtlasTreeNode node = data.node(nodeId);
            if (node != null && node.type() != null) {
                types.add(node.type());
            }
        }
        return types;
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
