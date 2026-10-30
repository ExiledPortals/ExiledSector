package exiledsector.charts.anomaly.facility;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import exiledsector.ui.VanillaText;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

public class FacilityDialog implements InteractionDialogPlugin {

    static final String ENGAGE = "exiledSector_facilityEngage";
    static final String SALVAGE = "exiledSector_facilitySalvage";
    static final String CONTINUE = "exiledSector_facilityContinue";
    static final String LEAVE = "exiledSector_facilityLeave";
    static final long LOOT_SALT = 0x4c4f4f5446L;

    private final Map<String, MemoryAPI> memoryMap = new HashMap<>();
    private InteractionDialogAPI dialog;
    private SectorEntityToken planet;
    private FleetInteractionDialogPluginImpl encounter;

    public static boolean wantsToHandle(SectorEntityToken target) {
        if (!(target instanceof PlanetAPI planet) || planet.isStar()) {
            return false;
        }
        ChartRun run = ChartRuns.currentOrNull();
        return PreCollapseFacility.inRunSystem(run, planet.getContainingLocation()) && FacilityStates.holds(run, planet.getId())
                && FacilityStates.standing(run);
    }

    static FIDConfig config(Consumer<InteractionDialogAPI> onLeave) {
        FIDConfig config = new FIDConfig();
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.showCommLinkOption = false;
        config.showFleetAttitude = false;
        config.alwaysAttackVsAttack = true;
        config.leaveAlwaysAvailable = true;
        config.impactsEnemyReputation = false;
        config.impactsAllyReputation = false;
        config.pullInAllies = false;
        config.pullInEnemies = false;
        config.pullInStations = false;
        config.dismissOnLeave = false;
        config.printXPToDialog = true;
        config.noSalvageLeaveOptionText = Translation.gameText("chart.facility.encounter.leave");
        config.delegate = new BaseFIDDelegate() {
            @Override
            public void battleContextCreated(InteractionDialogAPI interactionDialog, BattleCreationContext battleContext) {
                battleContext.aiRetreatAllowed = false;
            }

            @Override
            public void notifyLeave(InteractionDialogAPI interactionDialog) {
                onLeave.accept(interactionDialog);
            }
        };
        return config;
    }

    @Override
    public void init(InteractionDialogAPI interactionDialog) {
        dialog = interactionDialog;
        planet = interactionDialog.getInteractionTarget();
        showMain();
    }

    private void showMain() {
        dialog.getVisualPanel().showPlanetInfo(planet);
        TextPanelAPI text = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();
        FacilityStates.Stage stage = FacilityStates.stage(ChartRuns.currentOrNull());
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
        options.addOption(Translation.gameText("chart.dialog.option.leave"), LEAVE);
        options.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, true);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        ChartRun run = ChartRuns.currentOrNull();
        FacilityStates.Stage stage = FacilityStates.stage(run);
        if (ENGAGE.equals(optionData) && stage == FacilityStates.Stage.GUARDED) {
            engage(run);
        } else if (SALVAGE.equals(optionData) && stage == FacilityStates.Stage.OPEN) {
            salvage(run);
        } else if (CONTINUE.equals(optionData)) {
            InteractionDialogPlugin next = ChartAnomalies.dialogFor(planet, PreCollapseFacility.INSTANCE);
            InteractionDialogPlugin planetDialog = next == null ? new RuleBasedInteractionDialogPluginImpl() : next;
            dialog.setPlugin(planetDialog);
            planetDialog.init(dialog);
        } else {
            dialog.dismiss();
        }
    }

    private void engage(ChartRun run) {
        CampaignFleetAPI guards = FacilityGuards.fleetFor(planet, run);
        if (guards == null) {
            FacilityStates.advance(run, FacilityStates.Stage.GUARDED, FacilityStates.Stage.OPEN);
            showMain();
            return;
        }
        VanillaText.plainPara(dialog.getTextPanel(), Translation.gameText("chart.facility.dialog.intercepted"));
        encounter = new FleetInteractionDialogPluginImpl(config(leaving -> returnFromBattle(leaving, guards)));
        dialog.setInteractionTarget(guards);
        dialog.setPlugin(encounter);
        encounter.init(dialog);
    }

    private void returnFromBattle(InteractionDialogAPI leaving, CampaignFleetAPI guards) {
        leaving.setPlugin(this);
        leaving.setInteractionTarget(planet);
        boolean won = encounter != null && encounter.getContext() instanceof FleetEncounterContext context && context.didPlayerWinEncounterOutright();
        encounter = null;
        if (won || guards.isEmpty()) {
            FacilityStates.advance(ChartRuns.currentOrNull(), FacilityStates.Stage.GUARDED, FacilityStates.Stage.OPEN);
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
        VanillaText.plainPara(dialog.getTextPanel(), Translation.gameText("chart.facility.dialog.salvaging"));
        dialog.getVisualPanel().showLoot(Translation.gameText("chart.facility.dialog.lootTitle"), loot, false, true, true, new CoreInteractionListener() {
            @Override
            public void coreUIDismissed() {
                showMain();
            }
        });
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
