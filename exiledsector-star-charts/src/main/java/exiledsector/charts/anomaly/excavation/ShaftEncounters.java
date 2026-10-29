package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import exiledsector.i18n.Translation;

import java.util.function.Consumer;

public final class ShaftEncounters {

    private static CampaignFleetAPI expectedOpponent;

    private ShaftEncounters() {
    }

    public static FIDConfig config(Consumer<InteractionDialogAPI> onLeave) {
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
        config.straightToEngage = true;
        config.afterFirstTimeEngageOptionText = Translation.gameText("chart.excavation.encounter.reengage");
        config.noSalvageLeaveOptionText = Translation.gameText("chart.excavation.encounter.leave");
        config.delegate = new Delegate(onLeave);
        return config;
    }

    static void expect(CampaignFleetAPI opponent) {
        expectedOpponent = opponent;
    }

    public static boolean isShaftBattle(SectorEntityToken opponent) {
        return opponent != null && (ShaftFleets.isShaftFleet(opponent) || opponent == expectedOpponent);
    }

    static final class Delegate extends BaseFIDDelegate {

        private final Consumer<InteractionDialogAPI> onLeave;

        Delegate(Consumer<InteractionDialogAPI> onLeave) {
            this.onLeave = onLeave;
        }

        @Override
        public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext battleContext) {
            battleContext.aiRetreatAllowed = false;
            battleContext.objectivesAllowed = false;
            expect(battleContext.getOtherFleet());
        }

        @Override
        public void notifyLeave(InteractionDialogAPI dialog) {
            expect(null);
            onLeave.accept(dialog);
        }
    }
}
