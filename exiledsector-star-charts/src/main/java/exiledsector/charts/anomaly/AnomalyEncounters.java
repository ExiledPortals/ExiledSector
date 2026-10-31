package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import exiledsector.i18n.Translation;

import java.util.function.Consumer;

public final class AnomalyEncounters {

    private AnomalyEncounters() {
    }

    public static FIDConfig isolated(boolean straightToEngage, Consumer<BattleCreationContext> battle, Consumer<InteractionDialogAPI> onLeave) {
        FIDConfig config = new FIDConfig();
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.impactsEnemyReputation = false;
        config.impactsAllyReputation = false;
        config.pullInEnemies = false;
        config.pullInStations = false;
        config.straightToEngage = straightToEngage;
        config.delegate = new BaseFIDDelegate() {
            @Override
            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext battleContext) {
                battleContext.aiRetreatAllowed = false;
                if (battle != null) {
                    battle.accept(battleContext);
                }
            }

            @Override
            public void notifyLeave(InteractionDialogAPI dialog) {
                if (onLeave != null) {
                    onLeave.accept(dialog);
                }
            }
        };
        return config;
    }

    public static FIDConfig siteFight(String leaveTextKey, boolean straightToEngage, Consumer<BattleCreationContext> battle,
                                      Consumer<InteractionDialogAPI> onLeave) {
        FIDConfig config = isolated(straightToEngage, battle, onLeave);
        config.showCommLinkOption = false;
        config.showFleetAttitude = false;
        config.alwaysAttackVsAttack = true;
        config.leaveAlwaysAvailable = true;
        config.pullInAllies = false;
        config.dismissOnLeave = false;
        config.printXPToDialog = true;
        config.noSalvageLeaveOptionText = Translation.gameText(leaveTextKey);
        return config;
    }

    public static void engage(InteractionDialogAPI dialog, CampaignFleetAPI opponent, FleetInteractionDialogPluginImpl encounter) {
        dialog.setInteractionTarget(opponent);
        dialog.setPlugin(encounter);
        encounter.init(dialog);
    }

    public static boolean wonOutright(FleetInteractionDialogPluginImpl encounter) {
        return encounter != null && encounter.getContext() instanceof FleetEncounterContext context && context.didPlayerWinEncounterOutright();
    }
}
