package exiledsector.charts.anomaly.skirmish;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartStations;

final class SkirmishAdvance {

    static final String PUSHED_MEMORY_KEY = "$exiledSector_chartSkirmishPushed";

    private SkirmishAdvance() {
    }

    static boolean pushAttacker(StarSystemAPI system) {
        CampaignFleetAPI attacker = null;
        CampaignFleetAPI station = null;
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (fleet.isDespawning() || fleet.isEmpty()) {
                continue;
            }
            FightingSide side = FightingSide.of(fleet);
            if (side == FightingSide.ATTACKER) {
                attacker = fleet;
            } else if (side == FightingSide.DEFENDER && ChartStations.isStation(fleet)) {
                station = fleet;
            }
        }
        if (attacker == null || attacker.getMemoryWithoutUpdate().getBoolean(PUSHED_MEMORY_KEY)) {
            return false;
        }
        attacker.getMemoryWithoutUpdate().set(PUSHED_MEMORY_KEY, true);
        attacker.clearAssignments();
        if (station != null) {
            attacker.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, station, ChartContentGenerator.ASSIGNMENT_DAYS);
        }
        ChartContentGenerator.patrolSystem(attacker, system);
        return true;
    }
}
