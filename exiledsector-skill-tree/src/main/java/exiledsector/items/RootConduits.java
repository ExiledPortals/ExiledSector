package exiledsector.items;

import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;

public final class RootConduits {

    public static final int NODES_PER_STEP = 5;
    public static final float AFFINITY_PER_STEP = 5f;
    static final float DROP_CHANCE_PER_100_DEPLOYMENT_POINTS = 0.01f;
    static final String DROP_SOURCE_GROUP = "root_conduit";

    public record Conduit(String socketId, SkillEffect affinity, List<String> path) {

        public int nodesBetween() {
            return Math.max(0, path.size() - 2);
        }

        public float affinityGranted() {
            return AFFINITY_PER_STEP * (nodesBetween() / NODES_PER_STEP);
        }

        public Set<String> connectorKeys() {
            Set<String> keys = new HashSet<>();
            for (int index = 1; index < path.size(); index++) {
                keys.add(SkillTree.curveKey(path.get(index - 1), path.get(index)));
            }
            return keys;
        }
    }

    private record CachedPath(String socketId, String rootId, SkillTreeTopology topology, int revision, List<String> path) {
    }

    private static final Map<ShipSkillData, CachedPath> PATHS = new WeakHashMap<>();

    private RootConduits() {
    }

    public static void registerDrops() {
        Map<String, FactionFleetDrops.Source> sources = new LinkedHashMap<>();
        for (ItemDefinition definition : SubroutineKind.ROOT_CONDUIT.definitions()) {
            sources.put(definition.id(), FactionFleetDrops.Source.anyFaction(DROP_CHANCE_PER_100_DEPLOYMENT_POINTS));
        }
        FactionFleetDrops.register(DROP_SOURCE_GROUP, sources);
    }

    public static Conduit of(ShipSkillData data) {
        if (data == null || data.isNpcBuild()) {
            return null;
        }
        for (Map.Entry<String, String> socketed : new TreeMap<>(data.getSocketedItems()).entrySet()) {
            if (data.isAllocated(socketed.getKey()) && ItemStore.lookup(socketed.getValue()) instanceof Subroutine subroutine
                    && subroutine.subroutineKind() == SubroutineKind.ROOT_CONDUIT) {
                return new Conduit(socketed.getKey(), subroutine.conduitAffinity(), cachedPath(data, socketed.getKey()));
            }
        }
        return null;
    }

    public static SkillTypeEffect effectAt(ShipSkillData data, String socketId) {
        Conduit conduit = of(data);
        return conduit == null || !conduit.socketId().equals(socketId) ? null : new SkillTypeEffect(conduit.affinity(), conduit.affinityGranted());
    }

    private static List<String> cachedPath(ShipSkillData data, String socketId) {
        String rootId = ShipTrees.resolveStartingRootId(data);
        SkillTreeTopology topology = SkillTree.topology();
        synchronized (PATHS) {
            CachedPath cached = PATHS.get(data);
            if (cached != null && cached.socketId().equals(socketId) && Objects.equals(cached.rootId(), rootId) && cached.topology() == topology
                    && cached.revision() == data.revision()) {
                return cached.path();
            }
            List<String> path = rootId == null ? List.of() : shortestPath(topology, data, socketId, rootId);
            if (cached != null && cached.path().equals(path)) {
                path = cached.path();
            }
            PATHS.put(data, new CachedPath(socketId, rootId, topology, data.revision(), path));
            return path;
        }
    }

    static List<String> shortestPath(SkillTreeTopology topology, ShipSkillData data, String fromId, String toId) {
        Map<String, String> previous = new HashMap<>();
        Deque<String> frontier = new ArrayDeque<>();
        frontier.add(fromId);
        previous.put(fromId, null);
        while (!frontier.isEmpty()) {
            String currentId = frontier.poll();
            if (currentId.equals(toId)) {
                List<String> path = new ArrayList<>();
                for (String step = toId; step != null; step = previous.get(step)) {
                    path.add(step);
                }
                Collections.reverse(path);
                return List.copyOf(path);
            }
            SkillNode current = topology.node(currentId);
            if (current == null) {
                continue;
            }
            Set<String> neighbours = new TreeSet<>(current.getConnectedNodeIds());
            topology.dependents(currentId).forEach(dependent -> neighbours.add(dependent.getId()));
            String partnerId = ShipTreeLinks.wormholePartner(data, current);
            if (partnerId != null) {
                neighbours.add(partnerId);
            }
            for (String neighbourId : neighbours) {
                if (!previous.containsKey(neighbourId)) {
                    previous.put(neighbourId, currentId);
                    frontier.add(neighbourId);
                }
            }
        }
        return List.of();
    }
}
