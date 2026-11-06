package exiledsector.loot;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

public final class BattleLootTrigger extends BaseCampaignEventListener {

    public BattleLootTrigger() {
        super(false);
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (battle != null && battle.isPlayerInvolved()) {
            LootTaken.report();
        }
    }
}
