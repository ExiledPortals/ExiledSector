package exiledsector.loot;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public interface FleetDifficulty {

    default Float difficultyMultiplier(float battleDifficulty) {
        return null;
    }

    default boolean ratesShipLevels() {
        return false;
    }

    default int shipLevel(FleetMemberAPI member) {
        return 0;
    }

    default FleetLevelRange levelRange(MemoryAPI fleetMemory) {
        return null;
    }

    default String levelSeed(MemoryAPI fleetMemory) {
        return null;
    }
}
