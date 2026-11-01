package exiledsector.items;

import exiledsector.skills.ShipSkillData;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class WormholeAnchorSocketLimits {

    private static final Logger LOG = Logger.getLogger(WormholeAnchorSocketLimits.class);

    private WormholeAnchorSocketLimits() {
    }

    public static UseVerdict refusal(WormholeAnchor anchor, String socketNodeId, Map<String, String> socketedItemIds) {
        if (anchor == null) {
            return UseVerdict.ALLOW;
        }
        String volume = anchor.volume();
        if (!volumeTaken(volume, anchor.id(), socketNodeId, socketedItemIds, WormholeAnchorSocketLimits::volumeOf)) {
            return UseVerdict.ALLOW;
        }
        return UseVerdict.refused("socketable.anchor.volumeLimit").with("volume", WormholeAnchor.volumeName(volume));
    }

    static boolean volumeTaken(String volume, String itemId, String socketNodeId, Map<String, String> socketedItemIds,
                               Function<String, String> volumeLookup) {
        if (volume == null || socketedItemIds == null) {
            return false;
        }
        return SocketPeers.count(socketedItemIds, socketNodeId, itemId, socketedItemId -> volume.equals(volumeLookup.apply(socketedItemId))) > 0;
    }

    static List<Item> unsocketExtras(Map<String, ShipSkillData> shipDataById, ItemStore store) {
        List<Item> unsocketedItems = SocketPeers.unsocketPastLimits(shipDataById, store,
                item -> item instanceof WormholeAnchor anchor && anchor.volume() != null ? List.of(new SocketPeers.Limit(anchor.volume(), 1)) : List.of());
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
