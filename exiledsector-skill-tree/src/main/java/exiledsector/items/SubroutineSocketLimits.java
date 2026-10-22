package exiledsector.items;

import exiledlib.i18n.Translation;
import exiledsector.skills.ShipSkillData;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
        int sameDefinitionCount = 0;
        for (Map.Entry<String, String> socketedEntry : socketedItemIds.entrySet()) {
            String socketedItemId = socketedEntry.getValue();
            if (socketedEntry.getKey().equals(socketNodeId) || socketedItemId == null || socketedItemId.equals(itemId)) {
                continue;
            }
            ItemDefinition socketedDefinition = definitionLookup.apply(socketedItemId);
            if (socketedDefinition != null && socketedDefinition.id().equals(definition.id())) {
                sameDefinitionCount++;
            }
        }
        return sameDefinitionCount >= definition.socketLimit();
    }

    static boolean limitReached(SubroutineKind kind, String itemId, String socketNodeId, Map<String, String> socketedItemIds,
                                Function<String, ItemDefinition> definitionLookup) {
        if (kind == null || !kind.limitsSocketsPerShip() || socketedItemIds == null) {
            return false;
        }
        int sameKindCount = 0;
        for (Map.Entry<String, String> socketedEntry : socketedItemIds.entrySet()) {
            String socketedItemId = socketedEntry.getValue();
            if (socketedEntry.getKey().equals(socketNodeId) || socketedItemId == null || socketedItemId.equals(itemId)) {
                continue;
            }
            if (kindOf(definitionLookup.apply(socketedItemId)) == kind) {
                sameKindCount++;
            }
        }
        return sameKindCount >= kind.socketLimitPerShip();
    }

    static List<Item> unsocketExtras(Map<String, ShipSkillData> shipDataById, ItemStore store) {
        List<Item> unsocketedItems = new ArrayList<>();
        for (ShipSkillData shipData : shipDataById.values()) {
            Map<SubroutineKind, Integer> keptCountByKind = new EnumMap<>(SubroutineKind.class);
            Map<String, Integer> keptCountByDefinitionId = new HashMap<>();
            for (Map.Entry<String, String> socketedEntry : new TreeMap<>(shipData.getSocketedItems()).entrySet()) {
                Item item = store.find(socketedEntry.getValue());
                ItemDefinition definition = item == null ? null : item.definition();
                SubroutineKind kind = kindOf(definition);
                boolean kindLimited = kind != null && kind.limitsSocketsPerShip();
                boolean definitionLimited = definition != null && definition.limitsSocketsPerShip();
                boolean pastKindLimit = kindLimited && keptCountByKind.getOrDefault(kind, 0) >= kind.socketLimitPerShip();
                boolean pastDefinitionLimit = definitionLimited && keptCountByDefinitionId.getOrDefault(definition.id(), 0) >= definition.socketLimit();
                if (pastKindLimit || pastDefinitionLimit) {
                    shipData.unsocketItem(socketedEntry.getKey());
                    unsocketedItems.add(item);
                    continue;
                }
                if (kindLimited) {
                    keptCountByKind.merge(kind, 1, Integer::sum);
                }
                if (definitionLimited) {
                    keptCountByDefinitionId.merge(definition.id(), 1, Integer::sum);
                }
            }
        }
        if (!unsocketedItems.isEmpty()) {
            LOG.info("[ExiledSector] Returned " + unsocketedItems.size() + " subroutines to storage that went past a per-ship limit: "
                    + unsocketedItems.stream().map(Item::definitionId).toList());
        }
        return unsocketedItems;
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
