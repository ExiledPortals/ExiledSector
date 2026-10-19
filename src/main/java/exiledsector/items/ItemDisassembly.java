package exiledsector.items;

import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;

import java.util.Map;

public final class ItemDisassembly {

    public static final String PARTS_ITEM_ID = "exiledSector_socketable_parts";

    private ItemDisassembly() {
    }

    public static int disassemble(Item item, CraftingMaterials materials) {
        return disassemble(item, materials, ItemStore.get(), ShipSkillDataManager.all());
    }

    public static boolean canDisassemble(Item item) {
        return item != null && item.type() != ItemType.WORMHOLE_ANCHOR;
    }

    static int disassemble(Item item, CraftingMaterials materials, ItemStore store, Map<String, ShipSkillData> shipDataById) {
        if (!canDisassemble(item) || SocketCustody.isInstalled(shipDataById, item) || !store.remove(item)) {
            return 0;
        }
        int partsGained = item.rarity().disassemblyParts();
        materials.add(PARTS_ITEM_ID, partsGained);
        return partsGained;
    }
}
