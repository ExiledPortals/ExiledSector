package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;

import java.util.HashMap;
import java.util.Map;

final class AtlasAbilityDialog implements InteractionDialogPlugin {

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI dialog;
    private boolean opened;
    private boolean dismissed;

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        dialog = interactionDialog;
        interactionDialog.getOptionPanel().clearOptions();
        opened = openAtlas();
    }

    @Override
    public void advance(float amount) {
        if (opened || dismissed) {
            return;
        }
        opened = openAtlas();
        if (!opened) {
            dismiss();
        }
    }

    private boolean openAtlas() {
        return AtlasScreen.open(Global.getSector().getCampaignUI(), this::dismiss);
    }

    private void dismiss() {
        if (dismissed || dialog == null) {
            return;
        }
        dismissed = true;
        if (Global.getSector().getCampaignUI().getCurrentInteractionDialog() == dialog) {
            dialog.dismiss();
        }
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }
}
