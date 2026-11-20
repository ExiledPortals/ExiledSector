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

    public static int spentPoints(AtlasState state, int loadout) {
        return AtlasTreeAllocation.of(AtlasTreeData.get().topology(), state.atlasTreeNodeIds(loadout)).spentPoints();
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
        return atlasEffects(state, state.activeAtlasLoadout());
    }

    public static AtlasEffects atlasEffects(AtlasState state, int loadout) {
        List<AtlasEffects.Line> lines = new ArrayList<>();
        allocatedNodes(state.atlasTreeNodeIds(loadout)).forEach(node -> lines.addAll(node.type().atlasEffects()));
        return AtlasEffects.of(lines);
    }

    public static List<AtlasTreeEffectGroups.Group> effectGroups(AtlasState state) {
        return AtlasTreeEffectGroups.of(allocatedNodes(state));
    }

    private static List<AtlasTreeType> allocatedTypes(AtlasState state) {
        return allocatedNodes(state).stream().map(AtlasTreeNode::type).toList();
    }

    private static List<AtlasTreeNode> allocatedNodes(AtlasState state) {
        return allocatedNodes(state.atlasTreeNodeIds());
    }

    private static List<AtlasTreeNode> allocatedNodes(Set<String> storedNodeIds) {
        AtlasTreeData data = AtlasTreeData.get();
        Set<String> allocatedIds = new LinkedHashSet<>(data.topology().rootIds());
        allocatedIds.addAll(AtlasTreeAllocation.of(data.topology(), storedNodeIds).storedNodeIds());
        List<AtlasTreeNode> nodes = new ArrayList<>();
        for (String nodeId : allocatedIds) {
            AtlasTreeNode node = data.node(nodeId);
            if (node != null && node.type() != null) {
                nodes.add(node);
            }
        }
        return nodes;
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
