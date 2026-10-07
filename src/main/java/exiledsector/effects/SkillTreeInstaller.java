package exiledsector.effects;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.skills.skilleffect.FleetWideEffects;

public class SkillTreeInstaller implements EveryFrameScript {

    private boolean syncedSinceLoad;

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
        if (ShipTreeSync.takePlayerFleetSyncRequest() || !syncedSinceLoad) {
            syncPlayerFleet();
        }
        FleetWideEffects.recomputeExtendedPhaseFieldIfStale();
    }

    private void syncPlayerFleet() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;

        if (!ShipTreeSync.fleetChanged(playerFleet) && !syncedSinceLoad) {
            playerFleet.getFleetData().setSyncNeeded();
        }
        syncedSinceLoad = true;
    }
}
