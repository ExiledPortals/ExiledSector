package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;

final class CloseDialogStep implements HarnessStep {

    @Override
    public String name() {
        return "closeDialog";
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
        if (notInCampaign != null) {
            return notInCampaign;
        }
        InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (context.firstFrame()) {
            if (dialog == null) {
                return StepOutcome.failed("no interaction dialog is open");
            }
            dialog.dismiss();
            return StepOutcome.RUNNING;
        }
        return dialog == null ? StepOutcome.passed("closed the interaction dialog")
                : new StepOutcome(false, false, "the interaction dialog never closed");
    }
}
