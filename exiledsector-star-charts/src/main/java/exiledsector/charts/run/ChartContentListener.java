package exiledsector.charts.run;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.objective.ChartObjectives;
import exiledsector.charts.officer.OfficerSightings;
import exiledsector.charts.officer.turn.OfficerTurns;
import org.apache.log4j.Logger;

public class ChartContentListener implements FleetEventListener {

    private static final Logger LOG = Logger.getLogger(ChartContentListener.class);
    private boolean failureReported;

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        if (!ChartRuns.hasRun()) {
            return;
        }
        runSafely(() -> {
            ChartObjectives.fleetGone(Global.getSector(), fleet, reason, param);
            AnomalyRun run = AnomalyRun.live(Global.getSector());
            if (run != null) {
                ChartAnomalies.fleetGone(run, fleet);
            }
        });
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (!ChartRuns.hasRun()) {
            return;
        }
        runSafely(() -> ChartTransponderReputation.battleOccurred(battle));
        runSafely(() -> OfficerSightings.battleOccurred(battle));
        runSafely(() -> OfficerTurns.battleOccurred(battle, primaryWinner));
    }

    private void runSafely(Runnable step) {
        try {
            step.run();
        } catch (RuntimeException | LinkageError e) {
            if (!failureReported) {
                failureReported = true;
                LOG.error("[ExiledSector] Handling a Star Chart fleet or battle report failed; the objective is checked again every second while inside", e);
            }
        }
    }
}
