package exiledsector.hullitems;

import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;

import java.util.Map;

public final class HullItemDisassembly {

    public static final String PARTS_COMMODITY_ID = "exiledSector_socketable_parts";

    private HullItemDisassembly() {
    }

    public static int disassemble(HullItem hullItem, CraftingMaterials materials) {
        return disassemble(hullItem, materials, HullItemStore.get(), ShipSkillDataManager.all());
    }

    static int disassemble(HullItem hullItem, CraftingMaterials materials, HullItemStore store, Map<String, ShipSkillData> shipDataById) {
        if (hullItem == null || SocketCustody.isInstalled(shipDataById, hullItem) || !store.remove(hullItem)) {
            return 0;
        }
        int partsGained = hullItem.rarity().disassemblyParts();
        materials.add(PARTS_COMMODITY_ID, partsGained);
        return partsGained;
    }
}
