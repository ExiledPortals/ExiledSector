package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyDialog;
import exiledsector.charts.anomaly.AnomalyEncounters;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingMaterials;
import exiledsector.ui.VanillaText;

public class DormantFleetDialog extends AnomalyDialog {

    static final String ENGAGE_OPTION = "exiledSector_debrisEngage";
    static final String CORRUPT = "exiledSector_debrisCorrupt";
    static final String KERNEL_ID = CraftingCurrency.CORRUPTION.itemId();
    static final int KERNEL_COST = 1;

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
    protected void open() {
        dormantFleet = dialog().getInteractionTarget() instanceof CampaignFleetAPI fleet ? fleet : null;
        if (!DormantFleets.isDormant(dormantFleet)) {
            fight(false);
            return;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        dialog().getVisualPanel().showFleetInfo(null, playerFleet, null, dormantFleet);
        VanillaText.plainPara(text(), Translation.gameText("chart.debris.dialog.intro"));
        showOptions(options(hasReadyShips(playerFleet), kernelsHeld(playerFleet)));
    }

    private void showOptions(Options choices) {
        OptionPanelAPI options = options();
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
        addLeaveOption(Translation.gameText("chart.debris.dialog.leave"), LEAVE);
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
            dialog().dismiss();
        }
    }

    void engage(boolean corrupt, CampaignFleetAPI playerFleet) {
        if (corrupt) {
            if (!CraftingMaterials.forPlayer(playerFleet.getCargo()).take(KERNEL_ID, KERNEL_COST)) {
                dialog().dismiss();
                return;
            }
            DormantFleets.corrupt(dormantFleet, runContext());
        }
        DormantFleets.wake(dormantFleet);
        fight(true);
    }

    private ChartGenContext runContext() {
        ChartRun run = ChartRuns.liveIn(dormantFleet.getContainingLocation());
        return run == null ? ChartGenContext.none() : ChartRunEffects.contextOf(run);
    }

    private void fight(boolean straightToEngage) {
        handOff(new FleetInteractionDialogPluginImpl(AnomalyEncounters.isolated(straightToEngage, null, null)));
    }
}
