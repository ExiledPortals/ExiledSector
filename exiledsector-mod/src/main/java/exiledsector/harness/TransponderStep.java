package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

final class TransponderStep implements HarnessStep {

    private final boolean on;

    TransponderStep(boolean on) {
        this.on = on;
    }

    @Override
    public String name() {
        return "transponder " + (on ? "on" : "off");
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("transponder");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        playerFleet.setTransponderOn(on);
        return StepOutcome.passed("turned the player fleet's transponder " + (on ? "on" : "off"));
    }
}
