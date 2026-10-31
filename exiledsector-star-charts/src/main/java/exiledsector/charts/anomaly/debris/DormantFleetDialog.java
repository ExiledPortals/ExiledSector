package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingMaterials;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public class DormantFleetDialog implements InteractionDialogPlugin {

    static final String ENGAGE_OPTION = "exiledSector_debrisEngage";
    static final String CORRUPT = "exiledSector_debrisCorrupt";
    static final String LEAVE = "exiledSector_debrisLeave";
    static final String KERNEL_ID = CraftingCurrency.CORRUPTION.itemId();
    static final int KERNEL_COST = 1;

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI interactionDialog;
    private CampaignFleetAPI dormantFleet;

    public record Options(boolean canEngage, boolean canCorrupt, String corruptBlockedKey) {
    }

    public static Options options(boolean hasReadyShips, int kernelsHeld) {
        if (!hasReadyShips) {
            return new Options(false, false, "chart.debris.dialog.noReadyShips");
        }
        boolean affordable = kernelsHeld >= KERNEL_COST;
        return new Options(true, affordable, affordable ? null : "chart.debris.dialog.noKernel");
    }

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        this.interactionDialog = interactionDialog;
        this.dormantFleet = interactionDialog.getInteractionTarget() instanceof CampaignFleetAPI fleet ? fleet : null;
        if (!DormantFleets.isDormant(dormantFleet)) {
            handOff(false);
            return;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        interactionDialog.getVisualPanel().showFleetInfo(null, playerFleet, null, dormantFleet);
        VanillaText.plainPara(interactionDialog.getTextPanel(), Translation.gameText("chart.debris.dialog.intro"));
        showOptions(options(hasReadyShips(playerFleet), kernelsHeld(playerFleet)));
    }

    private void showOptions(Options choices) {
        OptionPanelAPI options = interactionDialog.getOptionPanel();
        options.clearOptions();
        options.addOption(Translation.gameText("chart.debris.dialog.engage"), ENGAGE_OPTION);
        options.setEnabled(ENGAGE_OPTION, choices.canEngage());
        String kernelName = Translation.gameText("material." + KERNEL_ID + ".name");
        options.addOption(I18n.forGameText(() -> Translation.msg("chart.debris.dialog.corrupt").arg("count", KERNEL_COST).arg("currency", kernelName)
                .text()), CORRUPT, Translation.gameText("chart.debris.dialog.corruptTooltip"));
        options.setEnabled(CORRUPT, choices.canCorrupt());
        if (choices.corruptBlockedKey() != null) {
            options.setTooltip(CORRUPT, I18n.forGameText(() -> Translation.msg(choices.corruptBlockedKey()).arg("currency", kernelName).text()));
        }
        if (!choices.canEngage()) {
            options.setTooltip(ENGAGE_OPTION, Translation.gameText("chart.debris.dialog.noReadyShips"));
        }
        options.addOption(Translation.gameText("chart.debris.dialog.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    static boolean hasReadyShips(CampaignFleetAPI playerFleet) {
        return playerFleet != null && playerFleet.getFleetData() != null && !playerFleet.getFleetData().getCombatReadyMembersListCopy().isEmpty();
    }

    static int kernelsHeld(CampaignFleetAPI playerFleet) {
        CargoAPI cargo = playerFleet == null ? null : playerFleet.getCargo();
        return cargo == null ? 0 : CraftingMaterials.forPlayer(cargo).count(KERNEL_ID);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        Options choices = options(hasReadyShips(playerFleet), kernelsHeld(playerFleet));
        if (ENGAGE_OPTION.equals(optionData) && choices.canEngage()) {
            engage(false, playerFleet);
        } else if (CORRUPT.equals(optionData) && choices.canCorrupt()) {
            engage(true, playerFleet);
        } else {
            interactionDialog.dismiss();
        }
    }

    void engage(boolean corrupt, CampaignFleetAPI playerFleet) {
        if (corrupt) {
            if (!CraftingMaterials.forPlayer(playerFleet.getCargo()).take(KERNEL_ID, KERNEL_COST)) {
                interactionDialog.dismiss();
                return;
            }
            DormantFleets.corrupt(dormantFleet, runContext());
        }
        DormantFleets.wake(dormantFleet);
        handOff(true);
    }

    private static ChartGenContext runContext() {
        ChartRun run = ChartRuns.currentOrNull();
        return run == null ? ChartGenContext.none() : ChartRunEffects.contextOf(run);
    }

    private void handOff(boolean straightToEngage) {
        FleetInteractionDialogPluginImpl encounter = new FleetInteractionDialogPluginImpl(DormantEncounters.config(straightToEngage));
        interactionDialog.setPlugin(encounter);
        encounter.init(interactionDialog);
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
