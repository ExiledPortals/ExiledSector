package exiledsector.socketables;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.intel.misc.GateHaulerIntel;

import java.util.function.Predicate;

public enum SocketableUnlock {

    FOUND_ONSLAUGHT_MK1("found_onslaught_mk1", sector -> sector.getMemoryWithoutUpdate().getBoolean("$foundOneslaught")),
    FOUND_GATE_HAULER("found_gate_hauler", SocketableUnlock::foundGateHauler),
    FOUND_PLANETKILLER("found_planetkiller", sector -> sector.getMemoryWithoutUpdate().getBoolean("$pk_recovered")),
    DEFEATED_ZIGGURAT("defeated_ziggurat", sector -> sector.getMemoryWithoutUpdate().getBoolean("$defeatedZiggurat"));

    public static final int UNIQUE_MIN_PLAYER_LEVEL = 15;
    public static final int FRAMEWORK_MIN_PLAYER_LEVEL = 15;
    private static final String LATCH_PREFIX = "$exiledSector_socketUnlock_";

    private final String id;
    private final Predicate<SectorAPI> condition;

    SocketableUnlock(String id, Predicate<SectorAPI> condition) {
        this.id = id;
        this.condition = condition;
    }

    public String id() {
        return id;
    }

    public static SocketableUnlock byId(String id) {
        for (SocketableUnlock unlock : values()) {
            if (unlock.id.equals(id)) {
                return unlock;
            }
        }
        return null;
    }

    public static boolean frameworksOpen(SectorAPI sector) {
        MutableCharacterStatsAPI playerStats = sector == null ? null : sector.getPlayerStats();
        return playerStats != null && frameworksOpen(playerStats.getLevel());
    }

    public static boolean frameworksOpen(int playerLevel) {
        return playerLevel >= FRAMEWORK_MIN_PLAYER_LEVEL;
    }

    public static boolean canDrop(SocketableDefinition definition, SectorAPI sector) {
        if (!definition.unique()) {
            return true;
        }
        if (sector == null) {
            return false;
        }
        MutableCharacterStatsAPI playerStats = sector.getPlayerStats();
        if (playerStats == null || playerStats.getLevel() < UNIQUE_MIN_PLAYER_LEVEL) {
            return false;
        }
        return unlockConditionMet(definition, sector);
    }

    public static boolean unlockConditionMet(SocketableDefinition definition, SectorAPI sector) {
        if (definition.unlock().isEmpty()) {
            return true;
        }
        SocketableUnlock unlock = sector == null ? null : byId(definition.unlock());
        return unlock != null && unlock.isMet(sector);
    }

    boolean isMet(SectorAPI sector) {
        MemoryAPI sectorMemory = sector.getMemoryWithoutUpdate();
        String latchKey = LATCH_PREFIX + id;
        if (sectorMemory.getBoolean(latchKey)) {
            return true;
        }
        if (!condition.test(sector)) {
            return false;
        }
        sectorMemory.set(latchKey, true);
        return true;
    }

    private static boolean foundGateHauler(SectorAPI sector) {
        if (sector.getIntelManager().hasIntelOfClass(GateHaulerIntel.class)) {
            return true;
        }
        for (StarSystemAPI system : sector.getStarSystems()) {
            if (system.getMemoryWithoutUpdate().getBoolean("$deployedGateHaulerHere")) {
                return true;
            }
        }
        return false;
    }
}
