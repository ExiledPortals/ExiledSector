package exiledsector.skills.unlock;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.apache.log4j.Logger;

import java.util.List;

public final class AnchorFinds {

    static final String FOUND_FLAG_PREFIX = "$exiledSector_anchorFound_";
    private static final Logger LOG = Logger.getLogger(AnchorFinds.class);

    private AnchorFinds() {
    }

    public static boolean isFound(SectorAPI sector, String volume) {
        MemoryAPI sectorMemory = sector == null || volume == null ? null : sector.getMemoryWithoutUpdate();
        return sectorMemory != null && sectorMemory.getBoolean(FOUND_FLAG_PREFIX + volume);
    }

    public static void record(SectorAPI sector, String volume, String reason) {
        if (markFound(sector, volume, reason) && opensAGate(volume)) {
            RelockedGates.reopen(sector, UnlockConditionType.ANCHOR_FOUND);
        }
    }

    private static boolean opensAGate(String volume) {
        for (List<UnlockCondition> conditions : NodeGates.all().values()) {
            for (UnlockCondition condition : conditions) {
                if (condition.getType() == UnlockConditionType.ANCHOR_FOUND && volume.equals(condition.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean markFound(SectorAPI sector, String volume, String reason) {
        MemoryAPI sectorMemory = sector == null || volume == null ? null : sector.getMemoryWithoutUpdate();
        if (sectorMemory == null) {
            return false;
        }
        if (!sectorMemory.getBoolean(FOUND_FLAG_PREFIX + volume)) {
            sectorMemory.set(FOUND_FLAG_PREFIX + volume, true);
            LOG.info("[ExiledSector] Recorded a found " + volume + " anchor: " + reason);
        }
        return true;
    }
}
