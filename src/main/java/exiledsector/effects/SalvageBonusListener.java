package exiledsector.effects;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import exiledsector.skills.skilleffect.FleetWideEffects;

public class SalvageBonusListener extends BaseCampaignEventListener {

    public SalvageBonusListener() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI engagementResult) {
        FleetWideEffects.recomputeSalvageBonus();
    }
}
