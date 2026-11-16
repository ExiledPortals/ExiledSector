package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import exiledsector.PlayerFleet;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyDialog;
import exiledsector.charts.anomaly.AnomalyEncounters;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRunEffects;
import exiledsector.charts.run.ChartRuns;
import exiledsector.items.Nanoswarm;
import exiledsector.items.CraftingMaterials;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;

public class DormantFleetDialog extends AnomalyDialog {

    static final String ENGAGE_OPTION = "exiledSector_debrisEngage";
    static final String CORRUPT = "exiledSector_debrisCorrupt";
    static final String KERNEL_ID = Nanoswarm.CORRUPTION.itemId();
    static final int KERNEL_COST = 1;

    private CampaignFleetAPI dormantFleet;

    public record CorruptOption(int times, boolean enabled, String blockedKey) {

        public int cost() {
            return times * KERNEL_COST;
        }

        String optionId() {
            return times == 1 ? CORRUPT : CORRUPT + times;
        }
    }

    public record Options(boolean canEngage, List<CorruptOption> corruptions) {

        public CorruptOption corruption(int times) {
            return corruptions.stream().filter(option -> option.times() == times).findFirst().orElse(null);
        }
    }

    public static Options options(boolean hasReadyShips, int kernelsHeld) {
        return options(hasReadyShips, kernelsHeld, 1);
    }

    public static Options options(boolean hasReadyShips, int kernelsHeld, int corruptionsLeft) {
        List<CorruptOption> corruptions = new ArrayList<>();
        for (int times = 1; times <= corruptionsLeft; times++) {
            boolean affordable = kernelsHeld >= times * KERNEL_COST;
            String blockedKey = !hasReadyShips ? "chart.anomaly.anomalous_debris.dialog.noReadyShips"
                    : affordable ? null : missingKernelsKey(times);
            corruptions.add(new CorruptOption(times, hasReadyShips && affordable, blockedKey));
        }
        return new Options(hasReadyShips, List.copyOf(corruptions));
    }

    private static String missingKernelsKey(int times) {
        return times == 1 ? "chart.anomaly.anomalous_debris.dialog.noKernel" : "chart.anomaly.anomalous_debris.dialog.notEnoughKernels";
    }

    @Override
    protected void open() {
        dormantFleet = dialog().getInteractionTarget() instanceof CampaignFleetAPI fleet ? fleet : null;
        if (!DormantFleets.isDormant(dormantFleet)) {
            fight(false);
            return;
        }
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        dialog().getVisualPanel().showFleetInfo(null, playerFleet, null, dormantFleet);
        VanillaText.plainPara(text(), Translation.gameText("chart.anomaly.anomalous_debris.dialog.intro"));
        showOptions(currentOptions(playerFleet));
    }

    private Options currentOptions(CampaignFleetAPI playerFleet) {
        return options(hasReadyShips(playerFleet), kernelsHeld(playerFleet), DormantFleets.corruptionsLeft(dormantFleet, runContext()));
    }

    private void showOptions(Options choices) {
        OptionPanelAPI options = options();
        options.clearOptions();
        options.addOption(Translation.gameText("chart.anomaly.anomalous_debris.dialog.engage"), ENGAGE_OPTION);
        options.setEnabled(ENGAGE_OPTION, choices.canEngage());
        String kernelName = Translation.gameText("material." + KERNEL_ID + ".name");
        for (CorruptOption corruption : choices.corruptions()) {
            String optionKey = corruption.times() == 1 ? "chart.anomaly.anomalous_debris.dialog.corrupt" : "chart.anomaly.anomalous_debris.dialog.corruptTwice";
            String tooltipKey = corruption.times() == 1 ? "chart.anomaly.anomalous_debris.dialog.corruptTooltip"
                    : "chart.anomaly.anomalous_debris.dialog.corruptTwiceTooltip";
            options.addOption(I18n.forGameText(() -> Translation.msg(optionKey).arg("count", corruption.cost()).arg("craftingItem", kernelName).text()),
                    corruption.optionId(), Translation.gameText(tooltipKey));
            options.setEnabled(corruption.optionId(), corruption.enabled());
            if (corruption.blockedKey() != null) {
                options.setTooltip(corruption.optionId(), I18n.forGameText(() -> Translation.msg(corruption.blockedKey()).arg("count", corruption.cost())
                        .arg("craftingItem", kernelName).text()));
            }
        }
        if (!choices.canEngage()) {
            options.setTooltip(ENGAGE_OPTION, Translation.gameText("chart.anomaly.anomalous_debris.dialog.noReadyShips"));
        }
        addLeaveOption(Translation.gameText("chart.anomaly.anomalous_debris.dialog.leave"), LEAVE);
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
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (playerFleet == null) {
            dialog().dismiss();
            return;
        }
        Options choices = currentOptions(playerFleet);
        CorruptOption chosenCorruption = choices.corruptions().stream().filter(option -> option.optionId().equals(optionData)).findFirst().orElse(null);
        if (ENGAGE_OPTION.equals(optionData) && choices.canEngage()) {
            engage(null, playerFleet);
        } else if (chosenCorruption != null && chosenCorruption.enabled()) {
            engage(chosenCorruption, playerFleet);
        } else {
            dialog().dismiss();
        }
    }

    void engage(CorruptOption corruption, CampaignFleetAPI playerFleet) {
        ChartGenContext context = runContext();
        if (corruption != null) {
            if (!CraftingMaterials.forPlayer(playerFleet.getCargo()).take(KERNEL_ID, corruption.cost())) {
                dialog().dismiss();
                return;
            }
            DormantFleets.corrupt(dormantFleet, context, corruption.times());
        }
        DormantFleets.wake(dormantFleet, DebrisTuning.of(context).roaming());
        fight(true);
    }

    private ChartGenContext runContext() {
        ChartRun run = dormantFleet == null ? null : ChartRuns.liveIn(dormantFleet.getContainingLocation());
        return ChartRunEffects.contextOf(run);
    }

    private void fight(boolean straightToEngage) {
        handOff(new FleetInteractionDialogPluginImpl(AnomalyEncounters.isolated(straightToEngage, null, null)));
    }
}
