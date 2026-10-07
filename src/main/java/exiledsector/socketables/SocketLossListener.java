package exiledsector.socketables;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.ShipRecoveryListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.List;

public class SocketLossListener extends BaseCampaignEventListener implements ShipRecoveryListener {

    public SocketLossListener() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI engagementResult) {
        if (engagementResult == null) {
            return;
        }
        EngagementResultForFleetAPI playerResult = engagementResult.didPlayerWin() ? engagementResult.getWinnerResult() : engagementResult.getLoserResult();
        if (playerResult == null) {
            return;
        }
        SocketCustody.recordLostInCombat(playerResult.getDestroyed());
        SocketCustody.recordLostInCombat(playerResult.getDisabled());
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> recoveredShips, InteractionDialogAPI dialog) {
        if (recoveredShips != null) {
            SocketCustody.recordRecovered(recoveredShips);
        }
    }
}
