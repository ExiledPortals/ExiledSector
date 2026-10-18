package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import exiledsector.i18n.Translation;

import java.util.HashMap;
import java.util.Map;

final class StarChartDialogPlugin implements InteractionDialogPlugin {

    static final String LEAVE = "exiledSector_starChartsLeave";

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI interactionDialog;

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        this.interactionDialog = interactionDialog;
        interactionDialog.getOptionPanel().addOption(Translation.gameText("chart.ui.dialog.leave"), LEAVE);
        interactionDialog.showCustomVisualDialog(StarChartScreen.dialogWidth(), StarChartScreen.dialogHeight(),
                StarChartScreen.forDialog(interactionDialog));
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (LEAVE.equals(optionData)) {
            interactionDialog.dismiss();
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
