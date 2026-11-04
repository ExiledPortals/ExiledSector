package exiledsector.skills.skilleffect.logistics;

import com.fs.starfarer.api.EveryFrameScript;
import exiledsector.PlayerFleet;

public final class MoveSlowSupplyScript implements EveryFrameScript {

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
        FleetWideEffects.applyMoveSlowSupplyUse(PlayerFleet.orNull());
    }
}
