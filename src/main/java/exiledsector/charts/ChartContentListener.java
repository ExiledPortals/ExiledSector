package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.listeners.ShowLootListener;
import org.apache.log4j.Logger;

public class ChartContentListener implements FleetEventListener, ShowLootListener {

    private static final Logger LOG = Logger.getLogger(ChartContentListener.class);
    private boolean failureReported;

    @Override
    public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        if (!ChartRuns.hasRun()) {
            return;
        }
        try {
            ChartObjectives.fleetGone(Global.getSector(), fleet, reason);
        } catch (RuntimeException | LinkageError e) {
            reportFailure(e);
        }
    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
        // TODO phase 4+: battle-scoped chart rules (for example drops seeded per destroyed ship) hook in here
    }

    @Override
    public void reportAboutToShowLootToPlayer(CargoAPI loot, InteractionDialogAPI dialog) {
        if (dialog == null || !ChartRuns.hasRun()) {
            return;
        }
        try {
            ChartObjectives.lootShown(Global.getSector(), dialog.getInteractionTarget());
        } catch (RuntimeException | LinkageError e) {
            reportFailure(e);
        }
    }

    private void reportFailure(Throwable cause) {
        if (!failureReported) {
            failureReported = true;
            LOG.error("[ExiledSector] Tracking a Star Chart objective failed; the objective is checked again every second while inside", cause);
        }
    }
}
