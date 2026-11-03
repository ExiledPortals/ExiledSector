package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.Translation;

import java.util.List;
import java.util.Map;

public final class ExiledSectorDisableChartOptions extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params == null) {
            return false;
        }
        OptionPanelAPI options = dialog.getOptionPanel();
        for (Misc.Token param : params) {
            String optionId = param.getString(memoryMap);
            if (options.hasOption(optionId)) {
                options.setEnabled(optionId, false);
                options.setTooltip(optionId, Translation.gameText("chart.run.option.temporarySystem"));
            }
        }
        return true;
    }
}
