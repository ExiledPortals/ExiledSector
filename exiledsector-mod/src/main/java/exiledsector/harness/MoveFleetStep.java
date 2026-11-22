package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

final class MoveFleetStep implements HarnessStep {

    static final float DEFAULT_ARRIVAL_DISTANCE = 150f;
    static final float DEFAULT_FLIGHT_SECONDS = 60f;

    private final String target;
    private final boolean teleport;
    private final float distance;
    private final boolean slow;
    private final float flightSeconds;
    private final boolean mustArrive;

    MoveFleetStep(String target, boolean teleport, float distance, boolean slow, float flightSeconds, boolean mustArrive) {
        if (!CampaignTargets.isKnown(target)) {
            throw new IllegalArgumentException("moveFleet needs a target: " + CampaignTargets.DESCRIPTION + ", not \"" + target + "\"");
        }
        if (distance < 0f || flightSeconds <= 0f) {
            throw new IllegalArgumentException("moveFleet needs a distance of 0 or more and seconds above 0, not " + distance + " and " + flightSeconds);
        }
        this.target = target;
        this.teleport = teleport;
        this.distance = distance;
        this.slow = slow;
        this.flightSeconds = flightSeconds;
        this.mustArrive = mustArrive;
    }

    @Override
    public String name() {
        return (teleport ? "moveFleet to " : "moveFleet flying to ") + target + (teleport || mustArrive ? "" : " for " + flightSeconds + " s");
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("moveFleet");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        Vector2f destination = CampaignTargets.worldPoint(target);
        if (destination == null) {
            return StepOutcome.failed(CampaignTargets.unknownTargetMessage(target));
        }
        if (teleport) {
            SectorEntityToken entity = CampaignTargets.entity(target);
            float gap = entity == null || target.startsWith(CampaignTargets.PICKET_GAP) ? distance : entity.getRadius() + distance;
            HarnessCampaign.movePlayerFleetTo(sector, playerFleet.getContainingLocation(), destination.x + gap, destination.y);
            playerFleet.setMoveDestination(playerFleet.getLocation().x, playerFleet.getLocation().y);
            return StepOutcome.passed("moved the player fleet " + Math.round(gap) + " from " + target);
        }
        float left = Misc.getDistance(playerFleet.getLocation(), destination);
        if (left <= Math.max(distance, 1f)) {
            playerFleet.setMoveDestination(playerFleet.getLocation().x, playerFleet.getLocation().y);
            return StepOutcome.passed("flew the player fleet to within " + Math.round(left) + " of " + target + " in " + Math.round(context.seconds()) + " seconds");
        }
        if (context.seconds() > flightSeconds && !mustArrive) {
            return StepOutcome.passed("flew the player fleet toward " + target + " for " + Math.round(flightSeconds) + " seconds, ending " + Math.round(left) + " from it");
        }
        if (context.seconds() > flightSeconds) {
            return StepOutcome.failed("the player fleet was still " + Math.round(left) + " from " + target + " after " + Math.round(flightSeconds) + " seconds");
        }
        HarnessCampaign.unpause(sector);
        playerFleet.setMoveDestination(destination.x, destination.y);
        if (slow) {
            playerFleet.goSlowOneFrame();
        }
        return new StepOutcome(false, false, Math.round(left) + " left to " + target);
    }

    @Override
    public float timeoutSeconds() {
        return flightSeconds + DEFAULT_TIMEOUT_SECONDS;
    }
}
