package exiledsector.effects;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.skills.skilleffect.FleetWideEffects;

public class SkillTreeInstaller implements EveryFrameScript {

    static final float MIN_SECONDS_AFTER_A_CHANGING_PASS = 1f;

    private boolean syncedSinceLoad;
    private boolean passPending;
    private float secondsSinceChangingPass = MIN_SECONDS_AFTER_A_CHANGING_PASS;

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
        secondsSinceChangingPass += amount;
        passPending |= ShipTreeSync.takePlayerFleetSyncRequest();
        if (!syncedSinceLoad || passPending && secondsSinceChangingPass >= MIN_SECONDS_AFTER_A_CHANGING_PASS) {
            syncPlayerFleet();
        }
        if (PhantomConflictWatch.hasPendingReverts()) {
            PhantomConflictWatch.applyPendingReverts();
        }
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();
    }

    private void syncPlayerFleet() {
        passPending = false;
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;

        if (ShipTreeSync.fleetChanged(playerFleet)) {
            secondsSinceChangingPass = 0f;
        } else if (!syncedSinceLoad) {
            playerFleet.getFleetData().setSyncNeeded();
        }
        syncedSinceLoad = true;
    }
}
