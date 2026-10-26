package exiledsector.skills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ShipTreeLinks {

    private ShipTreeLinks() {
    }

    public static WormholePartners wormholePartners(ShipSkillData data) {
        return node -> wormholePartner(data, node);
    }

    public static String wormholePartner(ShipSkillData data, SkillNode node) {
        String anchorPartner = anchorPartner(data, node.getId());
        return anchorPartner != null ? anchorPartner : WormholePartners.pairedWith(node);
    }

    public static String anchorPartner(ShipSkillData data, String nodeId) {
        ShipSkillData.AnchorLinkMaps links = anchorLinks(data);
        String exitId = links.exitBySocketId().get(nodeId);
        return exitId != null ? exitId : links.socketByExitId().get(nodeId);
    }

    public static String anchorExit(ShipSkillData data, String socketNodeId) {
        return anchorLinks(data).exitBySocketId().get(socketNodeId);
    }

    public static String anchorSocket(ShipSkillData data, String exitNodeId) {
        return anchorLinks(data).socketByExitId().get(exitNodeId);
    }

    public static Map<String, String> anchorExitsBySocket(ShipSkillData data) {
        return Collections.unmodifiableMap(anchorLinks(data).exitBySocketId());
    }

    public static List<SkillTreeTopology.WormholePair> wormholePairs(ShipSkillData data, SkillTreeTopology topology) {
        Map<String, String> exitBySocketId = anchorLinks(data).exitBySocketId();
        if (exitBySocketId.isEmpty()) {
            return topology.wormholePairs();
        }
        List<SkillTreeTopology.WormholePair> pairs = new ArrayList<>(topology.wormholePairs());
        exitBySocketId.forEach((socketId, exitId) -> {
            SkillNode socket = topology.node(socketId);
            SkillNode exit = topology.node(exitId);
            if (socket != null && exit != null && data.isAllocated(exitId)) {
                pairs.add(new SkillTreeTopology.WormholePair(socket, exit));
            }
        });
        return pairs;
    }

    private static ShipSkillData.AnchorLinkMaps anchorLinks(ShipSkillData data) {
        AnchorLinks.ExitLookup lookup = AnchorLinks.lookup();
        int lookupRevision = lookup.revision();
        LinkCache<ShipSkillData.AnchorLinkMaps> cache = data.anchorLinkCache();
        if (cache.isFresh(data.revision, lookup, lookupRevision)) {
            return cache.links();
        }
        Map<String, String> exitBySocketId = new LinkedHashMap<>();
        Map<String, String> socketByExitId = new LinkedHashMap<>();
        if (data.socketedItems != null) {
            new TreeMap<>(data.socketedItems).forEach((socketId, itemId) -> {
                if (data.isAllocated(socketId)) {
                    link(data, socketId, lookup.exitNodeId(itemId), exitBySocketId, socketByExitId);
                }
            });
        }
        return cache.store(new ShipSkillData.AnchorLinkMaps(exitBySocketId.isEmpty() ? Map.of() : exitBySocketId,
                socketByExitId.isEmpty() ? Map.of() : socketByExitId), data.revision, lookup, lookupRevision);
    }

    private static void link(ShipSkillData data, String socketId, String exitId, Map<String, String> exitBySocketId, Map<String, String> socketByExitId) {
        if (exitId == null || exitId.equals(socketId) || !data.isAllocated(socketId) || socketByExitId.containsKey(exitId)
                || exitBySocketId.containsKey(exitId) || socketByExitId.containsKey(socketId)) {
            return;
        }
        exitBySocketId.put(socketId, exitId);
        socketByExitId.put(exitId, socketId);
    }

    public static Map<String, List<String>> grantedNodesBySocket(ShipSkillData data) {
        return Collections.unmodifiableMap(grantLinks(data));
    }

    public static String grantingSocket(ShipSkillData data, String nodeId) {
        for (Map.Entry<String, List<String>> grantLink : grantLinks(data).entrySet()) {
            if (grantLink.getValue().contains(nodeId)) {
                return grantLink.getKey();
            }
        }
        return null;
    }

    public static boolean isGrantHeld(ShipSkillData data, String nodeId) {
        return data.isAllocated(nodeId) && grantingSocket(data, nodeId) != null;
    }

    static Set<String> grantedNodeIds(ShipSkillData data) {
        Set<String> granted = new HashSet<>();
        grantedNodesBySocket(data).values().forEach(granted::addAll);
        return granted;
    }

    private static Map<String, List<String>> grantLinks(ShipSkillData data) {
        NodeGrants.GrantLookup lookup = NodeGrants.lookup();
        int lookupRevision = lookup.revision();
        LinkCache<Map<String, List<String>>> cache = data.grantLinkCache();
        if (cache.isFresh(data.revision, lookup, lookupRevision)) {
            return cache.links();
        }
        Map<String, List<String>> nodesBySocketId = new LinkedHashMap<>();
        if (data.socketedItems != null) {
            new TreeMap<>(data.socketedItems).forEach((socketId, itemId) -> {
                List<String> grantedNodeIds = data.isAllocated(socketId) ? lookup.grantedNodeIds(itemId, socketId) : List.of();
                List<String> linkedNodeIds = grantedNodeIds == null ? List.of()
                        : grantedNodeIds.stream().filter(nodeId -> nodeId != null && !nodeId.equals(socketId)).distinct().toList();
                if (!linkedNodeIds.isEmpty()) {
                    nodesBySocketId.put(socketId, linkedNodeIds);
                }
            });
        }
        return cache.store(nodesBySocketId.isEmpty() ? Map.of() : nodesBySocketId, data.revision, lookup, lookupRevision);
    }
}
