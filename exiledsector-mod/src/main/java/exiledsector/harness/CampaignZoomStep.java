package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;

final class CampaignZoomStep implements HarnessStep {

    static final float FARTHEST = 0f;

    private final float zoomFactor;

    CampaignZoomStep(float zoomFactor) {
        if (zoomFactor < 0f) {
            throw new IllegalArgumentException("campaignZoom needs a zoom factor above 0, or none for the farthest zoom, not " + zoomFactor);
        }
        this.zoomFactor = zoomFactor;
    }

    @Override
    public String name() {
        return zoomFactor == FARTHEST ? "campaignZoom to the farthest" : "campaignZoom to " + zoomFactor;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("campaignZoom");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        float wanted = zoomFactor == FARTHEST ? campaignUI.getMaxZoomFactor()
                : Math.max(campaignUI.getMinZoomFactor(), Math.min(campaignUI.getMaxZoomFactor(), zoomFactor));
        campaignUI.setZoomFactor(wanted);
        return StepOutcome.passed("set the campaign zoom factor to " + wanted + " (from " + campaignUI.getMinZoomFactor() + " to " + campaignUI.getMaxZoomFactor() + ")");
    }
}
