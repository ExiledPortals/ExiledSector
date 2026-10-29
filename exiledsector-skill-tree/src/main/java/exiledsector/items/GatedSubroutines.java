package exiledsector.items;

import exiledsector.skills.CombatGate;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.modifiers.ModifierLine;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public final class GatedSubroutines {

    static final String GATING_EFFECT_PREFIX = "GATES_RADIUS_";

    public record Gating(CombatGate gate, float strength, String socketNodeId) {
    }

    private GatedSubroutines() {
    }

    public static Map<String, Gating> gatedNodeIds(ShipSkillData data) {
        Map<String, Gating> gated = new HashMap<>();
        for (Map.Entry<String, String> socketed : new TreeMap<>(data.getSocketedItems()).entrySet()) {
            if (!data.isAllocated(socketed.getKey())) {
                continue;
            }
            Item item = ItemStore.lookup(socketed.getValue());
            Gating gating = gatingOf(item, socketed.getKey());
            SubroutineRadius.Footprint footprint = gating == null ? null : SubroutineRadius.footprint(item, socketed.getKey());
            if (footprint != null) {
                for (String nodeId : SkillTree.radii().nodeIdsWithin(footprint.centreNodeId(), footprint.size())) {
                    SkillNode node = SkillTree.get(nodeId);
                    if (node != null && gateable(node)) {
                        gated.putIfAbsent(nodeId, gating);
                    }
                }
            }
        }
        return gated;
    }

    private static boolean gateable(SkillNode node) {
        SkillTier tier = node.getType().getTier();
        return tier == SkillTier.SMALL || tier == SkillTier.NOTABLE || tier == SkillTier.KEYSTONE;
    }

    static Gating gatingOf(Item item, String socketNodeId) {
        if (!(item instanceof Subroutine)) {
            return null;
        }
        for (SkillTypeEffect effect : item.skillEffects()) {
            CombatGate gate = effect.effect().radiusGate();
            if (gate != null) {
                return new Gating(gate, effect.magnitude(), socketNodeId);
            }
        }
        return null;
    }

    static boolean gates(ItemDefinition definition) {
        for (PoolEntry entry : definition.prefixes()) {
            for (ModifierLine line : entry.modifier().lines()) {
                if (line.effectName().startsWith(GATING_EFFECT_PREFIX)) {
                    return true;
                }
            }
        }
        return false;
    }
}
