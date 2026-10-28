package exiledsector.charts.run;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.charts.anomaly.convoy.ConvoyDialogs;
import exiledsector.charts.anomaly.debris.DormantFleetDialog;
import exiledsector.charts.anomaly.debris.DormantFleets;
import exiledsector.charts.anomaly.excavation.ExcavationSiteDialog;
import exiledsector.charts.anomaly.forge.DerelictForge;
import exiledsector.charts.anomaly.forge.ForgeDialog;
import exiledsector.charts.anomaly.excavation.ShaftBattleCreationPlugin;
import exiledsector.charts.anomaly.excavation.ShaftEncounters;
import exiledsector.charts.anomaly.relay.RelayDialog;
import exiledsector.charts.anomaly.relay.RelayNetwork;
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
        if (RelayNetwork.isRelay(interactionTarget)) {
            return new PluginPick<>(new RelayDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        if (DormantFleets.isDebrisFleet(interactionTarget)) {
            return new PluginPick<>(new DormantFleetDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        if (ChartWormholeDialog.isChartWormhole(interactionTarget)) {
            return new PluginPick<>(new ChartWormholeDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        if (DerelictForge.isForge(interactionTarget)) {
            return new PluginPick<>(new ForgeDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        if (ExcavationSiteDialog.wantsToHandle(interactionTarget)) {
            return new PluginPick<>(new ExcavationSiteDialog(), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        InteractionDialogPlugin convoyDialog = ConvoyDialogs.pick(interactionTarget);
        return convoyDialog == null ? null : new PluginPick<>(convoyDialog, CampaignPlugin.PickPriority.MOD_SPECIFIC);
    }

    @Override
    public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
        return ShaftEncounters.isShaftBattle(opponent) ? new PluginPick<>(new ShaftBattleCreationPlugin(), CampaignPlugin.PickPriority.MOD_SPECIFIC) : null;
    }
}
