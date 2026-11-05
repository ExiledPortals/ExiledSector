package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.PlayerFleet;

public final class PlayerFleetConditionScript implements EveryFrameScript {

    static final float CHECK_SECONDS = 0.25f;

    private float secondsSinceCheck = CHECK_SECONDS;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        secondsSinceCheck += amount;
        if (secondsSinceCheck < CHECK_SECONDS) {
            return;
        }
        secondsSinceCheck = 0f;
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        FleetWideEffects.applyMoveSlowSupplyUse(playerFleet);
        FleetWideEffects.applyHalfFueledFuelUse(playerFleet);
    }
}
