package exiledsector.items;

import exiledsector.skills.ShipSkillData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SectionCustody implements ShipCustody {

    public static final SectionCustody INSTANCE = new SectionCustody();

    private SectionCustody() {
    }

    @Override
    public boolean holdsItems(ShipSkillData shipData) {
        return !shipData.getSectionItems().isEmpty();
    }

    @Override
    public List<CargoRefund> release(ShipSkillData shipData, boolean destroyed, ItemStore store) {
        for (String socketTypeId : Set.copyOf(shipData.getSectionItems().keySet())) {
            Item item = store.find(shipData.removeFromSection(socketTypeId));
            if (destroyed && item != null) {
                store.remove(item);
            }
        }
        return List.of();
    }

    @Override
    public boolean unsocketIfMisfit(ShipSkillData shipData, Item item) {
        String itemId = item.id();
        for (Map.Entry<String, String> slotEntry : List.copyOf(shipData.getSectionItems().entrySet())) {
            if (slotEntry.getValue().equals(itemId) && !fitsSection(item, slotEntry.getKey())) {
                shipData.removeFromSection(slotEntry.getKey());
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Item> returnMisfits(ShipSkillData shipData, ItemStore store) {
        List<Item> returnedItems = new ArrayList<>();
        for (Map.Entry<String, String> slotEntry : List.copyOf(shipData.getSectionItems().entrySet())) {
            Item item = store.find(slotEntry.getValue());
            if (item != null && item.kind() != null && !fitsSection(item, slotEntry.getKey())) {
                shipData.removeFromSection(slotEntry.getKey());
                returnedItems.add(item);
            }
        }
        return returnedItems;
    }

    static boolean fitsSection(Item item, String socketTypeId) {
        return HullItem.fits(item, ShipSection.byIdOrNull(socketTypeId));
    }
}
