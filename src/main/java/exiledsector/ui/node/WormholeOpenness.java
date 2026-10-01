package exiledsector.ui.node;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;

import java.util.HashMap;
import java.util.Map;

final class WormholeOpenness {

    static final float OPEN_SECONDS = 1f;

    private final Map<String, Float> openness = new HashMap<>();

    void advance(float amount, ShipSkillData data) {
        float step = amount / OPEN_SECONDS;
        for (SkillNode node : SkillTree.topology().wormholes()) {
            float target = data.isAllocated(node.getId()) ? 1f : 0f;
            Float current = openness.get(node.getId());
            if (current != null && current == target) {
                continue;
            }
            float next = current == null ? target
                    : current < target ? Math.min(target, current + step) : Math.max(target, current - step);
            openness.put(node.getId(), next);
        }
    }

    float of(String nodeId) {
        return openness.getOrDefault(nodeId, 0f);
    }
}
