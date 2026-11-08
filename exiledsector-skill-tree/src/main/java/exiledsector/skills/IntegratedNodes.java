package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.List;
import java.util.Map;

public final class IntegratedNodes {

    private IntegratedNodes() {
    }

    public static boolean canIntegrate(ShipSkillData data, SkillNode node) {
        return data != null && node != null && data.isAllocated(node.getId()) && !data.isIntegrated(node.getId())
                && node.resolveEffectiveType(data).isIntegrable();
    }

    public static boolean isIntegrated(ShipSkillData data, String nodeId, SkillType effectiveType) {
        return data != null && effectiveType != null && effectiveType.isIntegrable() && data.isIntegrated(nodeId);
    }

    public static List<SkillTypeEffect> effectsFor(ShipSkillData data, String nodeId, SkillType effectiveType, HullSize hullSize) {
        return effectiveType.effectsFor(hullSize, isIntegrated(data, nodeId, effectiveType));
    }

    public static boolean holdsIntegrated(ShipSkillData data, SkillType type) {
        if (data == null || type == null || !type.isIntegrable() || data.getIntegratedNodeIds().isEmpty()) {
            return false;
        }
        for (AllocatedNode allocated : AllocatedNode.of(data)) {
            if (allocated.effectiveType().getId().equals(type.getId()) && data.isIntegrated(allocated.node().getId())) {
                return true;
            }
        }
        return false;
    }

    public static int nextCost(ShipSkillData data) {
        return data.getIntegratedNodeIds().size() + 1;
    }

    public static int costOfLast(int integratedCount, int removedCount) {
        int refund = 0;
        for (int cost = integratedCount; cost > integratedCount - removedCount && cost > 0; cost--) {
            refund += cost;
        }
        return refund;
    }

    static int forgetInapplicable(ShipSkillData data, Map<String, SkillNode> declaredNodesById) {
        int integratedCount = data.getIntegratedNodeIds().size();
        int removedCount = 0;
        for (String nodeId : List.copyOf(data.getIntegratedNodeIds())) {
            SkillNode node = declaredNodesById.get(nodeId);
            boolean applicable = node != null && node.getType() != null && (node.getType().isIntegrable() || node.getType().isChoice());
            if (!applicable && data.forgetIntegration(nodeId)) {
                removedCount++;
            }
        }
        return costOfLast(integratedCount, removedCount);
    }
}
