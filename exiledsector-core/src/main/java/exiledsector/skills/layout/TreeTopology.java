package exiledsector.skills.layout;

import exiledsector.skills.SkillTier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TreeTopology<N extends TreeNode> {

    public record Connector<N extends TreeNode>(N from, N to, ConnectorCurve curve, float minX, float minY, float maxX, float maxY) {
    }

    public record WormholePair<N extends TreeNode>(N first, N second) {
    }

    public interface ConnectorLinks {

        ConnectorLinks NONE = new ConnectorLinks() {
            @Override
            public ConnectorCurve curve(String firstNodeId, String secondNodeId) {
                return null;
            }

            @Override
            public boolean isVisible(String firstNodeId, String secondNodeId) {
                return true;
            }
        };

        ConnectorCurve curve(String firstNodeId, String secondNodeId);

        boolean isVisible(String firstNodeId, String secondNodeId);

        static ConnectorLinks of(Map<String, ConnectorCurve> curvesByKey, Set<String> hiddenConnectorKeys) {
            return new ConnectorLinks() {
                @Override
                public ConnectorCurve curve(String firstNodeId, String secondNodeId) {
                    return curvesByKey.get(ConnectorCurve.key(firstNodeId, secondNodeId));
                }

                @Override
                public boolean isVisible(String firstNodeId, String secondNodeId) {
                    return !hiddenConnectorKeys.contains(ConnectorCurve.key(firstNodeId, secondNodeId));
                }
            };
        }
    }

    private record Drawing<N extends TreeNode>(List<Connector<N>> connectors, Map<String, List<N>> neighbours) {
    }

    private final ConnectorLinks links;
    private final Map<String, N> byId;
    private final List<N> roots;
    private final List<N> nonRoots;
    private final Set<String> rootIds;
    private final List<N> sortedById;
    private final Map<String, List<N>> dependents;
    private final List<N> wormholes;
    private final List<WormholePair<N>> wormholePairs;
    private volatile Drawing<N> drawing;

    protected TreeTopology(Collection<N> nodes, ConnectorLinks links) {
        this.links = links;
        Map<String, N> nodesById = new LinkedHashMap<>();
        for (N node : nodes) {
            nodesById.put(node.getId(), node);
        }
        byId = nodesById;

        List<N> rootNodes = new ArrayList<>();
        List<N> otherNodes = new ArrayList<>();
        List<N> wormholeNodes = new ArrayList<>();
        for (N node : byId.values()) {
            SkillTier tier = node.getTier();
            if (tier == SkillTier.ROOT) {
                rootNodes.add(node);
            } else {
                otherNodes.add(node);
            }
            if (tier.isWormhole()) {
                wormholeNodes.add(node);
            }
        }
        roots = List.copyOf(rootNodes);
        nonRoots = List.copyOf(otherNodes);
        wormholes = List.copyOf(wormholeNodes);
        rootIds = Set.copyOf(rootIdsOf(roots));

        List<N> sorted = new ArrayList<>(byId.values());
        sorted.sort(Comparator.comparing(TreeNode::getId));
        sortedById = List.copyOf(sorted);
        dependents = frozen(dependentsOf(sortedById));

        List<WormholePair<N>> pairs = new ArrayList<>();
        for (N node : byId.values()) {
            N paired = node.getPairedNodeId() == null ? null : byId.get(node.getPairedNodeId());
            if (node.getTier() == SkillTier.WORMHOLE && paired != null && node.getId().compareTo(paired.getId()) < 0) {
                pairs.add(new WormholePair<>(node, paired));
            }
        }
        wormholePairs = List.copyOf(pairs);
    }

    public static <N extends TreeNode> TreeTopology<N> of(Collection<N> nodes, ConnectorLinks links) {
        return new TreeTopology<>(nodes, links);
    }

    private Drawing<N> drawing() {
        Drawing<N> built = drawing;
        if (built == null) {
            List<Connector<N>> drawn = new ArrayList<>();
            Map<String, Set<String>> neighbourIds = new HashMap<>();
            for (N node : byId.values()) {
                addDrawnEdges(node, drawn, neighbourIds);
            }
            built = new Drawing<>(List.copyOf(drawn), frozen(neighboursOf(byId.values(), neighbourIds)));
            drawing = built;
        }
        return built;
    }

    private static <N extends TreeNode> Map<String, List<N>> dependentsOf(List<N> sortedNodes) {
        Map<String, List<N>> dependentsById = new HashMap<>();
        for (N node : sortedNodes) {
            for (String connectedId : node.getConnectedNodeIds()) {
                dependentsById.computeIfAbsent(connectedId, key -> new ArrayList<>()).add(node);
            }
        }
        return dependentsById;
    }

    private void addDrawnEdges(N node, List<Connector<N>> drawn, Map<String, Set<String>> neighbourIds) {
        for (String connectedId : node.getConnectedNodeIds()) {
            N other = byId.get(connectedId);
            if (other != null && drawsListedEdge(node, other)) {
                drawn.add(connector(node, other, links.curve(node.getId(), other.getId())));
                neighbourIds.computeIfAbsent(node.getId(), key -> new LinkedHashSet<>()).add(other.getId());
                neighbourIds.computeIfAbsent(other.getId(), key -> new LinkedHashSet<>()).add(node.getId());
            }
        }
    }

    private static <N extends TreeNode> Map<String, List<N>> neighboursOf(Collection<N> nodes, Map<String, Set<String>> neighbourIds) {
        Map<String, List<N>> neighbours = new HashMap<>();
        for (N node : nodes) {
            for (String neighbourId : neighbourIds.getOrDefault(node.getId(), Set.of())) {
                neighbours.computeIfAbsent(neighbourId, key -> new ArrayList<>()).add(node);
            }
        }
        return neighbours;
    }

    public static Set<String> rootIdsOf(Collection<? extends TreeNode> nodes) {
        Set<String> ids = new LinkedHashSet<>();
        for (TreeNode node : nodes) {
            if (node.getTier() == SkillTier.ROOT) {
                ids.add(node.getId());
            }
        }
        return ids;
    }

    public boolean drawsEdge(N from, N to) {
        return from.getConnectedNodeIds().contains(to.getId()) && drawsListedEdge(from, to);
    }

    private boolean drawsListedEdge(N from, N to) {
        return from.getTier() != SkillTier.ROOT
                && (to.getTier() == SkillTier.ROOT || from.getId().compareTo(to.getId()) < 0)
                && links.isVisible(from.getId(), to.getId());
    }

    public static <N extends TreeNode> Connector<N> connector(N from, N to, ConnectorCurve curve) {
        float bendX = from.getX();
        float bendY = from.getY();
        if (curve != null) {
            bendX = 2f * curve.getControlOffsetX() - (from.getX() + to.getX()) / 2f;
            bendY = 2f * curve.getControlOffsetY() - (from.getY() + to.getY()) / 2f;
        }
        return new Connector<>(from, to, curve,
                Math.min(Math.min(from.getX(), to.getX()), bendX),
                Math.min(Math.min(from.getY(), to.getY()), bendY),
                Math.max(Math.max(from.getX(), to.getX()), bendX),
                Math.max(Math.max(from.getY(), to.getY()), bendY));
    }

    private static <N extends TreeNode> Map<String, List<N>> frozen(Map<String, List<N>> lists) {
        Map<String, List<N>> copy = new HashMap<>();
        lists.forEach((id, nodes) -> copy.put(id, List.copyOf(nodes)));
        return copy;
    }

    public N node(String nodeId) {
        return byId.get(nodeId);
    }

    public Collection<N> nodes() {
        return byId.values();
    }

    public List<N> roots() {
        return roots;
    }

    public List<N> nonRoots() {
        return nonRoots;
    }

    public Set<String> rootIds() {
        return rootIds;
    }

    public List<N> sortedById() {
        return sortedById;
    }

    public List<N> dependents(String nodeId) {
        return dependents.getOrDefault(nodeId, List.of());
    }

    public List<Connector<N>> connectors() {
        return drawing().connectors();
    }

    public List<N> drawnNeighbours(String nodeId) {
        return drawing().neighbours().getOrDefault(nodeId, List.of());
    }

    public List<N> wormholes() {
        return wormholes;
    }

    public List<WormholePair<N>> wormholePairs() {
        return wormholePairs;
    }
}
