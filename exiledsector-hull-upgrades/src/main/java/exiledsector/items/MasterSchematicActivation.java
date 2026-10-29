package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.PlayerFleet;
import exiledsector.effects.PlayerFleetStats;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import org.apache.log4j.Logger;

import java.util.Collection;

public final class MasterSchematicActivation {

    static final String ACTIVE_KEY = "$exiledSector_masterSchematicsActive";
    static final String STARTER_GRANTED_KEY = "$exiledSector_starterMasterSchematicGranted";
    private static final Logger LOG = Logger.getLogger(MasterSchematicActivation.class);

    private MasterSchematicActivation() {
    }

    public static void settingChanged() {
        if (PlayerFleet.orNull() != null) {
            sync(Global.getSector());
        }
        PlayerFleetStats.markForStatUpdate();
    }

    public static void sync(SectorAPI sector) {
        MemoryAPI memory = sector == null ? null : sector.getMemoryWithoutUpdate();
        if (memory == null) {
            return;
        }
        if (!MasterSchematic.required()) {
            memory.unset(ACTIVE_KEY);
            MasterSchematic.changed();
            return;
        }
        if (memory.getBoolean(ACTIVE_KEY)) {
            return;
        }
        memory.set(ACTIVE_KEY, true);
        Collection<ShipSkillData> playerShips = ShipSkillDataManager.all().values();
        int unlockedCount = unlockShipsWithNodes(playerShips);
        if (unlockedCount > 0) {
            LOG.info("[ExiledSector] Master Schematics are required: unlocked " + unlockedCount + " ships that already had nodes allocated");
        }
        if (playerShips.stream().noneMatch(ShipSkillData::isMasterSchematicd) && !memory.getBoolean(STARTER_GRANTED_KEY)) {
            ItemStore.get().addMasterSchematics(1);
            memory.set(STARTER_GRANTED_KEY, true);
        }
        MasterSchematic.changed();
    }

    static int unlockShipsWithNodes(Collection<ShipSkillData> playerShips) {
        int unlockedCount = 0;
        for (ShipSkillData shipData : playerShips) {
            if (!shipData.isNpcBuild() && shipData.getAllocatedNodeIds().size() > 1 && shipData.unlockSkillTree()) {
                unlockedCount++;
            }
        }
        return unlockedCount;
    }
}
