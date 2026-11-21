package exiledsector.charts.anomaly.breach;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.charts.anomaly.debris.DormantFleets;
import exiledsector.loot.BattleEngagement;
import exiledsector.loot.BattleLoot;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

final class BreachBattles {

    private static WeakReference<BattleLoot> lastBattle = new WeakReference<>(null);
    private static float anomalousDeploymentPoints;
    private static float totalDeploymentPoints;

    private BreachBattles() {
    }

    static void record(BattleLoot battle, BattleEngagement engagement) {
        record(battle, engagement, DormantFleets::isDebrisFleet);
    }

    static synchronized void record(BattleLoot battle, BattleEngagement engagement, Predicate<CampaignFleetAPI> anomalous) {
        if (lastBattle.get() != battle) {
            lastBattle = new WeakReference<>(battle);
            anomalousDeploymentPoints = 0f;
            totalDeploymentPoints = 0f;
        }
        if (engagement == null) {
            return;
        }
        for (Map.Entry<CampaignFleetAPI, List<FleetMemberAPI>> fleetLosses : engagement.lostEnemiesBySourceFleet().entrySet()) {
            float deploymentPoints = BattleEngagement.deploymentPoints(fleetLosses.getValue());
            totalDeploymentPoints += deploymentPoints;
            if (anomalous.test(fleetLosses.getKey())) {
                anomalousDeploymentPoints += deploymentPoints;
            }
        }
    }

    static synchronized float anomalousShare() {
        return totalDeploymentPoints > 0f ? Math.min(1f, anomalousDeploymentPoints / totalDeploymentPoints) : 0f;
    }

    static synchronized void reset() {
        lastBattle = new WeakReference<>(null);
        anomalousDeploymentPoints = 0f;
        totalDeploymentPoints = 0f;
    }
}
