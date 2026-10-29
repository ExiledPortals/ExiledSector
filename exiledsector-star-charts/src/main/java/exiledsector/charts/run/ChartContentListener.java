package exiledsector.charts.run;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.objective.ChartObjectives;
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
            for (ChartAnomaly anomaly : ChartAnomalies.all()) {
                anomaly.fleetGone(Global.getSector(), fleet);
            }
        } catch (RuntimeException | LinkageError e) {
            reportFailure(e);
        }
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
    }

    private void reportFailure(Throwable cause) {
        if (!failureReported) {
            failureReported = true;
            LOG.error("[ExiledSector] Tracking a Star Chart objective failed; the objective is checked again every second while inside", cause);
        }
    }
}
