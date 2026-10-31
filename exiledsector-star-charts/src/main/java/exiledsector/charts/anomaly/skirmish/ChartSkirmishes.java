package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartSites;

public final class ChartSkirmishes {

    static final String PUSHED_MEMORY_KEY = "$exiledSector_chartSkirmishPushed";

    private ChartSkirmishes() {
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
        SectorEntityToken patrolCentre = ChartSites.centreOf(system);
        if (patrolCentre != null) {
            commander.addAssignment(FleetAssignment.PATROL_SYSTEM, patrolCentre, ChartContentGenerator.ASSIGNMENT_DAYS);
        }
        return true;
    }
}
