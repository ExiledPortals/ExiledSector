package exiledsector.charts.content;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;

public final class ChartFleetOrders {

    private ChartFleetOrders() {
    }

    public static boolean canTakeOrders(CampaignFleetAPI fleet) {
        return fleet != null && !fleet.isPlayerFleet() && fleet.getAI() != null && !fleet.isStationMode() && !fleet.isDespawning();
    }

    public static void putFirst(CampaignFleetAPI fleet, FleetAssignment assignment, SectorEntityToken target, String actionText) {
        if (!hasOrder(fleet.getAI(), assignment, target)) {
            fleet.addAssignmentAtStart(assignment, target, ChartContentGenerator.ASSIGNMENT_DAYS, actionText, null);
        }
    }

    public static void withdraw(CampaignFleetAPI fleet, FleetAssignment assignment, SectorEntityToken target) {
        CampaignFleetAIAPI ai = fleet == null ? null : fleet.getAI();
        if (ai == null) {
            return;
        }
        for (FleetAssignmentDataAPI order : ai.getAssignmentsCopy()) {
            if (isOrder(order, assignment, target)) {
                ai.removeAssignment(order);
            }
        }
    }

    static boolean hasOrder(CampaignFleetAIAPI ai, FleetAssignment assignment, SectorEntityToken target) {
        for (FleetAssignmentDataAPI order : ai.getAssignmentsCopy()) {
            if (isOrder(order, assignment, target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOrder(FleetAssignmentDataAPI order, FleetAssignment assignment, SectorEntityToken target) {
        return order != null && order.getAssignment() == assignment && order.getTarget() == target;
    }
}
