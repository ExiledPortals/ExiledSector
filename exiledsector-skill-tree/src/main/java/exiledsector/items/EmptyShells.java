package exiledsector.items;

import exiledsector.skills.skilleffect.special.SpecialSkillEffect;
import exiledsector.modifiers.ModifierLine;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
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
        return item instanceof Subroutine && hollows(item.definition());
    }

    static boolean hollows(ItemDefinition definition) {
        if (definition == null) {
            return false;
        }
        for (PoolEntry entry : definition.prefixes()) {
            for (ModifierLine line : entry.modifier().lines()) {
                if (SpecialSkillEffect.EVERY_AFFINITY_FLAT_PER_HOLLOW_NODE.name().equals(line.effectName())) {
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
            if (node != null && hollowable(node)) {
                nodeIds.add(nodeId);
            }
        }
        return nodeIds;
    }

    private static boolean hollowable(SkillNode node) {
        SkillTier tier = node.getType().getTier();
        return tier == SkillTier.SMALL || tier == SkillTier.NOTABLE || tier == SkillTier.KEYSTONE;
    }

    public static boolean holdsEmptyShell(ShipSkillData data, String socketId) {
        return isEmptyShell(ItemStore.lookup(data.getSocketedItem(socketId)));
    }

    public static Set<String> hollowNodeIds(ShipSkillData data) {
        Set<String> hollow = new HashSet<>();
        for (Map.Entry<String, List<String>> grantLink : ShipTreeLinks.grantedNodesBySocket(data).entrySet()) {
            if (holdsEmptyShell(data, grantLink.getKey())) {
                grantLink.getValue().stream().filter(data::isAllocated).forEach(hollow::add);
            }
        }
        return hollow;
    }

    public static int hollowCount(ShipSkillData data, String socketId) {
        return (int) ShipTreeLinks.grantedNodesBySocket(data).getOrDefault(socketId, List.of()).stream().filter(data::isAllocated).count();
    }
}
