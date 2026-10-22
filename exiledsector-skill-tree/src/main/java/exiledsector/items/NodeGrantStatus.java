package exiledsector.items;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record NodeGrantStatus(String nodeId, State state, String reason) {

    public enum State {
        ACTIVE, READY, BY_PATH, DUPLICATE, INELIGIBLE, AREA_OFF, UNKNOWN_NODE
    }

    private record Remembered(String shipId, List<NodeGrantStatus> statuses) {
    }

    private static final Map<String, Remembered> LAST_KNOWN_BY_ITEM_ID = new ConcurrentHashMap<>();

    public static NodeGrantStatus of(String nodeId, State state) {
        return new NodeGrantStatus(nodeId, state, null);
    }

    public static NodeGrantStatus find(List<NodeGrantStatus> statuses, String nodeId) {
        if (statuses == null || nodeId == null) {
            return null;
        }
        for (NodeGrantStatus status : statuses) {
            if (nodeId.equals(status.nodeId())) {
                return status;
            }
        }
        return null;
    }

    public static void rememberForShip(String shipId, Map<String, List<NodeGrantStatus>> statusesByItemId) {
        LAST_KNOWN_BY_ITEM_ID.values().removeIf(remembered -> remembered.shipId().equals(shipId));
        statusesByItemId.forEach((itemId, statuses) -> LAST_KNOWN_BY_ITEM_ID.put(itemId, new Remembered(shipId, List.copyOf(statuses))));
    }

    public static void forget(String itemId) {
        if (itemId != null) {
            LAST_KNOWN_BY_ITEM_ID.remove(itemId);
        }
    }

    public static List<NodeGrantStatus> lastKnown(String itemId) {
        Remembered remembered = itemId == null ? null : LAST_KNOWN_BY_ITEM_ID.get(itemId);
        return remembered == null ? List.of() : remembered.statuses();
    }

    public static void forgetAll() {
        LAST_KNOWN_BY_ITEM_ID.clear();
    }
}
