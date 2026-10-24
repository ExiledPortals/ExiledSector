package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.CurrentLocationChangedListener;
import exiledlib.compat.CompatTarget;
import exiledsector.skills.unlock.GateTriggers;
import exiledsector.skills.unlock.UnlockConditionType;
import org.apache.log4j.Logger;

public final class LostSectorUnlocks {

    static final String FROZEN_HEART_SYSTEM_VISITED_FLAG = "$exiledSector_frozenHeartSystemVisited";
    static final String FROST_STAR_ID = "nskr_frost";
    static final String FROST_SYSTEM_NAME_KEY = "$nskr_frostName";
    private static final Logger LOG = Logger.getLogger(LostSectorUnlocks.class);

    private LostSectorUnlocks() {
    }

    public static boolean isFrozenHeartSystemVisited(SectorAPI sector) {
        if (sector == null) {
            return false;
        }
        if (sector.getMemoryWithoutUpdate().getBoolean(FROZEN_HEART_SYSTEM_VISITED_FLAG)) {
            return true;
        }
        StarSystemAPI frost = frostSystem(sector);
        if (frost == null) {
            return true;
        }
        if (frost.isEnteredByPlayer()) {
            CompatTarget.raiseSectorFlag(sector, FROZEN_HEART_SYSTEM_VISITED_FLAG, LOG,
                    "Unlocking the Enigma area: the player has entered " + frost.getBaseName() + ", the Frozen Heart's system");
            return true;
        }
        return false;
    }

    public static StarSystemAPI frostSystem(SectorAPI sector) {
        SectorEntityToken star = sector.getEntityById(FROST_STAR_ID);
        if (star != null && star.getContainingLocation() instanceof StarSystemAPI system) {
            return system;
        }
        Object systemName = sector.getPersistentData().get(FROST_SYSTEM_NAME_KEY);
        return systemName instanceof String name ? sector.getStarSystem(name) : null;
    }

    static void recordLocation(SectorAPI sector, LocationAPI location) {
        StarSystemAPI frost = location == null || sector == null ? null : frostSystem(sector);
        if (frost != null && location == frost) {
            GateTriggers.record(sector, UnlockConditionType.LOST_SECTOR_FROZEN_HEART_VISITED, sector.getMemoryWithoutUpdate(),
                    FROZEN_HEART_SYSTEM_VISITED_FLAG, "Unlocking the Enigma area: the player has entered " + frost.getBaseName() + ", the Frozen Heart's system");
        }
    }

    public static final class LocationListener implements CurrentLocationChangedListener {

        @Override
        public void reportCurrentLocationChanged(LocationAPI previousLocation, LocationAPI currentLocation) {
            recordLocation(Global.getSector(), currentLocation);
        }
    }
}
