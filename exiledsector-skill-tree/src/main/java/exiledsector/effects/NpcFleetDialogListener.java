package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;

public class NpcFleetDialogListener extends BaseCampaignEventListener {

    static final float ENCOUNTER_RANGE = 2000f;

    public NpcFleetDialogListener() {
        super(false);
    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        if (dialog != null && dialog.getInteractionTarget() instanceof CampaignFleetAPI) {
            NpcFleetSweepScript.sweepAround(Global.getSector().getPlayerFleet(), ENCOUNTER_RANGE);
        }
    }
}
