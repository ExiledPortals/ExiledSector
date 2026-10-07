package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;

final class BattleDifficulty {

    private BattleDifficulty() {
    }

    static float current() {
        CampaignUIAPI ui = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
        InteractionDialogAPI dialog = ui == null ? null : ui.getCurrentInteractionDialog();
        InteractionDialogPlugin plugin = dialog == null ? null : dialog.getPlugin();
        if (plugin != null && plugin.getContext() instanceof FleetEncounterContext context && context.isComputedDifficulty()) {
            return context.getDifficulty();
        }
        return 1f;
    }
}
