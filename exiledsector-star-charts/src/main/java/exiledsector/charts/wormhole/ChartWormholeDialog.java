package exiledsector.charts.wormhole;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunActions;
import exiledsector.charts.run.ChartRunState;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.BaseDialogPlugin;
import exiledsector.ui.VanillaText;

public class ChartWormholeDialog extends BaseDialogPlugin {

    static final String ENTER = "exiledSector_chartEnter";
    static final String COLLAPSE = "exiledSector_chartCollapse";
    static final String RETURN = "exiledSector_chartReturn";
    static final String LEAVE = "exiledSector_chartLeave";

    private SectorEntityToken wormhole;

    @Override
    protected void open() {
        wormhole = dialog().getInteractionTarget();
        showOptions();
    }

    public static boolean isChartWormhole(SectorEntityToken entity) {
        return entity != null && entity.hasTag(ChartIds.ENTITY_TAG)
                && (entity.hasTag(ChartIds.OUTER_WORMHOLE_TAG) || entity.hasTag(ChartIds.INNER_WORMHOLE_TAG));
    }

    private static boolean isInner(SectorEntityToken wormhole) {
        return wormhole.hasTag(ChartIds.INNER_WORMHOLE_TAG);
    }

    private void showOptions() {
        OptionPanelAPI options = options();
        options.clearOptions();
        ChartRun run = ChartRuns.unclosedOwning(wormhole);
        if (run == null) {
            VanillaText.plainPara(text(), Translation.gameText("chart.dialog.stale"));
        } else if (isInner(wormhole)) {
            showInnerOptions(run, options);
        } else {
            showOuterOptions(run, options);
        }
        addLeaveOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
    }

    private void showOuterOptions(ChartRun run, OptionPanelAPI options) {
        VanillaText.plainPara(text(), Translation.gameText("chart.dialog.outer.description"));
        int entriesLeft = run.entriesLeft();
        VanillaText.plainPara(text(), entriesLeft > 0
                ? I18n.forGameText(() -> Translation.msg("chart.dialog.outer.entriesLeft").count(entriesLeft).text())
                : Translation.gameText("chart.dialog.outer.noEntries"));
        options.addOption(I18n.forGameText(() -> Translation.msg("chart.dialog.option.enter").count(entriesLeft).text()), ENTER);
        options.setEnabled(ENTER, run.canEnter());
        options.addOption(Translation.gameText("chart.dialog.option.collapse"), COLLAPSE);
        options.addOptionConfirmation(COLLAPSE, Translation.gameText("chart.dialog.collapse.confirm"),
                Translation.gameText("chart.dialog.collapse.yes"), Translation.gameText("chart.dialog.collapse.no"));
        options.setEnabled(COLLAPSE, run.runState() == ChartRunState.OPEN);
    }

    private void showInnerOptions(ChartRun run, OptionPanelAPI options) {
        VanillaText.plainPara(text(), Translation.gameText("chart.dialog.inner.description"));
        boolean wayBackStaysOpen = !run.isOuterCollapsed() && run.entriesLeft() > 0;
        VanillaText.plainPara(text(), Translation.gameText(wayBackStaysOpen ? "chart.dialog.inner.canComeBack" : "chart.dialog.inner.lastExit"));
        options.addOption(Translation.gameText("chart.dialog.option.return"), RETURN);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        SectorAPI sector = Global.getSector();
        ChartRun run = ChartRuns.unclosedOwning(wormhole);
        if (ENTER.equals(optionData) && run != null) {
            dialog().dismiss();
            ChartRunActions.enter(sector, run, wormhole);
        } else if (COLLAPSE.equals(optionData) && run != null) {
            run.collapseOuter();
            dialog().dismiss();
        } else if (RETURN.equals(optionData) && run != null) {
            dialog().dismiss();
            ChartRunActions.returnToOrigin(sector, run, wormhole);
        } else {
            dialog().dismiss();
        }
    }
}
