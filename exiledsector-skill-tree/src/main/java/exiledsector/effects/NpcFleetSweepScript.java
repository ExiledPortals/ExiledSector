package exiledsector.effects;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import exiledsector.PlayerFleet;
import org.lwjgl.util.vector.Vector2f;

import java.util.function.Consumer;

public class NpcFleetSweepScript implements EveryFrameScript {

    static final float CHECK_INTERVAL_SECONDS = 1f;
    static final float SWEEP_RANGE = 4000f;

    private float timeSinceLastCheck = CHECK_INTERVAL_SECONDS;
    private final SweptFleetMemberships sweptMemberships = new SweptFleetMemberships();

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

        forEachFleetAround(PlayerFleet.orNull(), SWEEP_RANGE, this::ensureIfMembershipChanged);
    }

    static void sweepAround(CampaignFleetAPI playerFleet, float sweepRange) {
        forEachFleetAround(playerFleet, sweepRange, NpcFleetLeveller::ensure);
    }

    private void ensureIfMembershipChanged(CampaignFleetAPI nearbyFleet) {
        if (!NpcFleetLeveller.appliesTo(nearbyFleet)) {
            sweptMemberships.forget(nearbyFleet);
            return;
        }
        if (sweptMemberships.changedSinceRemembered(nearbyFleet)) {
            NpcFleetLeveller.ensure(nearbyFleet);
            sweptMemberships.remember(nearbyFleet);
        }
    }

    private static void forEachFleetAround(CampaignFleetAPI playerFleet, float sweepRange, Consumer<CampaignFleetAPI> fleetAction) {
        if (playerFleet == null) return;
        LocationAPI location = playerFleet.getContainingLocation();
        if (location == null) return;

        Vector2f origin = playerFleet.getLocation();
        for (CampaignFleetAPI nearbyFleet : location.getFleets()) {
            if (nearbyFleet == playerFleet) {
                continue;
            }
            if (isWithin(origin, nearbyFleet.getLocation(), sweepRange)) {
                fleetAction.accept(nearbyFleet);
            }
        }
    }

    private static boolean isWithin(Vector2f origin, Vector2f fleetLocation, float range) {
        if (origin == null || fleetLocation == null) return false;
        float dx = fleetLocation.x - origin.x;
        float dy = fleetLocation.y - origin.y;
        return dx * dx + dy * dy <= range * range;
    }
}
