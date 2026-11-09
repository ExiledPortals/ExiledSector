package exiledsector.ui.node;

import exiledsector.skills.layout.TreeNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class WormholeOpenness {

    public static final float OPEN_SECONDS = 1f;

    private final Map<String, Float> opennessByNodeId = new HashMap<>();

    public void advance(float amount, Collection<? extends TreeNode> wormholes, Predicate<String> allocated) {
        float step = amount / OPEN_SECONDS;
        for (TreeNode node : wormholes) {
            float targetOpenness = allocated.test(node.getId()) ? 1f : 0f;
            Float currentOpenness = opennessByNodeId.get(node.getId());
            if (currentOpenness != null && currentOpenness == targetOpenness) {
                continue;
            }
            float nextOpenness = currentOpenness == null ? targetOpenness
                    : currentOpenness < targetOpenness ? Math.min(targetOpenness, currentOpenness + step) : Math.max(targetOpenness, currentOpenness - step);
            opennessByNodeId.put(node.getId(), nextOpenness);
        }
    }

    public float of(String nodeId) {
        return opennessByNodeId.getOrDefault(nodeId, 0f);
    }
}
