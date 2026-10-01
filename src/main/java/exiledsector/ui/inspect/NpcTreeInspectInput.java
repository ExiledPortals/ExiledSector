package exiledsector.ui.inspect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SectorEntityToken.VisibilityLevel;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public class NpcTreeInspectInput implements CampaignInputListener {

    @Override
    public int getListenerInputPriority() {
        return 0;
    }

    @Override
    public void processCampaignInputPreCore(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (!event.isConsumed() && event.isKeyDownEvent() && event.getEventValue() == NpcInspectConfig.key() && tryOpen()) {
                event.consume();
            }
        }
    }

    @Override
    public void processCampaignInputPreFleetControl(List<InputEventAPI> events) {
    }

    @Override
    public void processCampaignInputPostCore(List<InputEventAPI> events) {
    }

    static boolean tryOpen() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        InteractionDialogAPI dialog = campaignUI.getCurrentInteractionDialog();
        if (dialog != null) {
            return openInEncounter(dialog);
        }
        if (campaignUI.getCurrentCoreTab() != null || campaignUI.isShowingMenu()) {
            return false;
        }
        CampaignFleetAPI fleet = inspectableFleet(Global.getSector().getMousedOverEntity());
        return fleet != null && campaignUI.showInteractionDialog(new NpcFleetInspectPlugin(fleet), fleet);
    }

    private static boolean openInEncounter(InteractionDialogAPI dialog) {
        if (dialog.getPlugin() instanceof NpcFleetInspectPlugin
                || !(dialog.getInteractionTarget() instanceof CampaignFleetAPI target)) {
            return false;
        }
        dialog.showCustomVisualDialog(NpcFleetInspectDialog.width(), NpcFleetInspectDialog.HEIGHT,
                new NpcFleetInspectDialog(targetSide(target), null));
        return true;
    }

    static List<CampaignFleetAPI> targetSide(CampaignFleetAPI target) {
        BattleAPI battle = target.getBattle();
        List<CampaignFleetAPI> side = battle == null ? null : battle.getSideFor(target);
        return side == null || side.isEmpty() ? List.of(target) : side;
    }

    static CampaignFleetAPI inspectableFleet(SectorEntityToken entity) {
        if (!(entity instanceof CampaignFleetAPI fleet) || fleet.isPlayerFleet()) {
            return null;
        }
        VisibilityLevel visibility = fleet.getVisibilityLevelToPlayerFleet();
        return visibility != null && visibility.ordinal() >= VisibilityLevel.COMPOSITION_DETAILS.ordinal() ? fleet : null;
    }
}
