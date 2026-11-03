package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import exiledsector.charts.anomaly.debris.DormantFleets;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.i18n.Translation;

final class BreachCharge {

    static final FleetAssignment ASSIGNMENT = FleetAssignment.ATTACK_LOCATION;

    private BreachCharge() {
    }

    static void rally(LocationAPI location, SectorEntityToken breach) {
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (canCharge(fleet) && !isCharging(fleet.getAI(), breach)) {
                fleet.addAssignmentAtStart(ASSIGNMENT, breach, ChartContentGenerator.ASSIGNMENT_DAYS,
                        Translation.gameText("chart.anomaly.spatial_breach.fleet.charging"), null);
            }
        }
    }

    static void release(LocationAPI location, SectorEntityToken breach) {
        for (CampaignFleetAPI fleet : location.getFleets()) {
            CampaignFleetAIAPI ai = fleet == null ? null : fleet.getAI();
            if (ai == null) {
                continue;
            }
            for (FleetAssignmentDataAPI assignment : ai.getAssignmentsCopy()) {
                if (isCharge(assignment, breach)) {
                    ai.removeAssignment(assignment);
                }
            }
        }
    }

    static boolean canCharge(CampaignFleetAPI fleet) {
        return fleet != null && !fleet.isPlayerFleet() && fleet.getAI() != null && !fleet.isStationMode() && !fleet.isDespawning()
                && !DormantFleets.isDormant(fleet);
    }

    private static boolean isCharging(CampaignFleetAIAPI ai, SectorEntityToken breach) {
        for (FleetAssignmentDataAPI assignment : ai.getAssignmentsCopy()) {
            if (isCharge(assignment, breach)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCharge(FleetAssignmentDataAPI assignment, SectorEntityToken breach) {
        return assignment != null && assignment.getAssignment() == ASSIGNMENT && assignment.getTarget() == breach;
    }
}
