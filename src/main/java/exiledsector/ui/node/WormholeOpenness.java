package exiledsector.ui.node;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;

import java.util.HashMap;
import java.util.Map;

final class WormholeOpenness {

    static final float OPEN_SECONDS = 1f;

    private final Map<String, Float> opennessByNodeId = new HashMap<>();

    void advance(float amount, ShipSkillData skillData) {
        float step = amount / OPEN_SECONDS;
        for (SkillNode node : SkillTree.topology().wormholes()) {
            float targetOpenness = skillData.isAllocated(node.getId()) ? 1f : 0f;
            Float currentOpenness = opennessByNodeId.get(node.getId());
            if (currentOpenness != null && currentOpenness == targetOpenness) {
                continue;
            }
            float nextOpenness = currentOpenness == null ? targetOpenness
                    : currentOpenness < targetOpenness ? Math.min(targetOpenness, currentOpenness + step) : Math.max(targetOpenness, currentOpenness - step);
            opennessByNodeId.put(node.getId(), nextOpenness);
        }
    }

    float of(String nodeId) {
        return opennessByNodeId.getOrDefault(nodeId, 0f);
    }
}
