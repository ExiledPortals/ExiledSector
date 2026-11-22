package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

final class DestroyFleetStep implements HarnessStep {

    private final String target;

    DestroyFleetStep(String target) {
        if (target == null || !target.startsWith(CampaignTargets.FLEET_PREFIX) || !CampaignTargets.isKnown(target)) {
            throw new IllegalArgumentException("destroyFleet needs a target of " + CampaignTargets.FLEET_PREFIX + "<fleet name>, not \"" + target + "\"");
        }
        this.target = target;
    }

    @Override
    public String name() {
        return "destroyFleet " + target;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("destroyFleet");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        SectorEntityToken entity = CampaignTargets.entity(target);
        if (!(entity instanceof CampaignFleetAPI fleet)) {
            return StepOutcome.failed(CampaignTargets.unknownTargetMessage(target));
        }
        fleet.despawn(CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE, Global.getSector().getPlayerFleet());
        return StepOutcome.passed("destroyed " + fleet.getName() + " as if in battle");
    }
}
