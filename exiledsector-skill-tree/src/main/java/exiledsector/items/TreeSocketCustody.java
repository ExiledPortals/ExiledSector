package exiledsector.items;

import exiledlib.items.Item;
import exiledlib.items.ItemStore;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TreeSocketCustody implements ShipCustody {

    public static final TreeSocketCustody INSTANCE = new TreeSocketCustody();

    private TreeSocketCustody() {
    }

    @Override
    public boolean holdsItems(ShipSkillData shipData) {
        return !shipData.getSocketedItems().isEmpty();
    }

    @Override
    public List<SkillItemCost> release(ShipSkillData shipData, boolean destroyed, ItemStore store) {
        Set<String> grantOnlyBefore = destroyed ? Set.of() : grantOnlyNodeIds(shipData);
        for (String nodeId : Set.copyOf(shipData.getSocketedItems().keySet())) {
            String itemId = shipData.unsocketItem(nodeId);
            NodeGrantStatus.forget(itemId);
            Item item = store.find(itemId);
            if (destroyed && item != null) {
                store.remove(item);
            }
        }
        if (destroyed) {
            return List.of();
        }
        return ShipTrees.releaseUngrantedFreeNodes(shipData, SkillTree.topology(), ShipTrees.resolveStartingRootId(shipData), grantOnlyBefore).charges();
    }

    private static Set<String> grantOnlyNodeIds(ShipSkillData shipData) {
        if (ShipTrees.grantedNodesBySocket(shipData).isEmpty()) {
            return Set.of();
        }
        return ShipTrees.grantOnlyNodeIds(shipData, SkillTree.topology(), ShipTrees.resolveStartingRootId(shipData));
    }

    @Override
    public boolean unsocketIfMisfit(ShipSkillData shipData, Item item) {
        String itemId = item.id();
        for (Map.Entry<String, String> socketedEntry : List.copyOf(shipData.getSocketedItems().entrySet())) {
            if (socketedEntry.getValue().equals(itemId) && (!fitsTreeSocket(item)
                    || SubroutineSocketLimits.limitReached(item, socketedEntry.getKey(), shipData.getSocketedItems()))) {
                shipData.unsocketItem(socketedEntry.getKey());
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Item> returnMisfits(ShipSkillData shipData, ItemStore store) {
        List<Item> returnedItems = new ArrayList<>();
        for (Map.Entry<String, String> socketedEntry : List.copyOf(shipData.getSocketedItems().entrySet())) {
            Item item = store.find(socketedEntry.getValue());
            if (item != null && item.kind() != null && !fitsTreeSocket(item)) {
                shipData.unsocketItem(socketedEntry.getKey());
                returnedItems.add(item);
            }
        }
        return returnedItems;
    }

    @Override
    public void enforceLimits(Map<String, ShipSkillData> shipDataById, ItemStore store) {
        SubroutineSocketLimits.unsocketExtras(shipDataById, store);
        WormholeAnchorSocketLimits.unsocketExtras(shipDataById, store);
    }

    static boolean fitsTreeSocket(Item item) {
        return item instanceof Subroutine || item instanceof WormholeAnchor;
    }

    @Override
    public void forgetShip(String shipId) {
        SkillTreeTemplateStore.clearAssignment(shipId);
    }
}
