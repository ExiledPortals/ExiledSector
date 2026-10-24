package exiledsector.items;

import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

final class SocketPeers {

    record Limit(Object key, int limit) {
    }

    private SocketPeers() {
    }

    static int count(Map<String, String> socketedItemIds, String socketNodeId, String itemId, Predicate<String> matches) {
        if (socketedItemIds == null) {
            return 0;
        }
        int matching = 0;
        for (Map.Entry<String, String> socketedEntry : socketedItemIds.entrySet()) {
            String socketedItemId = socketedEntry.getValue();
            if (!socketedEntry.getKey().equals(socketNodeId) && socketedItemId != null && !socketedItemId.equals(itemId)
                    && matches.test(socketedItemId)) {
                matching++;
            }
        }
        return matching;
    }

    static List<Item> unsocketPastLimits(Map<String, ShipSkillData> shipDataById, ItemStore store, Function<Item, List<Limit>> limitsOf) {
        List<Item> unsocketedItems = new ArrayList<>();
        for (ShipSkillData shipData : shipDataById.values()) {
            Map<Object, Integer> keptCountByKey = new HashMap<>();
            for (Map.Entry<String, String> socketedEntry : new TreeMap<>(shipData.getSocketedItems()).entrySet()) {
                Item item = store.find(socketedEntry.getValue());
                List<Limit> limits = item == null ? List.of() : limitsOf.apply(item);
                if (limits.stream().anyMatch(limit -> keptCountByKey.getOrDefault(limit.key(), 0) >= limit.limit())) {
                    shipData.unsocketItem(socketedEntry.getKey());
                    unsocketedItems.add(item);
                    continue;
                }
                limits.forEach(limit -> keptCountByKey.merge(limit.key(), 1, Integer::sum));
            }
        }
        return unsocketedItems;
    }
}
