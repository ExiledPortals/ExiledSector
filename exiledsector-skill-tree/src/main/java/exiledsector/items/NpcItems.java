package exiledsector.items;

import exiledlib.items.Item;
import exiledlib.items.ItemCodec;
import exiledlib.items.ItemData;
import exiledlib.items.ItemType;
import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcItems {

    public static final String ID_PREFIX = ItemCodec.NPC_PREFIX;
    static final long ANCHOR_SEED = 0L;
    private static final Map<String, Item> PREVIEWS = new ConcurrentHashMap<>();

    private NpcItems() {
    }

    public static String id(String definitionId, long seed) {
        return ItemCodec.npc(definitionId, seed);
    }

    public static String id(ItemData itemData) {
        return ItemCodec.npc(itemData);
    }

    public static boolean isNpcId(String itemId) {
        return ItemCodec.isNpc(itemId);
    }

    public static ItemData item(String itemId) {
        return isNpcId(itemId) ? ItemCodec.decode(itemId) : null;
    }

    static Item resolve(String itemId) {
        ItemData itemData = item(itemId);
        if (itemData == null || itemData.definition() == null) {
            return null;
        }
        return PREVIEWS.computeIfAbsent(itemId, itemData::create);
    }

    public static String anchorFor(String volume) {
        String definitionId = volume == null ? null : WormholeAnchorDefinitions.definitionIdFor(volume);
        return definitionId == null || !WormholeAnchorDefinitions.areaActive(definitionId) ? null : id(definitionId, ANCHOR_SEED);
    }

    public static boolean isAnchor(String itemId) {
        ItemData itemData = item(itemId);
        return itemData != null && itemData.definition() != null && itemData.definition().type() == ItemType.WORMHOLE_ANCHOR;
    }

    public static List<String> unsocketAll(ShipSkillData shipData) {
        List<String> unsocketedNodeIds = new ArrayList<>();
        for (Map.Entry<String, String> socketedEntry : List.copyOf(shipData.getSocketedItems().entrySet())) {
            if (isNpcId(socketedEntry.getValue())) {
                shipData.unsocketItem(socketedEntry.getKey());
                unsocketedNodeIds.add(socketedEntry.getKey());
            }
        }
        return unsocketedNodeIds;
    }

    public static void clearCache() {
        PREVIEWS.clear();
    }
}
