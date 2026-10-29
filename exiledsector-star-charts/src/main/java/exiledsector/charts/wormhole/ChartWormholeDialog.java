package exiledsector.charts.wormhole;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunActions;
import exiledsector.charts.run.ChartRunState;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public class ChartWormholeDialog implements InteractionDialogPlugin {

    static final String ENTER = "exiledSector_chartEnter";
    static final String COLLAPSE = "exiledSector_chartCollapse";
    static final String RETURN = "exiledSector_chartReturn";
    static final String LEAVE = "exiledSector_chartLeave";

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI interactionDialog;
    private SectorEntityToken wormhole;

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        this.interactionDialog = interactionDialog;
        this.wormhole = interactionDialog.getInteractionTarget();
        showOptions();
    }

    public static boolean isChartWormhole(SectorEntityToken entity) {
        return entity != null && entity.hasTag(ChartIds.ENTITY_TAG)
                && (entity.hasTag(ChartIds.OUTER_WORMHOLE_TAG) || entity.hasTag(ChartIds.INNER_WORMHOLE_TAG));
    }

    static ChartRun runFor(SectorEntityToken wormhole) {
        ChartRun run = ChartRuns.currentOrNull();
        if (run == null || wormhole == null || !ChartIds.belongsToRun(wormhole, run.runId()) || run.runState() == ChartRunState.CLOSED) {
            return null;
        }
        return run;
    }

    private static boolean isInner(SectorEntityToken wormhole) {
        return wormhole.hasTag(ChartIds.INNER_WORMHOLE_TAG);
    }

    private void showOptions() {
        OptionPanelAPI options = interactionDialog.getOptionPanel();
        options.clearOptions();
        ChartRun run = runFor(wormhole);
        if (run == null) {
            VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText("chart.dialog.stale"));
        } else if (isInner(wormhole)) {
            showInnerOptions(run, options);
        } else {
            showOuterOptions(run, options);
        }
        options.addOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void showOuterOptions(ChartRun run, OptionPanelAPI options) {
        VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText("chart.dialog.outer.description"));
        int entriesLeft = run.entriesLeft();
        VanillaText.plainPara(interactionDialog.getTextPanel(), entriesLeft > 0
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
        VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText("chart.dialog.inner.description"));
        boolean wayBackStaysOpen = !run.isOuterCollapsed() && run.entriesLeft() > 0;
        VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText(wayBackStaysOpen ? "chart.dialog.inner.canComeBack" : "chart.dialog.inner.lastExit"));
        options.addOption(Translation.gameText("chart.dialog.option.return"), RETURN);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        SectorAPI sector = Global.getSector();
        ChartRun run = runFor(wormhole);
        if (ENTER.equals(optionData) && run != null) {
            interactionDialog.dismiss();
            ChartRunActions.enter(sector, run, wormhole);
        } else if (COLLAPSE.equals(optionData) && run != null) {
            ChartRunActions.collapseOuter(run);
            interactionDialog.dismiss();
        } else if (RETURN.equals(optionData) && run != null) {
            interactionDialog.dismiss();
            ChartRunActions.returnToOrigin(sector, run, wormhole);
        } else {
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
