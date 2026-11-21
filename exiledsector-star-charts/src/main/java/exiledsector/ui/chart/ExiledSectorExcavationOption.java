package exiledsector.ui.chart;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.anomaly.excavation.ExcavationSiteDialog;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;

import java.util.List;
import java.util.Map;

public final class ExiledSectorExcavationOption extends BaseCommandPlugin {

    static final String OPEN = "open";
    static final int OPTION_ORDER = 5;

    @Override
    public boolean doesCommandAddOptions() {
        return true;
    }

    @Override
    public int getOptionOrder(List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        return OPTION_ORDER;
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || !StarChartSettings.enabled()) {
            return false;
        }
        SectorEntityToken target = dialog.getInteractionTarget();
        ChartRun run = target == null ? null : ChartRuns.liveIn(target.getContainingLocation());
        if (run == null) {
            return false;
        }
        if (params != null && !params.isEmpty() && OPEN.equals(params.get(0).getString(memoryMap))) {
            if (!ExcavationSiteDialog.openFromPlanet(run, dialog)) {
                dialog.getOptionPanel().clearOptions();
                FireAll.fire(null, dialog, memoryMap, ExiledSectorAtlasOption.POPULATE_OPTIONS);
            }
            return true;
        }
        return ExcavationSiteDialog.addPlanetOption(run, dialog);
    }
}
