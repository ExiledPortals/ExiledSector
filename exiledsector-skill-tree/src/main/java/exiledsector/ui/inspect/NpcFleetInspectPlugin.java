package exiledsector.ui.inspect;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.i18n.Translation;
import exiledsector.ui.BaseDialogPlugin;

import java.util.List;

public class NpcFleetInspectPlugin extends BaseDialogPlugin {

    private static final String CLOSE = "exiledSector_inspectLeave";

    private final CampaignFleetAPI inspectedFleet;

    public NpcFleetInspectPlugin(CampaignFleetAPI inspectedFleet) {
        this.inspectedFleet = inspectedFleet;
    }

    @Override
    protected void open() {
        options().addOption(Translation.gameText("inspect.close"), CLOSE);
        dialog().showCustomVisualDialog(NpcFleetInspectDialog.width(), NpcFleetInspectDialog.HEIGHT,
                new NpcFleetInspectDialog(List.of(inspectedFleet), dialog()::dismiss));
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (CLOSE.equals(optionData)) {
            dialog().dismiss();
        }
    }
}
