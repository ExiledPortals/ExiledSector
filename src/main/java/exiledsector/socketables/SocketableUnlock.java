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
    FOUND_PLANETKILLER("found_planetkiller", sector -> sector.getMemoryWithoutUpdate().getBoolean("$pk_recovered"));

    public static final int UNIQUE_MIN_PLAYER_LEVEL = 15;
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

    public static boolean canDrop(SocketableDefinition definition, SectorAPI sector) {
        if (!definition.unique()) {
            return true;
        }
        if (sector == null) {
            return false;
        }
        MutableCharacterStatsAPI player = sector.getPlayerStats();
        if (player == null || player.getLevel() < UNIQUE_MIN_PLAYER_LEVEL) {
            return false;
        }
        if (definition.unlock().isEmpty()) {
            return true;
        }
        SocketableUnlock unlock = byId(definition.unlock());
        return unlock != null && unlock.isMet(sector);
    }

    boolean isMet(SectorAPI sector) {
        MemoryAPI memory = sector.getMemoryWithoutUpdate();
        String latch = LATCH_PREFIX + id;
        if (memory.getBoolean(latch)) {
            return true;
        }
        if (!condition.test(sector)) {
            return false;
        }
        memory.set(latch, true);
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
