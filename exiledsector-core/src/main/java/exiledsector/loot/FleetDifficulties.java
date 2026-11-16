package exiledsector.loot;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.domain.DomainContributions;
import exiledsector.domain.ExiledSectorDomain;

import java.util.List;

public final class FleetDifficulties {

    private static final DomainContributions<FleetDifficulty> DIFFICULTIES = DomainContributions.fromEveryDomain(ExiledSectorDomain::fleetDifficulties);

    private FleetDifficulties() {
    }

    public static float difficultyMultiplier(float battleDifficulty) {
        return DIFFICULTIES.firstOr(difficulty -> difficulty.difficultyMultiplier(battleDifficulty), 1f);
    }

    public static boolean shipLevelsRated() {
        return DIFFICULTIES.all().stream().anyMatch(FleetDifficulty::ratesShipLevels);
    }

    public static int shipLevel(FleetMemberAPI member) {
        int level = 0;
        for (FleetDifficulty difficulty : DIFFICULTIES.all()) {
            level = Math.max(level, difficulty.shipLevel(member));
        }
        return level;
    }

    public static FleetLevelRange levelRange(MemoryAPI fleetMemory) {
        return DIFFICULTIES.first(difficulty -> difficulty.levelRange(fleetMemory));
    }

    public static String levelSeed(MemoryAPI fleetMemory) {
        return DIFFICULTIES.first(difficulty -> difficulty.levelSeed(fleetMemory));
    }

    public static String memberLevelSeed(MemoryAPI fleetMemory, FleetMemberAPI member) {
        return DIFFICULTIES.first(difficulty -> difficulty.memberLevelSeed(fleetMemory, member));
    }

    public static int memberExtraLevels(MemoryAPI fleetMemory, FleetMemberAPI member) {
        int levels = 0;
        for (FleetDifficulty difficulty : DIFFICULTIES.all()) {
            levels = Math.max(levels, difficulty.memberExtraLevels(fleetMemory, member));
        }
        return levels;
    }

    public static List<String> grantedNodeIds(MemoryAPI fleetMemory) {
        return DIFFICULTIES.firstOr(difficulty -> difficulty.grantedNodeIds(fleetMemory), List.of());
    }
}
