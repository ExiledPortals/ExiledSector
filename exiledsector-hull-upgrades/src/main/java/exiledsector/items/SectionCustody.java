package exiledsector.items;

import exiledsector.skills.ShipSkillData;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SectionCustody extends SlottedItemCustody {

    public static final SectionCustody INSTANCE = new SectionCustody();

    private SectionCustody() {
    }

    @Override
    protected Map<String, String> itemIdsBySlot(ShipSkillData shipData) {
        return shipData.getSectionItems();
    }

    @Override
    protected void emptySlot(ShipSkillData shipData, String sectionKindId) {
        shipData.removeFromSection(sectionKindId);
    }

    @Override
    public List<CargoRefund> release(ShipSkillData shipData, boolean destroyed, ItemStore store) {
        for (String sectionKindId : Set.copyOf(shipData.getSectionItems().keySet())) {
            Item item = store.find(shipData.removeFromSection(sectionKindId));
            if (destroyed && item != null) {
                store.remove(item);
            }
        }
        return List.of();
    }

    @Override
    public boolean unsocketIfMisfit(ShipSkillData shipData, Item item) {
        return emptyFirstSlotHolding(shipData, item, sectionKindId -> !fitsSection(item, sectionKindId));
    }

    @Override
    public List<Item> returnMisfits(ShipSkillData shipData, ItemStore store) {
        return emptyMisfitSlots(shipData, store, (socketedItem, sectionKindId) -> !fitsSection(socketedItem, sectionKindId));
    }

    static boolean fitsSection(Item item, String sectionKindId) {
        return HullItem.fits(item, ShipSection.byIdOrNull(sectionKindId));
    }
}
