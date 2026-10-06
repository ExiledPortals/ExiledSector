package exiledsector.ui.inspect;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import exiledsector.i18n.Translation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcFleetInspectPlugin implements InteractionDialogPlugin {

    private static final String CLOSE = "exiledSector_inspectLeave";

    private final CampaignFleetAPI fleet;
    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI dialog;

    public NpcFleetInspectPlugin(CampaignFleetAPI fleet) {
        this.fleet = fleet;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.getOptionPanel().addOption(Translation.gameText("inspect.close"), CLOSE);
        dialog.showCustomVisualDialog(NpcFleetInspectDialog.width(), NpcFleetInspectDialog.HEIGHT,
                new NpcFleetInspectDialog(List.of(fleet), dialog::dismiss));
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (CLOSE.equals(optionData)) {
            dialog.dismiss();
        }
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
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
