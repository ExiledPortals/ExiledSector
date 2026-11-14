package exiledsector.charts.run;

import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import exiledsector.charts.anomaly.RunFactionDress;

public final class RunFactionReputation implements ReputationActionResponsePlugin {

    static boolean ignores(String factionId) {
        return RunFactionDress.SKIRMISH_FACTION_IDS.contains(factionId);
    }

    @Override
    public ReputationAdjustmentResult handlePlayerReputationAction(Object action, String factionId) {
        return new ReputationAdjustmentResult(0f);
    }

    @Override
    public ReputationAdjustmentResult handlePlayerReputationAction(Object action, PersonAPI person) {
        return new ReputationAdjustmentResult(0f);
    }
}
