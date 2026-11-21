package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;

import java.util.function.Predicate;

final class OpenOverlayStep implements HarnessStep {

    private final String stepName;
    private final String screenName;
    private final Predicate<CampaignUIAPI> opener;

    OpenOverlayStep(String stepName, String screenName, Predicate<CampaignUIAPI> opener) {
        this.stepName = stepName;
        this.screenName = screenName;
        this.opener = opener;
    }

    @Override
    public String name() {
        return stepName;
    }

    @Override
    public StepOutcome advance(StepContext context) {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (!opener.test(campaignUI)) {
            String dialog = campaignUI.getCurrentInteractionDialog() == null ? "" : " while a dialog was open";
            return StepOutcome.failed(screenName + " did not open" + dialog + "; another Exiled Sector screen may already be open");
        }
        String openedLine = context.log().lastLineContaining("[perf] " + screenName + " opened", context.firstLogLine());
        return StepOutcome.passed(openedLine == null ? screenName + " opened" : openedLine);
    }
}
