package exiledsector.effects;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import org.lwjgl.util.vector.Vector2f;

public class NpcFleetSweepScript implements EveryFrameScript {

    static final float CHECK_INTERVAL_SECONDS = 1f;
    static final float SWEEP_RANGE = 4000f;

    private float timeSinceLastCheck = CHECK_INTERVAL_SECONDS;

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
        timeSinceLastCheck += amount;
        if (timeSinceLastCheck < CHECK_INTERVAL_SECONDS) return;
        timeSinceLastCheck = 0f;

        sweepAround(Global.getSector().getPlayerFleet(), SWEEP_RANGE);
    }

    static void sweepAround(CampaignFleetAPI playerFleet, float range) {
        if (playerFleet == null) return;
        LocationAPI location = playerFleet.getContainingLocation();
        if (location == null) return;

        Vector2f origin = playerFleet.getLocation();
        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (fleet == playerFleet) {
                continue;
            }
            if (isWithin(origin, fleet.getLocation(), range)) {
                NpcFleetLeveller.ensure(fleet);
            }
            NpcUniqueAlerts.alertIfSensed(fleet);
        }
    }

    private static boolean isWithin(Vector2f origin, Vector2f target, float range) {
        if (origin == null || target == null) return false;
        float dx = target.x - origin.x;
        float dy = target.y - origin.y;
        return dx * dx + dy * dy <= range * range;
    }
}
