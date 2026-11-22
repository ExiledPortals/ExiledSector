package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

final class VisitNearestStep implements HarnessStep {

    private final String target;
    private String visitedName;

    VisitNearestStep(String target) {
        if (!CampaignTargets.isKnown(target)) {
            throw new IllegalArgumentException("visitNearest needs a custom entity type, such as \"station_research\", or a target: "
                    + CampaignTargets.DESCRIPTION + ", not \"" + target + "\"");
        }
        this.target = target.trim();
    }

    static VisitNearestStep ofType(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("visitNearest needs a custom entity type, such as \"station_research\"");
        }
        return new VisitNearestStep(CampaignTargets.ENTITY_PREFIX + entityType.trim());
    }

    @Override
    public String name() {
        return "visitNearest " + target;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("visitNearest");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (context.firstFrame()) {
            SectorEntityToken nearest = CampaignTargets.entity(target);
            if (nearest == null) {
                return StepOutcome.failed(CampaignTargets.unknownTargetMessage(target));
            }
            visitedName = nearest.getName();
            campaignUI.showInteractionDialog(nearest);
            return StepOutcome.RUNNING;
        }
        if (campaignUI.getCurrentInteractionDialog() != null) {
            return StepOutcome.passed("opened the interaction dialog with " + visitedName);
        }
        return new StepOutcome(false, false, "the interaction dialog with " + visitedName + " never opened");
    }
}
