package exiledsector.charts.run;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.wormhole.ChartWormholeDialog;

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
}
