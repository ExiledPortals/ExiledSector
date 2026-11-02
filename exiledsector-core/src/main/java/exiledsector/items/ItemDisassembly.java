package exiledsector.items;

import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;

import java.util.Map;

public final class ItemDisassembly {

    public static final String FEEDSTOCK_ITEM_ID = "exiledSector_nanite_feedstock";

    private ItemDisassembly() {
    }

    public static int disassemble(Item item, CraftingMaterials materials) {
        return disassemble(item, materials, ItemStore.get(), ShipSkillDataManager.all());
    }

    public static int disassemble(Item item, CraftingMaterials materials, ItemStore store) {
        return store == null ? 0 : disassemble(item, materials, store, ShipSkillDataManager.all());
    }

    static int disassemble(Item item, CraftingMaterials materials, ItemStore store, Map<String, ShipSkillData> shipDataById) {
        if (item == null ||SocketCustody.isInstalled(shipDataById, item) || !store.remove(item)) {
            return 0;
        }
        int feedstockGained = item.rarity().disassemblyFeedstock();
        materials.add(FEEDSTOCK_ITEM_ID, feedstockGained);
        return feedstockGained;
    }
}
