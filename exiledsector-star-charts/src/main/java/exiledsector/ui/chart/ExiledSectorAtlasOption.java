package exiledsector.ui.chart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.util.Misc;
import exiledsector.charts.StarChartSettings;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.i18n.Translation;
import exiledsector.ui.atlas.AtlasScreen;

import java.util.List;
import java.util.Map;

public final class ExiledSectorAtlasOption extends BaseCommandPlugin {

    public static final String OPTION_ID = "exiledSector_viewAtlas";
    static final String OPEN = "open";
    static final int OPTION_ORDER = 16;
    static final String POPULATE_OPTIONS = "PopulateOptions";

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
        if (dialog == null || !offeredAt(dialog)) {
            return false;
        }
        if (params != null && !params.isEmpty() && OPEN.equals(params.get(0).getString(memoryMap))) {
            return AtlasScreen.open(Global.getSector().getCampaignUI(), () -> FireAll.fire(null, dialog, memoryMap, POPULATE_OPTIONS));
        }
        OptionPanelAPI options = dialog.getOptionPanel();
        if (!options.hasOption(OPTION_ID)) {
            options.addOption(Translation.gameText("chart.gateway.option.colonyMenu"), OPTION_ID, Translation.gameText("chart.gateway.option.tooltip"));
        }
        return true;
    }

    private static boolean offeredAt(InteractionDialogAPI dialog) {
        return StarChartSettings.enabled() && WormholeGateway.statusAt(dialog.getInteractionTarget()) != WormholeGateway.Status.NONE;
    }
}
