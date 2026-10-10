package exiledsector.socketables;

import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;

import java.util.Map;

public final class SocketableDisassembly {

    public static final String PARTS_COMMODITY_ID = "exiledSector_socketable_parts";

    private SocketableDisassembly() {
    }

    public static int disassemble(Socketable socketable, SocketMaterials materials) {
        return disassemble(socketable, materials, SocketableStore.get(), ShipSkillDataManager.all());
    }

    static int disassemble(Socketable socketable, SocketMaterials materials, SocketableStore store, Map<String, ShipSkillData> shipDataById) {
        if (socketable == null || SocketCustody.isInstalled(shipDataById, socketable) || !store.remove(socketable)) {
            return 0;
        }
        int partsGained = socketable.rarity().disassemblyParts();
        materials.add(PARTS_COMMODITY_ID, partsGained);
        return partsGained;
    }
}
