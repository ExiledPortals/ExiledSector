package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;

public final class DormantEncounters {

    private DormantEncounters() {
    }

    public static FIDConfig config(boolean straightToEngage) {
        FIDConfig config = new FIDConfig();
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.impactsEnemyReputation = false;
        config.impactsAllyReputation = false;
        config.pullInEnemies = false;
        config.pullInStations = false;
        config.straightToEngage = straightToEngage;
        config.delegate = new NoRetreat();
        return config;
    }

    static final class NoRetreat extends BaseFIDDelegate {

        @Override
        public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext battleContext) {
            battleContext.aiRetreatAllowed = false;
        }
    }
}
