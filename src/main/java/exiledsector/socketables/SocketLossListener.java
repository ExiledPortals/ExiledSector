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
    public void reportPlayerEngagement(EngagementResultAPI result) {
        if (result == null) {
            return;
        }
        EngagementResultForFleetAPI player = result.didPlayerWin() ? result.getWinnerResult() : result.getLoserResult();
        if (player == null) {
            return;
        }
        SocketCustody.recordLostInCombat(player.getDestroyed());
        SocketCustody.recordLostInCombat(player.getDisabled());
    }

    @Override
    public void reportShipsRecovered(List<FleetMemberAPI> ships, InteractionDialogAPI dialog) {
        if (ships != null) {
            SocketCustody.recordRecovered(ships);
        }
    }
}
