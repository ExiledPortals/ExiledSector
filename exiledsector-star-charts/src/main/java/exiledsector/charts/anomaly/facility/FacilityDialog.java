package exiledsector.charts.anomaly.facility;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.AnomalyDialog;
import exiledsector.charts.anomaly.AnomalyEncounters;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;

import java.util.Random;
import java.util.function.Consumer;

public class FacilityDialog extends AnomalyDialog {

    static final String ENGAGE = "exiledSector_facilityEngage";
    static final String SALVAGE = "exiledSector_facilitySalvage";
    static final String CONTINUE = "exiledSector_facilityContinue";
    static final long LOOT_SALT = 0x4c4f4f5446L;

    private SectorEntityToken planet;
    private FleetInteractionDialogPluginImpl encounter;

    public static boolean wantsToHandle(ChartRun run, SectorEntityToken target) {
        return target instanceof PlanetAPI planet && !planet.isStar() && FacilityStates.holds(run, planet.getId()) && FacilityStates.standing(run);
    }

    static FIDConfig config(Consumer<InteractionDialogAPI> onLeave) {
        return AnomalyEncounters.siteFight("chart.facility.encounter.leave", false, null, onLeave);
    }

    @Override
    protected void open() {
        planet = dialog().getInteractionTarget();
        showMain();
    }

    private ChartRun run() {
        return ChartRuns.liveIn(planet.getContainingLocation());
    }

    private void showMain() {
        dialog().getVisualPanel().showPlanetInfo(planet);
        TextPanelAPI text = text();
        OptionPanelAPI options = options();
        options.clearOptions();
        FacilityStates.Stage stage = FacilityStates.stage(run());
        String planetName = planet.getName();
        if (stage == FacilityStates.Stage.GUARDED) {
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.facility.dialog.guarded").arg("planet", planetName).text()));
            options.addOption(Translation.gameText("chart.facility.dialog.option.engage"), ENGAGE);
        } else if (stage == FacilityStates.Stage.OPEN) {
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.facility.dialog.open").arg("planet", planetName).text()));
            options.addOption(Translation.gameText("chart.facility.dialog.option.salvage"), SALVAGE);
        } else {
            VanillaText.plainPara(text, I18n.forGameText(() -> Translation.msg("chart.facility.dialog.salvaged").arg("planet", planetName).text()));
        }
        options.addOption(Translation.gameText("chart.facility.dialog.option.continue"), CONTINUE);
        addLeaveOption();
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        ChartRun run = run();
        FacilityStates.Stage stage = FacilityStates.stage(run);
        if (ENGAGE.equals(optionData) && stage == FacilityStates.Stage.GUARDED) {
            engage(run);
        } else if (SALVAGE.equals(optionData) && stage == FacilityStates.Stage.OPEN) {
            salvage(run);
        } else if (CONTINUE.equals(optionData)) {
            continueAfter(PreCollapseFacility.INSTANCE, planet);
        } else {
            dialog().dismiss();
        }
    }

    private void engage(ChartRun run) {
        CampaignFleetAPI guards = FacilityGuards.fleetFor(planet, run);
        if (guards == null) {
            FacilityStates.advance(run, FacilityStates.Stage.GUARDED, FacilityStates.Stage.OPEN);
            showMain();
            return;
        }
        VanillaText.plainPara(text(), Translation.gameText("chart.facility.dialog.intercepted"));
        encounter = new FleetInteractionDialogPluginImpl(config(leaving -> returnFromBattle(leaving, guards)));
        AnomalyEncounters.engage(dialog(), guards, encounter);
    }

    private void returnFromBattle(InteractionDialogAPI leaving, CampaignFleetAPI guards) {
        returnHere(leaving, planet);
        boolean won = AnomalyEncounters.wonOutright(encounter);
        encounter = null;
        if (won || guards.isEmpty()) {
            FacilityStates.advance(run(), FacilityStates.Stage.GUARDED, FacilityStates.Stage.OPEN);
            FacilityGuards.forget(planet);
        }
        showMain();
    }

    private void salvage(ChartRun run) {
        if (!FacilityStates.advance(run, FacilityStates.Stage.OPEN, FacilityStates.Stage.SALVAGED)) {
            showMain();
            return;
        }
        CargoAPI loot = FacilityLoot.roll(Global.getSector(), run, new Random(PreCollapseFacility.seed(run, planet.getId(), LOOT_SALT)));
        VanillaText.plainPara(text(), Translation.gameText("chart.facility.dialog.salvaging"));
        dialog().getVisualPanel().showLoot(Translation.gameText("chart.facility.dialog.lootTitle"), loot, false, true, true, new CoreInteractionListener() {
            @Override
            public void coreUIDismissed() {
                showMain();
            }
        });
    }
}
