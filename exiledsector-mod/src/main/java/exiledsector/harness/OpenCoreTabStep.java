package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import exiledsector.ui.refit.CoreUiDismissal;

import java.util.Arrays;
import java.util.Locale;

final class OpenCoreTabStep implements HarnessStep {

    private final CoreUITabId tab;
    private boolean tabRequested;

    OpenCoreTabStep(String tabName) {
        this.tab = tab(tabName);
    }

    static CoreUITabId tab(String tabName) {
        try {
            return CoreUITabId.valueOf(tabName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknownTab) {
            throw new IllegalArgumentException("openCoreTab opens one of " + Arrays.toString(CoreUITabId.values()).toLowerCase(Locale.ROOT)
                    + ", not \"" + tabName + "\"", unknownTab);
        }
    }

    @Override
    public String name() {
        return "openCoreTab " + tab.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public StepOutcome advance(StepContext context) {
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign("openCoreTab");
        if (notInCampaign != null) {
            return notInCampaign;
        }
        CampaignUIAPI campaignUi = Global.getSector().getCampaignUI();
        if (context.firstFrame()) {
            if (campaignUi.getCurrentCoreTab() != null && campaignUi.getCurrentCoreTab() != tab && !CoreUiDismissal.dismiss(campaignUi)) {
                return StepOutcome.failed("the open " + campaignUi.getCurrentCoreTab() + " screen would not close before opening " + tab);
            }
            return StepOutcome.RUNNING;
        }
        if (!tabRequested) {
            if (campaignUi.getCurrentCoreTab() == tab) {
                return StepOutcome.passed("the " + tab + " tab was already open");
            }
            if (campaignUi.getCurrentCoreTab() != null) {
                return new StepOutcome(false, false, "the open " + campaignUi.getCurrentCoreTab() + " screen never closed");
            }
            campaignUi.showCoreUITab(tab);
            tabRequested = true;
            return StepOutcome.RUNNING;
        }
        return campaignUi.getCurrentCoreTab() == tab
                ? StepOutcome.passed("the " + tab + " tab opened")
                : new StepOutcome(false, false, "the " + tab + " tab never opened");
    }
}
