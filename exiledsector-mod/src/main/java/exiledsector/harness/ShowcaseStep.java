package exiledsector.harness;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

final class ShowcaseStep implements HarnessStep {

    static final String VERSION_AND_FPS_SETTING = "renderVersionAndIdle";

    private final boolean hideCampaignUi;
    private final boolean keepPaused;

    ShowcaseStep(boolean hideCampaignUi, boolean keepPaused) {
        this.hideCampaignUi = hideCampaignUi;
        this.keepPaused = keepPaused;
    }

    @Override
    public String name() {
        return "showcase" + (hideCampaignUi ? " without the campaign UI" : "") + (keepPaused ? " paused" : "");
    }

    @Override
    public StepOutcome advance(StepContext context) {
        Global.getSettings().setBoolean(VERSION_AND_FPS_SETTING, false);
        StepOutcome notInCampaign = HarnessCampaign.notInCampaign(name());
        if (notInCampaign != null) {
            return StepOutcome.passed("hid the version and FPS readout; not in the campaign, so its messages and UI stay");
        }
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUi = sector.getCampaignUI();
        campaignUi.clearMessages();
        campaignUi.setHideUI(hideCampaignUi);
        if (!keepPaused) {
            HarnessCampaign.unpause(sector);
        }
        return StepOutcome.passed("hid the version and FPS readout, cleared the campaign messages" + (hideCampaignUi ? ", hid the campaign UI" : "")
                + (keepPaused ? "" : ", unpaused the campaign"));
    }
}
