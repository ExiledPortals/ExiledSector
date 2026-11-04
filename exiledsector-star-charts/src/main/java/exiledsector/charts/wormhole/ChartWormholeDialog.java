package exiledsector.charts.wormhole;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.run.ChartEntryBans;
import exiledsector.charts.run.ChartEntryFuel;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunActions;
import exiledsector.charts.run.ChartRunState;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.BaseDialogPlugin;
import exiledsector.ui.VanillaText;

import java.util.Set;

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
            VanillaText.plainPara(text(), Translation.gameText("chart.wormhole.dialog.stale"));
        } else if (isInner(wormhole)) {
            showInnerOptions(run, options);
        } else {
            showOuterOptions(run, options);
        }
        addLeaveOption(Translation.gameText("chart.wormhole.dialog.option.leave"), LEAVE);
    }

    private void showOuterOptions(ChartRun run, OptionPanelAPI options) {
        VanillaText.plainPara(text(), Translation.gameText("chart.wormhole.dialog.outer.description"));
        int entriesLeft = run.entriesLeft();
        VanillaText.plainPara(text(), entriesLeft > 0
                ? I18n.forGameText(() -> Translation.msg("chart.wormhole.dialog.outer.entriesLeft").count(entriesLeft).text())
                : Translation.gameText("chart.wormhole.dialog.outer.noEntries"));
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        Set<HullSize> barred = ChartEntryBans.breached(run, playerFleet);
        for (HullSize hullSize : barred) {
            VanillaText.plainPara(text(), Translation.gameText("chart.wormhole.dialog.outer.barred." + hullSize.name()));
        }
        boolean fuelEnough = ChartEntryFuel.canAfford(run, playerFleet);
        if (entriesLeft > 0) {
            StyledText fuel = StyledText.styled(Misc.getWithDGS(ChartEntryFuel.cost(run)), fuelEnough ? Style.HIGHLIGHT : Style.BAD);
            VanillaText.addPara(text(), I18n.forGameText(() -> Translation.msg(fuelEnough ? "chart.wormhole.dialog.outer.fuel"
                    : "chart.wormhole.dialog.outer.fuelShort").arg("fuel", fuel).styled()), Misc.getTextColor());
        }
        options.addOption(I18n.forGameText(() -> Translation.msg("chart.wormhole.dialog.option.enter").count(entriesLeft).text()), ENTER);
        options.setEnabled(ENTER, run.canEnter() && barred.isEmpty() && fuelEnough);
        options.addOption(Translation.gameText("chart.wormhole.dialog.option.collapse"), COLLAPSE);
        options.addOptionConfirmation(COLLAPSE, Translation.gameText("chart.wormhole.dialog.collapse.confirm"),
                Translation.gameText("chart.wormhole.dialog.collapse.yes"), Translation.gameText("chart.wormhole.dialog.collapse.no"));
        options.setEnabled(COLLAPSE, run.runState() == ChartRunState.OPEN);
    }

    private void showInnerOptions(ChartRun run, OptionPanelAPI options) {
        VanillaText.plainPara(text(), Translation.gameText("chart.wormhole.dialog.inner.description"));
        boolean wayBackStaysOpen = !run.isOuterCollapsed() && run.entriesLeft() > 0;
        VanillaText.plainPara(text(), Translation.gameText(wayBackStaysOpen ? "chart.wormhole.dialog.inner.canComeBack" : "chart.wormhole.dialog.inner.lastExit"));
        options.addOption(Translation.gameText("chart.wormhole.dialog.option.return"), RETURN);
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
