package exiledsector.skills;

import exiledsector.skills.layout.ConnectorCurve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkillTreeTopology {

    public record Connector(SkillNode from, SkillNode to, ConnectorCurve curve,
                            float minX, float minY, float maxX, float maxY) {
    }

    public record WormholePair(SkillNode first, SkillNode second) {
    }

    private final Map<String, SkillNode> byId;
    private final List<SkillNode> roots;
    private final List<SkillNode> nonRoots;
    private final Set<String> rootIds;
    private final List<SkillNode> sortedById;
    private final Map<String, List<SkillNode>> dependents;
    private final List<Connector> connectors;
    private final Map<String, List<SkillNode>> drawnNeighbours;
    private final List<SkillNode> wormholes;
    private final List<WormholePair> wormholePairs;

    private SkillTreeTopology(Collection<SkillNode> nodes) {
        Map<String, SkillNode> nodesById = new LinkedHashMap<>();
        for (SkillNode node : nodes) {
            nodesById.put(node.getId(), node);
        }
        byId = nodesById;

        List<SkillNode> rootNodes = new ArrayList<>();
        List<SkillNode> otherNodes = new ArrayList<>();
        List<SkillNode> wormholeNodes = new ArrayList<>();
        for (SkillNode node : byId.values()) {
            SkillTier tier = node.getType().getTier();
            if (tier == SkillTier.ROOT) {
                rootNodes.add(node);
            } else {
                otherNodes.add(node);
            }
            if (tier == SkillTier.WORMHOLE) {
                wormholeNodes.add(node);
            }
        }
        roots = List.copyOf(rootNodes);
        nonRoots = List.copyOf(otherNodes);
        wormholes = List.copyOf(wormholeNodes);
        rootIds = Set.copyOf(rootIdsOf(roots));

        List<SkillNode> sorted = new ArrayList<>(byId.values());
        sorted.sort(Comparator.comparing(SkillNode::getId));
        sortedById = List.copyOf(sorted);
        Map<String, List<SkillNode>> dependentsById = new HashMap<>();
        for (SkillNode node : sortedById) {
            for (String connectedId : node.getConnectedNodeIds()) {
                dependentsById.computeIfAbsent(connectedId, key -> new ArrayList<>()).add(node);
            }
        }
        dependents = frozen(dependentsById);

        List<Connector> drawn = new ArrayList<>();
        Map<String, Set<String>> neighbourIds = new HashMap<>();
        List<WormholePair> pairs = new ArrayList<>();
        for (SkillNode node : byId.values()) {
            for (String connectedId : node.getConnectedNodeIds()) {
                SkillNode other = byId.get(connectedId);
                if (other != null && drawsListedEdge(node, other)) {
                    drawn.add(connector(node, other, SkillTree.getCurve(node.getId(), other.getId())));
                    neighbourIds.computeIfAbsent(node.getId(), key -> new LinkedHashSet<>()).add(other.getId());
                    neighbourIds.computeIfAbsent(other.getId(), key -> new LinkedHashSet<>()).add(node.getId());
                }
            }
            SkillNode paired = node.getPairedNodeId() == null ? null : byId.get(node.getPairedNodeId());
            if (node.getType().getTier() == SkillTier.WORMHOLE && paired != null && node.getId().compareTo(paired.getId()) < 0) {
                pairs.add(new WormholePair(node, paired));
            }
        }
        connectors = List.copyOf(drawn);
        wormholePairs = List.copyOf(pairs);

        Map<String, List<SkillNode>> neighbours = new HashMap<>();
        for (SkillNode node : byId.values()) {
            for (String neighbourId : neighbourIds.getOrDefault(node.getId(), Set.of())) {
                neighbours.computeIfAbsent(neighbourId, key -> new ArrayList<>()).add(node);
            }
        }
        drawnNeighbours = frozen(neighbours);
    }

    public static SkillTreeTopology of(Collection<SkillNode> nodes) {
        return new SkillTreeTopology(nodes);
    }

    public static Set<String> rootIdsOf(Collection<SkillNode> nodes) {
        Set<String> ids = new LinkedHashSet<>();
        for (SkillNode node : nodes) {
            if (node.getType().getTier() == SkillTier.ROOT) {
                ids.add(node.getId());
            }
        }
        return ids;
    }

    public static boolean drawsEdge(SkillNode from, SkillNode to) {
        return from.getConnectedNodeIds().contains(to.getId()) && drawsListedEdge(from, to);
    }

    private static boolean drawsListedEdge(SkillNode from, SkillNode to) {
        return from.getType().getTier() != SkillTier.ROOT
                && (to.getType().getTier() == SkillTier.ROOT || from.getId().compareTo(to.getId()) < 0)
                && SkillTree.isConnectorVisible(from.getId(), to.getId());
    }

    static Connector connector(SkillNode from, SkillNode to, ConnectorCurve curve) {
        float bendX = from.getOffsetX();
        float bendY = from.getOffsetY();
        if (curve != null) {
            bendX = 2f * curve.getControlOffsetX() - (from.getOffsetX() + to.getOffsetX()) / 2f;
            bendY = 2f * curve.getControlOffsetY() - (from.getOffsetY() + to.getOffsetY()) / 2f;
        }
        return new Connector(from, to, curve,
                Math.min(Math.min(from.getOffsetX(), to.getOffsetX()), bendX),
                Math.min(Math.min(from.getOffsetY(), to.getOffsetY()), bendY),
                Math.max(Math.max(from.getOffsetX(), to.getOffsetX()), bendX),
                Math.max(Math.max(from.getOffsetY(), to.getOffsetY()), bendY));
    }

    private static Map<String, List<SkillNode>> frozen(Map<String, List<SkillNode>> lists) {
        Map<String, List<SkillNode>> copy = new HashMap<>();
        lists.forEach((id, nodes) -> copy.put(id, List.copyOf(nodes)));
        return copy;
    }

    public SkillNode node(String nodeId) {
        return byId.get(nodeId);
    }

    public List<SkillNode> roots() {
        return roots;
    }

    public List<SkillNode> nonRoots() {
        return nonRoots;
    }

    public Set<String> rootIds() {
        return rootIds;
    }

    public List<SkillNode> sortedById() {
        return sortedById;
    }

    public List<SkillNode> dependents(String nodeId) {
        return dependents.getOrDefault(nodeId, List.of());
    }

    public List<Connector> connectors() {
        return connectors;
    }

    public List<SkillNode> drawnNeighbours(String nodeId) {
        return drawnNeighbours.getOrDefault(nodeId, List.of());
    }

    public List<SkillNode> wormholes() {
        return wormholes;
    }

    public List<WormholePair> wormholePairs() {
        return wormholePairs;
    }
}
