package exiledsector.ui;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import exiledsector.ui.socket.BenchMode;
import exiledsector.ui.socket.BenchModeSupport;

import java.util.function.Consumer;

public final class ModificationBenchCargoLauncher {

    private ModificationBenchCargoLauncher() {
    }

    public static boolean open(CampaignUIAPI campaignUI, Runnable afterClose) {
        return CampaignOverlay.open(campaignUI, ModificationBenchScreen::new, afterClose);
    }

    public static boolean openMode(CampaignUIAPI campaignUI, BenchMode benchMode, BenchModeSupport.OpenTarget openTarget, Consumer<String> opened,
                                   Runnable afterClose) {
        return CampaignOverlay.open(campaignUI, host -> new ModificationBenchScreen(host, benchMode, openTarget, opened), afterClose);
    }
}
