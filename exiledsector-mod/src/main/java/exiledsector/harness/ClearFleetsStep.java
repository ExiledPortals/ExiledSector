package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

final class ClearFleetsStep implements HarnessStep {

    private final float radius;

    ClearFleetsStep(float radius) {
        if (radius < 0f) {
            throw new IllegalArgumentException("clearFleets needs a radius of 0 (the whole location) or more, not " + radius);
        }
        this.radius = radius;
    }

    @Override
    public String name() {
        return radius > 0f ? "clearFleets within " + Math.round(radius) : "clearFleets";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
        if (notInCampaign != null) {
            return notInCampaign;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        List<String> removed = new ArrayList<>();
        for (CampaignFleetAPI fleet : new ArrayList<>(playerFleet.getContainingLocation().getFleets())) {
            if (fleet == playerFleet || fleet.isStationMode() || (radius > 0f && Misc.getDistance(fleet, playerFleet) > radius)) {
                continue;
            }
            removed.add(fleet.getName());
            fleet.despawn(CampaignEventListener.FleetDespawnReason.OTHER, null);
        }
        return StepOutcome.passed("removed " + removed.size() + " fleets from " + playerFleet.getContainingLocation().getName()
                + (removed.isEmpty() ? "" : ": " + String.join(", ", removed)));
    }
}
