package exiledsector.charts.run;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.RunFactionDress;

import java.util.EnumSet;
import java.util.Set;

public final class ChartAllyReputation implements ReputationActionResponsePlugin {

    static final Set<RepActions> ALLY_COMBAT_ACTIONS = EnumSet.of(RepActions.COMBAT_HELP_MINOR, RepActions.COMBAT_HELP_MAJOR,
            RepActions.COMBAT_HELP_CRITICAL, RepActions.COMBAT_FRIENDLY_FIRE);

    static boolean ignores(Object action, String factionId) {
        return RunFactionDress.ALLY_FACTION_ID.equals(factionId) || ALLY_COMBAT_ACTIONS.contains(actionOf(action)) && playerInChart();
    }

    private static RepActions actionOf(Object action) {
        if (action instanceof RepActionEnvelope envelope) {
            return envelope.action;
        }
        return action instanceof RepActions repAction ? repAction : null;
    }

    static boolean playerInChart() {
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        LocationAPI location = playerFleet == null ? null : playerFleet.getContainingLocation();
        return location != null && location.hasTag(ChartIds.SYSTEM_TAG);
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
