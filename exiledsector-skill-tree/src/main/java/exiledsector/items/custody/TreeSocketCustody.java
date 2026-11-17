package exiledsector.items.custody;

import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.anchor.WormholeAnchor;
import exiledsector.items.anchor.WormholeAnchorSocketLimits;
import exiledsector.items.subroutine.NodeGrantStatus;
import exiledsector.items.subroutine.Subroutine;
import exiledsector.items.subroutine.SubroutineSocketLimits;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTreeLinks;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillTree;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TreeSocketCustody extends SlottedItemCustody {

    public static final TreeSocketCustody INSTANCE = new TreeSocketCustody();

    private TreeSocketCustody() {
    }

    @Override
    protected Map<String, String> itemIdsBySlot(ShipSkillData shipData) {
        return shipData.getSocketedItems();
    }

    @Override
    protected void emptySlot(ShipSkillData shipData, String nodeId) {
        shipData.unsocketItem(nodeId);
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
        if (ShipTreeLinks.grantedNodesBySocket(shipData).isEmpty()) {
            return Set.of();
        }
        return ShipTrees.grantOnlyNodeIds(shipData, SkillTree.topology(), ShipTrees.resolveStartingRootId(shipData));
    }

    @Override
    public boolean unsocketIfMisfit(ShipSkillData shipData, Item item) {
        return emptyFirstSlotHolding(shipData, item, nodeId -> !fitsTreeSocket(item)
                || SubroutineSocketLimits.limitReached(item, nodeId, shipData.getSocketedItems()));
    }

    @Override
    public List<Item> returnMisfits(ShipSkillData shipData, ItemStore store) {
        return emptyMisfitSlots(shipData, store, (socketedItem, nodeId) -> !fitsTreeSocket(socketedItem));
    }

    @Override
    public void enforceLimits(Map<String, ShipSkillData> shipDataById, ItemStore store) {
        SubroutineSocketLimits.unsocketExtras(shipDataById, store);
        WormholeAnchorSocketLimits.unsocketExtras(shipDataById, store);
    }

    public static boolean fitsTreeSocket(Item item) {
        return item instanceof Subroutine || item instanceof WormholeAnchor;
    }

    @Override
    public void forgetShip(String shipId) {
        SkillTreeTemplateStore.clearAssignment(shipId);
    }
}
