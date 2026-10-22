package exiledsector.items;

import exiledlib.i18n.Translation;
import exiledsector.skills.ShipSkillData;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

public final class WormholeAnchorSocketLimits {

    private static final Logger LOG = Logger.getLogger(WormholeAnchorSocketLimits.class);

    private WormholeAnchorSocketLimits() {
    }

    public static String refusal(WormholeAnchor anchor, String socketNodeId, Map<String, String> socketedItemIds) {
        if (anchor == null) {
            return null;
        }
        String volume = anchor.volume();
        if (!volumeTaken(volume, anchor.id(), socketNodeId, socketedItemIds, WormholeAnchorSocketLimits::volumeOf)) {
            return null;
        }
        return Translation.msg("socketable.anchor.volumeLimit").arg("name", anchor.name())
                .arg("volume", WormholeAnchor.volumeName(volume)).text();
    }

    static boolean volumeTaken(String volume, String itemId, String socketNodeId, Map<String, String> socketedItemIds,
                               Function<String, String> volumeLookup) {
        if (volume == null || socketedItemIds == null) {
            return false;
        }
        for (Map.Entry<String, String> socketedEntry : socketedItemIds.entrySet()) {
            String socketedItemId = socketedEntry.getValue();
            if (socketedEntry.getKey().equals(socketNodeId) || socketedItemId == null || socketedItemId.equals(itemId)) {
                continue;
            }
            if (volume.equals(volumeLookup.apply(socketedItemId))) {
                return true;
            }
        }
        return false;
    }

    static List<Item> unsocketExtras(Map<String, ShipSkillData> shipDataById, ItemStore store) {
        List<Item> unsocketedItems = new ArrayList<>();
        for (ShipSkillData shipData : shipDataById.values()) {
            Set<String> keptVolumes = new HashSet<>();
            for (Map.Entry<String, String> socketedEntry : new TreeMap<>(shipData.getSocketedItems()).entrySet()) {
                Item item = store.find(socketedEntry.getValue());
                String volume = item instanceof WormholeAnchor anchor ? anchor.volume() : null;
                if (volume != null && !keptVolumes.add(volume)) {
                    shipData.unsocketItem(socketedEntry.getKey());
                    unsocketedItems.add(item);
                }
            }
        }
        if (!unsocketedItems.isEmpty()) {
            LOG.info("[ExiledSector] Returned " + unsocketedItems.size() + " wormhole anchors to storage that repeated a volume already anchored on the same ship: "
                    + unsocketedItems.stream().map(Item::definitionId).toList());
        }
        return unsocketedItems;
    }

    static String volumeOf(String itemId) {
        Item item = ItemStore.lookup(itemId);
        return item instanceof WormholeAnchor anchor ? anchor.volume() : null;
    }
}
