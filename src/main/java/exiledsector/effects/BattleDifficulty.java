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
        CampaignUIAPI campaignUi = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
        InteractionDialogAPI dialog = campaignUi == null ? null : campaignUi.getCurrentInteractionDialog();
        InteractionDialogPlugin dialogPlugin = dialog == null ? null : dialog.getPlugin();
        if (dialogPlugin != null && dialogPlugin.getContext() instanceof FleetEncounterContext encounterContext && encounterContext.isComputedDifficulty()) {
            return encounterContext.getDifficulty();
        }
        return 1f;
    }
}
