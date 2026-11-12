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
        try {
            ChartObjectives.fleetGone(Global.getSector(), fleet, reason);
            AnomalyRun run = AnomalyRun.live(Global.getSector());
            if (run != null) {
                ChartAnomalies.fleetGone(run, fleet);
            }
        } catch (RuntimeException | LinkageError e) {
            reportFailure(e);
        }
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (!ChartRuns.hasRun()) {
            return;
        }
        try {
            ChartTransponderReputation.battleOccurred(battle);
        } catch (RuntimeException | LinkageError e) {
            reportFailure(e);
        }
        try {
            OfficerSightings.battleOccurred(battle);
        } catch (RuntimeException | LinkageError e) {
            reportFailure(e);
        }
        try {
            OfficerTurns.battleOccurred(battle, primaryWinner);
        } catch (RuntimeException | LinkageError e) {
            reportFailure(e);
        }
    }

    private void reportFailure(Throwable cause) {
        if (!failureReported) {
            failureReported = true;
            LOG.error("[ExiledSector] Handling a Star Chart fleet or battle report failed; the objective is checked again every second while inside", cause);
        }
    }
}
