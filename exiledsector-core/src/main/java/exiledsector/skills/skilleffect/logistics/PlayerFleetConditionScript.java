package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;

public final class PlayerFleetConditionScript implements EveryFrameScript {

    static final float CHECK_SECONDS = 0.25f;

    private static boolean playerFleetSynced = true;

    private float secondsSinceCheck = CHECK_SECONDS;
    private CampaignFleetAPI conditionedFleet;
    private boolean conditionedSlowMoving;
    private boolean conditionedHalfFueled;

    public static void markPlayerFleetSynced() {
        playerFleetSynced = true;
    }

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
        if (playerFleet == null) {
            return;
        }
        boolean slowMoving = Misc.isSlowMoving(playerFleet);
        boolean halfFueled = FleetWideEffects.isHalfFueled(playerFleet);
        if (!playerFleetSynced && playerFleet == conditionedFleet && slowMoving == conditionedSlowMoving
                && halfFueled == conditionedHalfFueled) {
            return;
        }
        playerFleetSynced = false;
        conditionedFleet = playerFleet;
        conditionedSlowMoving = slowMoving;
        conditionedHalfFueled = halfFueled;
        FleetWideEffects.applyPlayerFleetConditions(playerFleet, slowMoving, halfFueled);
    }
}
