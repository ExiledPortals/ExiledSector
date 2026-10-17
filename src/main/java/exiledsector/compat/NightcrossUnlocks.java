package exiledsector.compat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;

public final class NightcrossUnlocks {

    static final String PASCAL_SYSTEM_NAME = "Pascal";
    static final String PASCAL_VISITED_FLAG = "$exiledSector_pascalVisited";
    static final String PROTOTYPE_FOUND_FLAG = "$exiledSector_ancientPrototypeFound";
    static final String LUNAR_FLEET_DEFEATED_SECTOR_FLAG = "$na_defeated_macula";
    static final String LUNAR_FLEET_DEFEATED_PLAYER_FLAG = "$defeatedLunarFleet";
    static final String PROTOTYPE_DEFENDERS_DEFEATED_FLAG = "$na_defeatedAutoUnitFleet";
    static final List<String> PROTOTYPE_WRECK_IDS = List.of("na_mare_x_wreck", "na_sop_x_wreck", "na_macula_x_wreck");
    static final Set<String> PROTOTYPE_HULL_IDS = Set.of("na_mare_proto", "na_sop_proto", "na_macula_proto");
    private static final Logger LOG = Logger.getLogger(NightcrossUnlocks.class);

    private NightcrossUnlocks() {
    }

    public static boolean isPascalVisited(SectorAPI sector) {
        if (sector == null) {
            return false;
        }
        if (sector.getMemoryWithoutUpdate().getBoolean(PASCAL_VISITED_FLAG)) {
            return true;
        }
        StarSystemAPI pascal = sector.getStarSystem(PASCAL_SYSTEM_NAME);
        if (pascal == null) {
            return true;
        }
        if (pascal.isEnteredByPlayer()) {
            record(sector, PASCAL_VISITED_FLAG, "the player has entered Pascal");
            return true;
        }
        return false;
    }

    public static boolean isLunarFleetDefeated(SectorAPI sector) {
        if (sector == null) {
            return false;
        }
        MemoryAPI playerMemory = sector.getPlayerMemoryWithoutUpdate();
        return sector.getMemoryWithoutUpdate().getBoolean(LUNAR_FLEET_DEFEATED_SECTOR_FLAG)
                || playerMemory != null && playerMemory.getBoolean(LUNAR_FLEET_DEFEATED_PLAYER_FLAG);
    }

    public static boolean isAncientPrototypeFound(SectorAPI sector) {
        if (sector == null) {
            return false;
        }
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        return memory.getBoolean(PROTOTYPE_FOUND_FLAG) || memory.getBoolean(PROTOTYPE_DEFENDERS_DEFEATED_FLAG)
                || memory.getBoolean(LUNAR_FLEET_DEFEATED_SECTOR_FLAG);
    }

    public static void checkSaveOnLoad(SectorAPI sector) {
        if (sector == null) {
            return;
        }
        isPascalVisited(sector);
        if (isAncientPrototypeFound(sector)) {
            return;
        }
        if (anyPrototypeWreckDiscovered(sector)) {
            record(sector, PROTOTYPE_FOUND_FLAG, "an Ancient Prototype wreck has been discovered");
        } else if (playerOwnsPrototype(sector.getPlayerFleet())) {
            record(sector, PROTOTYPE_FOUND_FLAG, "the player already owns an Ancient Prototype hull");
        }
    }

    static void recordDiscovery(SectorAPI sector, SectorEntityToken entity) {
        if (sector != null && entity != null && PROTOTYPE_WRECK_IDS.contains(entity.getId())
                && !sector.getMemoryWithoutUpdate().getBoolean(PROTOTYPE_FOUND_FLAG)) {
            record(sector, PROTOTYPE_FOUND_FLAG, "the player discovered an Ancient Prototype wreck");
        }
    }

    private static boolean anyPrototypeWreckDiscovered(SectorAPI sector) {
        for (String wreckId : PROTOTYPE_WRECK_IDS) {
            SectorEntityToken wreck = sector.getEntityById(wreckId);
            if (wreck != null && !wreck.isDiscoverable()) {
                return true;
            }
        }
        return false;
    }

    static boolean playerOwnsPrototype(CampaignFleetAPI playerFleet) {
        if (playerFleet == null || playerFleet.getFleetData() == null) {
            return false;
        }
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            ShipHullSpecAPI hullSpec = member.getHullSpec();
            if (hullSpec != null && (PROTOTYPE_HULL_IDS.contains(hullSpec.getHullId()) || PROTOTYPE_HULL_IDS.contains(hullSpec.getBaseHullId()))) {
                return true;
            }
        }
        return false;
    }

    private static void record(SectorAPI sector, String flag, String reason) {
        CompatTarget.raiseSectorFlag(sector, flag, LOG, "Recording " + flag + ": " + reason);
    }

    public static final class DiscoveryListener implements DiscoverEntityListener {

        @Override
        public void reportEntityDiscovered(SectorEntityToken entity) {
            recordDiscovery(Global.getSector(), entity);
        }
    }
}
