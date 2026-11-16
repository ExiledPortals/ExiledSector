package exiledsector.charts.effect;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.officer.OfficerFleets;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.FleetLevelRange;

import java.util.List;

public final class ChartFleetDifficulty implements FleetDifficulty {

    public static final ChartFleetDifficulty INSTANCE = new ChartFleetDifficulty();

    private ChartFleetDifficulty() {
    }

    @Override
    public FleetLevelRange levelRange(MemoryAPI fleetMemory) {
        return ChartTiers.fleetStrengthRangeFor(fleetMemory);
    }

    @Override
    public String levelSeed(MemoryAPI fleetMemory) {
        return ChartIds.fleetSeed(fleetMemory);
    }

    @Override
    public String memberLevelSeed(MemoryAPI fleetMemory, FleetMemberAPI member) {
        return OfficerFleets.flagshipTreeSeed(fleetMemory, member);
    }

    @Override
    public int memberExtraLevels(MemoryAPI fleetMemory, FleetMemberAPI member) {
        return OfficerFleets.flagshipExtraLevels(fleetMemory, member);
    }

    @Override
    public List<String> grantedNodeIds(MemoryAPI fleetMemory) {
        List<String> nodeIds = ChartEnemyShipEffects.grantedNodeIds(fleetMemory);
        return nodeIds.isEmpty() ? null : nodeIds;
    }
}
