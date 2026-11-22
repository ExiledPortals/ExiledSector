package exiledsector.charts.run;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.phasing.PhaseGateDialog;
import exiledsector.charts.phasing.PhasePull;
import exiledsector.charts.wormhole.ChartWormholeDialog;
import exiledsector.compat.RuthlessSectorCompat;

public class ChartCampaignPlugin extends BaseCampaignPlugin {

    public static final String ID = "exiledSector_chartCampaignPlugin";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        RuthlessSectorCompat.matchVictoryReputationTo(playerInChart());
        if (PhasePull.isPhaseGate(interactionTarget)) {
            return new PluginPick<>(new PhaseGateDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        if (ChartWormholeDialog.isChartWormhole(interactionTarget)) {
            return new PluginPick<>(new ChartWormholeDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        InteractionDialogPlugin anomalyDialog = ChartAnomalies.dialogFor(interactionTarget);
        return anomalyDialog == null ? null : new PluginPick<>(anomalyDialog, CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }

    @Override
    public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
        BattleCreationPlugin anomalyBattle = ChartAnomalies.battleFor(opponent);
        return anomalyBattle == null ? null : new PluginPick<>(anomalyBattle, CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }

    @Override
    public PluginPick<ReputationActionResponsePlugin> pickReputationActionResponsePlugin(Object action, String factionId) {
        return RunFactionReputation.ignores(factionId)
                ? new PluginPick<>(new RunFactionReputation(), CampaignPlugin.PickPriority.MOD_SPECIFIC) : null;
    }

    static boolean playerInChart() {
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        LocationAPI location = playerFleet == null ? null : playerFleet.getContainingLocation();
        return location != null && location.hasTag(ChartIds.SYSTEM_TAG);
    }
}
