package exiledsector.items;

import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public abstract class SlottedItemCustody implements ShipCustody {

    protected abstract Map<String, String> itemIdsBySlot(ShipSkillData shipData);

    protected abstract void emptySlot(ShipSkillData shipData, String slotId);

    @Override
    public boolean holdsItems(ShipSkillData shipData) {
        return !itemIdsBySlot(shipData).isEmpty();
    }

    protected final boolean emptyFirstSlotHolding(ShipSkillData shipData, Item item, Predicate<String> misfitsSlot) {
        for (Map.Entry<String, String> slotEntry : List.copyOf(itemIdsBySlot(shipData).entrySet())) {
            if (slotEntry.getValue().equals(item.id()) && misfitsSlot.test(slotEntry.getKey())) {
                emptySlot(shipData, slotEntry.getKey());
                return true;
            }
        }
        return false;
    }

    protected final List<Item> emptyMisfitSlots(ShipSkillData shipData, ItemStore store, BiPredicate<Item, String> misfitsSlot) {
        List<Item> returnedItems = new ArrayList<>();
        for (Map.Entry<String, String> slotEntry : List.copyOf(itemIdsBySlot(shipData).entrySet())) {
            Item item = store.find(slotEntry.getValue());
            if (item != null && item.kind() != null && misfitsSlot.test(item, slotEntry.getKey())) {
                emptySlot(shipData, slotEntry.getKey());
                returnedItems.add(item);
            }
        }
        return returnedItems;
    }
}
