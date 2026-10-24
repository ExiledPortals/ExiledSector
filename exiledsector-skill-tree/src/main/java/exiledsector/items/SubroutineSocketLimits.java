package exiledsector.items;

import exiledlib.i18n.Translation;
import exiledsector.skills.ShipSkillData;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class SubroutineSocketLimits {

    private static final Logger LOG = Logger.getLogger(SubroutineSocketLimits.class);

    private SubroutineSocketLimits() {
    }

    public static String refusal(Item item, String socketNodeId, Map<String, String> socketedItemIds) {
        if (item == null) {
            return null;
        }
        ItemDefinition definition = item.definition();
        SubroutineKind kind = kindOf(definition);
        if (limitReached(kind, item.id(), socketNodeId, socketedItemIds, SubroutineSocketLimits::definitionOf)) {
            return Translation.msg("socketable.socket.kindLimit").arg("name", item.name()).arg("limit", kind.socketLimitPerShip()).text();
        }
        if (definitionLimitReached(definition, item.id(), socketNodeId, socketedItemIds, SubroutineSocketLimits::definitionOf)) {
            return Translation.msg("socketable.socket.definitionLimit").arg("name", item.name()).arg("limit", definition.socketLimit()).text();
        }
        return null;
    }

    public static boolean limitReached(Item item, String socketNodeId, Map<String, String> socketedItemIds) {
        return item != null && limitReached(item.definition(), item.id(), socketNodeId, socketedItemIds);
    }

    public static boolean limitReached(ItemDefinition definition, String socketNodeId, Map<String, String> socketedItemIds) {
        return limitReached(definition, null, socketNodeId, socketedItemIds);
    }

    private static boolean limitReached(ItemDefinition definition, String itemId, String socketNodeId, Map<String, String> socketedItemIds) {
        return limitReached(kindOf(definition), itemId, socketNodeId, socketedItemIds, SubroutineSocketLimits::definitionOf)
                || definitionLimitReached(definition, itemId, socketNodeId, socketedItemIds, SubroutineSocketLimits::definitionOf);
    }

    static boolean definitionLimitReached(ItemDefinition definition, String itemId, String socketNodeId, Map<String, String> socketedItemIds,
                                          Function<String, ItemDefinition> definitionLookup) {
        if (definition == null || !definition.limitsSocketsPerShip() || socketedItemIds == null) {
            return false;
        }
        return SocketPeers.count(socketedItemIds, socketNodeId, itemId, socketedItemId -> {
            ItemDefinition socketedDefinition = definitionLookup.apply(socketedItemId);
            return socketedDefinition != null && socketedDefinition.id().equals(definition.id());
        }) >= definition.socketLimit();
    }

    static boolean limitReached(SubroutineKind kind, String itemId, String socketNodeId, Map<String, String> socketedItemIds,
                                Function<String, ItemDefinition> definitionLookup) {
        if (kind == null || !kind.limitsSocketsPerShip() || socketedItemIds == null) {
            return false;
        }
        return SocketPeers.count(socketedItemIds, socketNodeId, itemId, socketedItemId -> kindOf(definitionLookup.apply(socketedItemId)) == kind)
                >= kind.socketLimitPerShip();
    }

    static List<Item> unsocketExtras(Map<String, ShipSkillData> shipDataById, ItemStore store) {
        List<Item> unsocketedItems = SocketPeers.unsocketPastLimits(shipDataById, store, SubroutineSocketLimits::limitsOf);
        if (!unsocketedItems.isEmpty()) {
            LOG.info("[ExiledSector] Returned " + unsocketedItems.size() + " subroutines to storage that went past a per-ship limit: "
                    + unsocketedItems.stream().map(Item::definitionId).toList());
        }
        return unsocketedItems;
    }

    private static List<SocketPeers.Limit> limitsOf(Item item) {
        ItemDefinition definition = item.definition();
        SubroutineKind kind = kindOf(definition);
        List<SocketPeers.Limit> limits = new ArrayList<>(2);
        if (kind != null && kind.limitsSocketsPerShip()) {
            limits.add(new SocketPeers.Limit(kind, kind.socketLimitPerShip()));
        }
        if (definition != null && definition.limitsSocketsPerShip()) {
            limits.add(new SocketPeers.Limit(definition.id(), definition.socketLimit()));
        }
        return limits;
    }

    static ItemDefinition definitionOf(String itemId) {
        if (NpcItems.isNpcId(itemId)) {
            ItemData npcItem = NpcItems.item(itemId);
            return npcItem == null ? null : npcItem.definition();
        }
        Item ownedItem = ItemStore.lookup(itemId);
        return ownedItem == null ? null : ownedItem.definition();
    }

    private static SubroutineKind kindOf(ItemDefinition definition) {
        return definition == null || definition.type() != ItemType.SUBROUTINE ? null : SubroutineKind.of(definition);
    }
}
