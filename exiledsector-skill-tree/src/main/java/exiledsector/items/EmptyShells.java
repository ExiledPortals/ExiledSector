package exiledsector.items;

import exiledsector.skills.skilleffect.special.SpecialSkillEffect;
import exiledsector.modifiers.ModifierLine;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class EmptyShells {

    private EmptyShells() {
    }

    public static boolean isEmptyShell(Item item) {
        return item instanceof Subroutine && emptyShells(item.definition());
    }

    static boolean emptyShells(ItemDefinition definition) {
        if (definition == null) {
            return false;
        }
        for (PoolEntry entry : definition.prefixes()) {
            for (ModifierLine line : entry.modifier().lines()) {
                if (SpecialSkillEffect.EVERY_AFFINITY_FLAT_PER_EMPTY_SHELL_NODE.name().equals(line.effectName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<String> radiusNodeIds(Item item, String socketId) {
        SubroutineRadius.Footprint footprint = isEmptyShell(item) ? SubroutineRadius.footprint(item, socketId) : null;
        if (footprint == null) {
            return List.of();
        }
        List<String> nodeIds = new ArrayList<>();
        for (String nodeId : new TreeSet<>(SkillTree.radii().nodeIdsWithin(footprint.centreNodeId(), footprint.size()))) {
            SkillNode node = SkillTree.get(nodeId);
            if (node != null && node.getType().getTier() == SkillTier.SMALL) {
                nodeIds.add(nodeId);
            }
        }
        return nodeIds;
    }

    public static Set<String> emptyShellNodeIds(ShipSkillData data) {
        Set<String> emptyShell = new HashSet<>();
        for (Map.Entry<String, String> socketed : data.getSocketedItems().entrySet()) {
            if (data.isAllocated(socketed.getKey())) {
                radiusNodeIds(ItemStore.lookup(socketed.getValue()), socketed.getKey()).stream().filter(data::isAllocated).forEach(emptyShell::add);
            }
        }
        return emptyShell;
    }

    public static int emptyShellCount(ShipSkillData data, String socketId) {
        return radiusNodeIds(ItemStore.lookup(data.getSocketedItem(socketId)), socketId).size();
    }
}
