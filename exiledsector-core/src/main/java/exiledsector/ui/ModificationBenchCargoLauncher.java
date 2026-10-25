package exiledsector.ui;

import com.fs.starfarer.api.campaign.CampaignUIAPI;

public final class ModificationBenchCargoLauncher {

    private ModificationBenchCargoLauncher() {
    }

    public static boolean open(CampaignUIAPI campaignUI, Runnable afterClose) {
        return CampaignOverlay.open(campaignUI, ModificationBenchScreen::new, afterClose);
    }
}
