package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.anomaly.skirmish.FactionSkirmish;
import exiledsector.charts.anomaly.skirmish.SkirmishSide;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

public final class ChartSkirmishes {

    static final String PUSHED_MEMORY_KEY = "$exiledSector_chartSkirmishPushed";

    private ChartSkirmishes() {
    }

    public static boolean checkFinalPush(SectorAPI sector) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || !run.runState().isLive() || !run.anomalyIds().contains(FactionSkirmish.ID)) {
            return false;
        }
        StarSystemAPI system = sector.getStarSystem(run.systemId());
        return system != null && pushCommander(system);
    }

    public static boolean pushCommander(StarSystemAPI system) {
        CampaignFleetAPI commander = null;
        CampaignFleetAPI station = null;
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (fleet.isDespawning() || fleet.isEmpty()) {
                continue;
            }
            MemoryAPI memory = fleet.getMemoryWithoutUpdate();
            String side = memory.getString(ChartIds.SKIRMISH_SIDE_MEMORY_KEY);
            if (SkirmishSide.ATTACKER.key().equals(side)) {
                if (!memory.getBoolean(ChartIds.SKIRMISH_COMMANDER_MEMORY_KEY)) {
                    return false;
                }
                commander = fleet;
            } else if (SkirmishSide.DEFENDER.key().equals(side) && fleet.isStationMode()) {
                station = fleet;
            }
        }
        if (commander == null || commander.getMemoryWithoutUpdate().getBoolean(PUSHED_MEMORY_KEY)) {
            return false;
        }
        commander.getMemoryWithoutUpdate().set(PUSHED_MEMORY_KEY, true);
        commander.clearAssignments();
        if (station != null) {
            commander.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, station, ChartContentGenerator.ASSIGNMENT_DAYS);
        }
        SectorEntityToken patrolCentre = system.getCenter() != null ? system.getCenter() : system.getStar();
        if (patrolCentre != null) {
            commander.addAssignment(FleetAssignment.PATROL_SYSTEM, patrolCentre, ChartContentGenerator.ASSIGNMENT_DAYS);
        }
        return true;
    }
}
