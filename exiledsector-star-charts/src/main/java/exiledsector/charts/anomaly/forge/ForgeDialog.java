package exiledsector.charts.anomaly.forge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageDefenderInteraction;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.charts.run.ChartRun;
import exiledsector.items.CraftingMaterials;
import exiledsector.ui.ModificationBenchCargoLauncher;
import exiledsector.ui.VanillaText;
import exiledsector.ui.socket.BenchMode;
import exiledsector.ui.socket.BenchModeSupport;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeDialog implements InteractionDialogPlugin {

    static final String LEAVE = "exiledSector_forgeLeave";
    static final String BEAT_DEFENDERS_CONTINUE = "salBeatDefendersContinue";

    private record RunOption(ForgePrograms.Program program) {
    }

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI interactionDialog;
    private SectorEntityToken forge;
    private String ranMessage;

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        this.interactionDialog = interactionDialog;
        this.forge = interactionDialog.getInteractionTarget();
        if (forge != null && forge.getCustomInteractionDialogImageVisual() != null) {
            interactionDialog.getVisualPanel().showImageVisual(forge.getCustomInteractionDialogImageVisual());
        }
        VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText("chart.forge.description"));
        if (!engageGuards()) {
            showState(true);
        }
    }

    private boolean engageGuards() {
        CampaignFleetAPI guards = ForgeGuards.muster(forge, liveRun());
        if (guards == null) {
            return false;
        }
        fillMemoryMap();
        VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText("chart.forge.guarded"));
        return new SalvageDefenderInteraction().execute(null, interactionDialog, new ArrayList<>(), memoryMap);
    }

    private void fillMemoryMap() {
        SectorAPI sector = Global.getSector();
        memoryMap.put(MemKeys.LOCAL, forge.getMemoryWithoutUpdate());
        FactionAPI faction = forge.getFaction();
        memoryMap.put(MemKeys.FACTION, faction != null ? faction.getMemoryWithoutUpdate() : Global.getFactory().createMemory());
        memoryMap.put(MemKeys.GLOBAL, sector.getMemoryWithoutUpdate());
        memoryMap.put(MemKeys.PLAYER, sector.getCharacterData().getMemoryWithoutUpdate());
    }

    private void continueAfterGuards() {
        if (forge.getCustomInteractionDialogImageVisual() != null) {
            interactionDialog.getVisualPanel().showImageVisual(forge.getCustomInteractionDialogImageVisual());
        }
        showState(true);
    }

    private ChartRun liveRun() {
        return ChartForge.liveRunFor(forge);
    }

    private void showState(boolean describe) {
        TextPanelAPI text = interactionDialog.getTextPanel();
        OptionPanelAPI options = interactionDialog.getOptionPanel();
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
        options.addOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    private void describePrograms(ChartRun run, TextPanelAPI text) {
        int runsLeft = ForgeStates.runsLeft(run);
        VanillaText.plainPara(text, Translation.msg("chart.forge.runsLeft").count(runsLeft).arg("count", runsLeft).text());
        for (ForgePrograms.Program program : ForgeStates.programs(run)) {
            VanillaText.plainPara(text, Translation.msg("chart.forge.programLine").arg("program", ForgeTexts.programName(program))
                    .arg("description", ForgeTexts.programDescription(program)).arg("cost", ForgeTexts.costText(ChartForge.costFor(run, program))).text());
        }
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        int metals = cargo == null ? 0 : Math.round(cargo.getCommodityQuantity(Commodities.METALS));
        int rareMetals = cargo == null ? 0 : Math.round(cargo.getCommodityQuantity(Commodities.RARE_METALS));
        int feedstock = cargo == null ? 0 : CraftingMaterials.forPlayer(cargo).count(ChartForge.FEEDSTOCK_ITEM_ID);
        VanillaText.addPara(text, Translation.msg("chart.forge.stock").arg("metals", metals).arg("rareMetals", rareMetals).arg("feedstock", feedstock)
                .styled(), Misc.getGrayColor());
    }

    private void addProgramOptions(ChartRun run, OptionPanelAPI options) {
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        CraftingMaterials materials = cargo == null ? null : CraftingMaterials.forPlayer(cargo);
        for (ForgePrograms.Program program : ForgeStates.programs(run)) {
            RunOption option = new RunOption(program);
            options.addOption(Translation.msg("chart.forge.option.run").arg("program", ForgeTexts.programName(program)).text(), option);
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
            VanillaText.addPara(interactionDialog.getTextPanel(), StyledText.styled(ranMessage, Style.HIGHLIGHT), Misc.getTextColor());
            ranMessage = null;
            showState(true);
            return;
        }
        showState(false);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (BEAT_DEFENDERS_CONTINUE.equals(optionData)) {
            continueAfterGuards();
        } else if (optionData instanceof RunOption option) {
            openBench(option.program());
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
