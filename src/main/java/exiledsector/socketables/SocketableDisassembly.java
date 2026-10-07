package exiledsector.socketables;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;

import java.util.Map;

public final class SocketableDisassembly {

    public static final String PARTS_COMMODITY_ID = "exiledSector_socketable_parts";

    private SocketableDisassembly() {
    }

    public static int disassemble(Socketable socketable, CargoAPI cargo) {
        return disassemble(socketable, cargo, SocketableStore.get(), ShipSkillDataManager.all());
    }

    static int disassemble(Socketable socketable, CargoAPI cargo, SocketableStore store, Map<String, ShipSkillData> ships) {
        if (socketable == null || cargo == null || SocketCustody.isInstalled(ships, socketable) || !store.remove(socketable)) {
            return 0;
        }
        int parts = socketable.rarity().disassemblyParts();
        cargo.addCommodity(PARTS_COMMODITY_ID, parts);
        return parts;
    }
}
