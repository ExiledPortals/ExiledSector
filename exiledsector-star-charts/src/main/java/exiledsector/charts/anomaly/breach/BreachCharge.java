package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.anomaly.debris.DormantFleets;
import exiledsector.charts.content.ChartFleetMarks;
import exiledsector.charts.content.ChartFleetOrders;
import exiledsector.i18n.Translation;

final class BreachCharge {

    static final FleetAssignment ASSIGNMENT = FleetAssignment.ATTACK_LOCATION;

    private BreachCharge() {
    }

    static void rally(LocationAPI location, SectorEntityToken breach) {
        rally(location, breach, false);
    }

    static void rally(LocationAPI location, SectorEntityToken breach, boolean wakesAnomalousFleets) {
        String chargingText = null;
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (wakesAnomalousFleets && DormantFleets.isDormant(fleet)) {
                DormantFleets.wake(fleet);
            }
            if (canCharge(fleet)) {
                if (chargingText == null) {
                    chargingText = Translation.gameText("chart.anomaly.spatial_breach.fleet.charging");
                }
                ChartFleetOrders.putFirst(fleet, ASSIGNMENT, breach, chargingText);
            }
        }
    }

    static void release(LocationAPI location, SectorEntityToken breach) {
        for (CampaignFleetAPI fleet : location.getFleets()) {
            ChartFleetOrders.withdraw(fleet, ASSIGNMENT, breach);
        }
    }

    static boolean canCharge(CampaignFleetAPI fleet) {
        return ChartFleetOrders.canTakeOrders(fleet) && !DormantFleets.isDormant(fleet) && !BreachIncursion.isGuard(fleet)
                && !ChartFleetMarks.isAvenger(fleet);
    }
}
