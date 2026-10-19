package exiledsector.skills.loader;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

final class TreeReachability {

    private TreeReachability() {
    }

    static Set<String> fromRootsAndExits(List<SkillNode> nodes, Predicate<SkillNode> passable) {
        Map<String, SkillNode> byId = new HashMap<>();
        nodes.forEach(node -> byId.put(node.getId(), node));
        Map<String, Set<String>> links = new HashMap<>();
        for (SkillNode node : nodes) {
            for (String other : node.getConnectedNodeIds()) {
                links.computeIfAbsent(node.getId(), key -> new HashSet<>()).add(other);
                links.computeIfAbsent(other, key -> new HashSet<>()).add(node.getId());
            }
        }
        Set<String> reached = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        for (SkillNode node : nodes) {
            SkillTier tier = node.getType().getTier();
            if ((tier == SkillTier.ROOT || tier == SkillTier.WORMHOLE_EXIT) && reached.add(node.getId())) {
                queue.add(node.getId());
            }
        }
        while (!queue.isEmpty()) {
            for (String next : links.getOrDefault(queue.poll(), Set.of())) {
                SkillNode node = byId.get(next);
                if (node != null && passable.test(node) && reached.add(next)) {
                    queue.add(next);
                }
            }
        }
        return reached;
    }
}
