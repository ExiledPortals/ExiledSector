package exiledsector.ui.atlas;

import com.fs.starfarer.api.Global;
import exiledsector.ui.BaseDialogPlugin;

final class AtlasAbilityDialog extends BaseDialogPlugin {

    private boolean opened;
    private boolean dismissed;

    @Override
    protected void open() {
        options().clearOptions();
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
        if (dismissed || dialog() == null) {
            return;
        }
        dismissed = true;
        if (Global.getSector().getCampaignUI().getCurrentInteractionDialog() == dialog()) {
            dialog().dismiss();
        }
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
    }
}
