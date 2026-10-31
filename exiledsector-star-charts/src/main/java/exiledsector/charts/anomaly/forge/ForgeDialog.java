package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.GuardedSiteDialog;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.CraftingMaterials;
import exiledsector.ui.ModificationBenchCargoLauncher;
import exiledsector.ui.VanillaText;
import exiledsector.ui.socket.BenchMode;
import exiledsector.ui.socket.BenchModeSupport;

import java.util.List;

public class ForgeDialog extends GuardedSiteDialog {

    private record RunOption(ForgePrograms.Program program) {
    }

    private String ranMessage;

    @Override
    protected String descriptionKey() {
        return "chart.forge.description";
    }

    @Override
    protected String guardedKey() {
        return "chart.forge.guarded";
    }

    @Override
    protected CampaignFleetAPI musterGuards() {
        return ForgeGuards.muster(site(), liveRun());
    }

    @Override
    protected void showSite() {
        showState(true);
    }

    private ChartRun liveRun() {
        return ChartForge.liveRunFor(site());
    }

    private void showState(boolean describe) {
        TextPanelAPI text = text();
        OptionPanelAPI options = options();
        options.clearOptions();
        ChartRun run = liveRun();
        if (run == null) {
            VanillaText.plainPara(text, Translation.gameText("chart.forge.stale"));
        } else if (ForgeStates.runsLeft(run) <= 0) {
            VanillaText.plainPara(text, Translation.gameText("chart.forge.dark"));
        } else {
            if (describe) {
                describePrograms(run, text);
            }
            addProgramOptions(run, options);
        }
        addLeaveOption();
    }

    private void describePrograms(ChartRun run, TextPanelAPI text) {
        int runsLeft = ForgeStates.runsLeft(run);
        VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.forge.runsLeft").count(runsLeft).arg("count", runsLeft).text()));
        for (ForgePrograms.Program program : ForgeStates.programs(run)) {
            ForgeRules.Cost cost = ChartForge.costFor(run, program);
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.forge.programLine").arg("program", ForgeTexts.programName(program))
                    .arg("description", ForgeTexts.programDescription(program)).arg("cost", ForgeTexts.costText(cost)).text()));
        }
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        int metals = cargo == null ? 0 : Math.round(cargo.getCommodityQuantity(Commodities.METALS));
        int rareMetals = cargo == null ? 0 : Math.round(cargo.getCommodityQuantity(Commodities.RARE_METALS));
        int feedstock = cargo == null ? 0 : CraftingMaterials.forPlayer(cargo).count(ChartForge.FEEDSTOCK_ITEM_ID);
        VanillaText.addPara(text, I18n.forGameText(() -> Translation.msg("chart.forge.stock").arg("metals", metals).arg("rareMetals", rareMetals)
                .arg("feedstock", feedstock).styled()), Misc.getGrayColor());
    }

    private void addProgramOptions(ChartRun run, OptionPanelAPI options) {
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        CraftingMaterials materials = cargo == null ? null : CraftingMaterials.forPlayer(cargo);
        for (ForgePrograms.Program program : ForgeStates.programs(run)) {
            RunOption option = new RunOption(program);
            options.addOption(I18n.forGameText(() -> Translation.msg("chart.forge.option.run").arg("program", ForgeTexts.programName(program)).text()),
                    option);
            options.setEnabled(option, ChartForge.canAfford(cargo, materials, ChartForge.costFor(run, program)));
        }
    }

    private void openBench(ForgePrograms.Program program) {
        ChartRun run = liveRun();
        if (run == null) {
            showState(true);
            return;
        }
        BenchModeSupport.OpenTarget target = new BenchModeSupport.OpenTarget(new ChartForge.Order(run.runId(), program), List::of);
        boolean opened = ModificationBenchCargoLauncher.openMode(Global.getSector().getCampaignUI(), BenchMode.FORGE, target,
                message -> ranMessage = message, this::benchClosed);
        if (!opened) {
            showState(false);
        }
    }

    private void benchClosed() {
        if (ranMessage != null) {
            VanillaText.addPara(text(), StyledText.styled(ranMessage, Style.HIGHLIGHT), Misc.getTextColor());
            ranMessage = null;
            showState(true);
            return;
        }
        showState(false);
    }

    @Override
    protected void siteOptionSelected(Object optionData) {
        if (optionData instanceof RunOption option) {
            openBench(option.program());
        } else {
            dialog().dismiss();
        }
    }
}
