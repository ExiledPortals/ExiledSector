package exiledsector.ui.node;

import exiledsector.skills.ExoshipNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

final class ConnectorFlow {

    static final float WAVE_INTERVAL_SECONDS = 4f;
    static final float SPEED_PER_SECOND = 120f;
    private static final float CLOCK_WRAP_SECONDS = WAVE_INTERVAL_SECONDS * 1000f;

    interface Graph {

        List<String> neighbours(String nodeId);

        float length(String fromId, String toId);
    }

    private record Reached(String nodeId, float distance) {
    }

    private final Map<String, Float> distances = new HashMap<>();
    private NodeAllocator.Snapshot source;
    private Set<String> sourcePlannedOnly = Set.of();
    private float clockSeconds;

    void advance(float amount) {
        clockSeconds = (clockSeconds + amount) % CLOCK_WRAP_SECONDS;
    }

    void refresh(NodeAllocator.Snapshot allocation, Set<String> plannedOnlyNodeIds) {
        if (allocation == source && plannedOnlyNodeIds.equals(sourcePlannedOnly)) {
            return;
        }
        source = allocation;
        sourcePlannedOnly = Set.copyOf(plannedOnlyNodeIds);
        String rootId = allocation.satisfiedRootId();
        if (rootId == null) {
            distances.clear();
            return;
        }
        recompute(rootId, new SatisfiedTree(allocation.skillData(), rootId, sourcePlannedOnly));
    }

    void recompute(String rootId, Graph graph) {
        distances.clear();
        PriorityQueue<Reached> frontier = new PriorityQueue<>((first, second) -> Float.compare(first.distance(), second.distance()));
        distances.put(rootId, 0f);
        frontier.add(new Reached(rootId, 0f));
        while (!frontier.isEmpty()) {
            Reached reached = frontier.poll();
            if (reached.distance() > distances.get(reached.nodeId())) {
                continue;
            }
            for (String neighbourId : graph.neighbours(reached.nodeId())) {
                float distance = reached.distance() + graph.length(reached.nodeId(), neighbourId);
                Float known = distances.get(neighbourId);
                if (known == null || distance < known) {
                    distances.put(neighbourId, distance);
                    frontier.add(new Reached(neighbourId, distance));
                }
            }
        }
    }

    float distance(String nodeId) {
        Float distance = distances.get(nodeId);
        return distance == null ? Float.NaN : distance;
    }

    int bubbles(float nearDistance, float length, float[] out) {
        return bubbles(clockSeconds, nearDistance, length, out);
    }

    static int bubbles(float timeSeconds, float nearDistance, float length, float[] out) {
        if (length <= 0f) {
            return 0;
        }
        float travelled = timeSeconds * SPEED_PER_SECOND;
        float spacing = WAVE_INTERVAL_SECONDS * SPEED_PER_SECOND;
        int count = 0;
        for (long wave = (long) Math.ceil((travelled - nearDistance - length) / spacing); count < out.length; wave++) {
            float along = travelled - wave * spacing - nearDistance;
            if (along < 0f) {
                break;
            }
            if (along <= length) {
                out[count++] = along / length;
            }
        }
        return count;
    }

    private static final class SatisfiedTree implements Graph {
        private final ShipSkillData skillData;
        private final String rootId;
        private final Set<String> plannedOnlyNodeIds;
        private final SkillTreeTopology topology = SkillTree.topology();
        private final Map<String, String> wormholePartners = new HashMap<>();

        SatisfiedTree(ShipSkillData skillData, String rootId, Set<String> plannedOnlyNodeIds) {
            this.skillData = skillData;
            this.rootId = rootId;
            this.plannedOnlyNodeIds = plannedOnlyNodeIds;
            for (SkillTreeTopology.WormholePair pair : ShipTreeLinks.wormholePairs(skillData, topology)) {
                wormholePartners.put(pair.first().getId(), pair.second().getId());
                wormholePartners.put(pair.second().getId(), pair.first().getId());
            }
        }

        @Override
        public List<String> neighbours(String nodeId) {
            SkillNode node = SkillTree.get(nodeId);
            List<String> neighbourIds = new ArrayList<>();
            if (node == null) {
                return neighbourIds;
            }
            boolean carried = ExoshipNodes.isCarried(node);
            for (SkillNode neighbour : topology.drawnNeighbours(nodeId)) {
                if (ExoshipNodes.isCarried(neighbour) == carried && live(neighbour.getId())) {
                    neighbourIds.add(neighbour.getId());
                }
            }
            String partnerId = wormholePartners.get(nodeId);
            if (partnerId != null && live(partnerId)) {
                neighbourIds.add(partnerId);
            }
            return neighbourIds;
        }

        private boolean live(String nodeId) {
            return skillData.isSatisfied(nodeId, rootId) && !plannedOnlyNodeIds.contains(nodeId);
        }

        @Override
        public float length(String fromId, String toId) {
            if (toId.equals(wormholePartners.get(fromId))) {
                return 0f;
            }
            SkillNode from = SkillTree.get(fromId);
            SkillNode to = SkillTree.get(toId);
            return (float) Math.hypot(to.getOffsetX() - from.getOffsetX(), to.getOffsetY() - from.getOffsetY());
        }
    }
}
