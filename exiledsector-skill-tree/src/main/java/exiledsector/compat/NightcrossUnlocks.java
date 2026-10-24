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
import exiledsector.loot.BattleEngagement;
import exiledsector.skills.unlock.GateTriggers;
import exiledsector.skills.unlock.UnlockConditionType;

import java.util.List;
import java.util.Set;

public final class NightcrossUnlocks {

    public static final String PASCAL_SYSTEM_NAME = "Pascal";
    static final String PASCAL_VISITED_FLAG = "$exiledSector_pascalVisited";
    static final String PROTOTYPE_FOUND_FLAG = "$exiledSector_ancientPrototypeFound";
    static final String EXISTENTIAL_THREAT_DEFEATED_SECTOR_FLAG = "$na_defeated_macula";
    static final String EXISTENTIAL_THREAT_DEFEATED_PLAYER_FLAG = "$defeatedLunarFleet";
    static final String EXISTENTIAL_THREAT_MEMORY_KEY = "$SDFStargazerLunarFleet";
    static final String PROTOTYPE_DEFENDERS_DEFEATED_FLAG = "$na_defeatedAutoUnitFleet";
    static final List<String> PROTOTYPE_WRECK_IDS = List.of("na_mare_x_wreck", "na_sop_x_wreck", "na_macula_x_wreck");
    static final Set<String> PROTOTYPE_HULL_IDS = Set.of("na_mare_proto", "na_sop_proto", "na_macula_proto");

    private NightcrossUnlocks() {
    }

    public static boolean pascalVisitedBeforeAnchors(SectorAPI sector) {
        if (sector == null) {
            return false;
        }
        MemoryAPI sectorMemory = sector.getMemoryWithoutUpdate();
        StarSystemAPI pascal = sector.getStarSystem(PASCAL_SYSTEM_NAME);
        if (sectorMemory != null && sectorMemory.getBoolean(PASCAL_VISITED_FLAG)) {
            return true;
        }
        return pascal == null ? NightcrossCompat.isModEnabled() : pascal.isEnteredByPlayer();
    }

    public static boolean isExistentialThreatDefeated(SectorAPI sector) {
        if (sector == null) {
            return false;
        }
        MemoryAPI playerMemory = sector.getPlayerMemoryWithoutUpdate();
        return sector.getMemoryWithoutUpdate().getBoolean(EXISTENTIAL_THREAT_DEFEATED_SECTOR_FLAG)
                || playerMemory != null && playerMemory.getBoolean(EXISTENTIAL_THREAT_DEFEATED_PLAYER_FLAG);
    }

    public static boolean existentialThreatBeatenIn(BattleEngagement engagement) {
        if (engagement == null || engagement.battle() == null || engagement.lostEnemies().isEmpty()) {
            return false;
        }
        for (FleetMemberAPI lost : engagement.lostEnemies()) {
            CampaignFleetAPI sourceFleet = lost == null ? null : engagement.battle().getSourceFleet(lost);
            if (sourceFleet != null && sourceFleet.getMemoryWithoutUpdate() != null
                    && sourceFleet.getMemoryWithoutUpdate().getBoolean(EXISTENTIAL_THREAT_MEMORY_KEY)) {
                return sourceFleet.getFleetData() == null || engagement.lostEnemies().containsAll(sourceFleet.getFleetData().getMembersListCopy());
            }
        }
        return false;
    }

    public static boolean isAncientPrototypeFound(SectorAPI sector) {
        if (sector == null) {
            return false;
        }
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        return memory.getBoolean(PROTOTYPE_FOUND_FLAG) || memory.getBoolean(PROTOTYPE_DEFENDERS_DEFEATED_FLAG);
    }

    public static void checkSaveOnLoad(SectorAPI sector) {
        if (sector == null) {
            return;
        }
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
                && GateTriggers.awaits(sector, UnlockConditionType.NIGHTCROSS_ANCIENT_PROTOTYPE_FOUND, isAncientPrototypeFound(sector))) {
            record(sector, PROTOTYPE_FOUND_FLAG, "the player discovered an Ancient Prototype wreck");
        }
    }

    public static void recordEngagement(SectorAPI sector, BattleEngagement engagement) {
        if (sector != null && GateTriggers.awaits(sector, UnlockConditionType.NIGHTCROSS_EXISTENTIAL_THREAT_DEFEATED, isExistentialThreatDefeated(sector))
                && existentialThreatBeatenIn(engagement)) {
            GateTriggers.record(sector, UnlockConditionType.NIGHTCROSS_EXISTENTIAL_THREAT_DEFEATED, null, null, null);
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
        GateTriggers.record(sector, UnlockConditionType.NIGHTCROSS_ANCIENT_PROTOTYPE_FOUND, sector.getMemoryWithoutUpdate(), flag,
                "Recording " + flag + ": " + reason);
    }

    public static final class DiscoveryListener implements DiscoverEntityListener {

        @Override
        public void reportEntityDiscovered(SectorEntityToken entity) {
            recordDiscovery(Global.getSector(), entity);
        }
    }
}
