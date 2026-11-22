package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.util.Misc;

import java.util.Comparator;

final class VisitNearestStep implements HarnessStep {

    private final String entityType;
    private String visitedName;

    VisitNearestStep(String entityType) {
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("visitNearest needs a custom entity type, such as \"station_research\"");
        }
        this.entityType = entityType.trim();
    }

    @Override
    public String name() {
        return "visitNearest " + entityType;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("visitNearest");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (context.firstFrame()) {
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            SectorEntityToken nearest = playerFleet.getContainingLocation().getCustomEntities().stream()
                    .filter(entity -> entity.getCustomEntitySpec() != null && entityType.equals(entity.getCustomEntitySpec().getId()))
                    .min(Comparator.comparingDouble(entity -> Misc.getDistance(playerFleet, entity)))
                    .orElse(null);
            if (nearest == null) {
                return StepOutcome.failed("no " + entityType + " is in the player fleet's location");
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
