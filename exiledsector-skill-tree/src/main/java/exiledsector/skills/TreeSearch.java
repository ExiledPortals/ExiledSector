package exiledsector.skills;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TreeSearch {

    private final Map<String, Integer> distances = new LinkedHashMap<>();
    private final Map<String, String> parents = new HashMap<>();
    private final Set<String> seeds = new HashSet<>();
    private final Set<String> jumps = new HashSet<>();

    private TreeSearch() {
    }

    public static TreeSearch from(SkillTreeTopology topology, Collection<String> seeds, Predicate<SkillNode> enterable) {
        return from(topology, seeds, enterable, node -> null);
    }

    public static TreeSearch from(SkillTreeTopology topology, Collection<String> seeds, Predicate<SkillNode> enterable,
                                  Function<SkillNode, String> jumpTarget) {
        TreeSearch search = new TreeSearch();
        Deque<String> queue = new ArrayDeque<>();
        for (String seed : seeds) {
            if (search.distances.putIfAbsent(seed, 0) == null) {
                search.seeds.add(seed);
                queue.add(seed);
            }
        }
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int next = search.distances.get(current) + 1;
            for (SkillNode candidate : topology.dependents(current)) {
                String id = candidate.getId();
                if (search.distances.containsKey(id) || !enterable.test(candidate)) {
                    continue;
                }
                search.distances.put(id, next);
                search.parents.put(id, current);
                queue.add(id);
                String jump = jumpTarget.apply(candidate);
                if (jump != null && !search.distances.containsKey(jump)) {
                    search.distances.put(jump, next);
                    search.parents.put(jump, id);
                    search.jumps.add(jump);
                    queue.add(jump);
                }
            }
        }
        return search;
    }

    public boolean reaches(String nodeId) {
        return distances.containsKey(nodeId);
    }

    public int distance(String nodeId) {
        return distances.getOrDefault(nodeId, Integer.MAX_VALUE);
    }

    public String parent(String nodeId) {
        return parents.get(nodeId);
    }

    public boolean isSeed(String nodeId) {
        return seeds.contains(nodeId);
    }

    public boolean isJump(String nodeId) {
        return jumps.contains(nodeId);
    }

    public Set<String> reached() {
        return Collections.unmodifiableSet(distances.keySet());
    }
}
