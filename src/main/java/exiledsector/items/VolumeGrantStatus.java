package exiledsector.items;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record VolumeGrantStatus(State state, String reason) {

    public enum State {
        ACTIVE, READY, BY_PATH, DUPLICATE, INELIGIBLE, WAITING, AREA_OFF, UNKNOWN_NODE
    }

    private record Remembered(String shipId, VolumeGrantStatus status) {
    }

    private static final Map<String, Remembered> LAST_KNOWN_BY_ITEM_ID = new ConcurrentHashMap<>();

    public static VolumeGrantStatus of(State state) {
        return new VolumeGrantStatus(state, null);
    }

    public static void rememberForShip(String shipId, Map<String, VolumeGrantStatus> statusesByItemId) {
        LAST_KNOWN_BY_ITEM_ID.values().removeIf(remembered -> remembered.shipId().equals(shipId));
        statusesByItemId.forEach((itemId, status) -> LAST_KNOWN_BY_ITEM_ID.put(itemId, new Remembered(shipId, status)));
    }

    public static void forget(String itemId) {
        if (itemId != null) {
            LAST_KNOWN_BY_ITEM_ID.remove(itemId);
        }
    }

    public static VolumeGrantStatus lastKnown(String itemId) {
        Remembered remembered = itemId == null ? null : LAST_KNOWN_BY_ITEM_ID.get(itemId);
        return remembered == null ? null : remembered.status();
    }

    public static void forgetAll() {
        LAST_KNOWN_BY_ITEM_ID.clear();
    }
}
