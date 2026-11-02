package exiledsector.ui;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import exiledsector.ui.socket.BenchMode;
import exiledsector.ui.socket.BenchModeSupport;

import java.util.function.Consumer;

public final class CraftingBenchCargoLauncher {

    private CraftingBenchCargoLauncher() {
    }

    public static boolean open(CampaignUIAPI campaignUI, Runnable afterClose) {
        return CampaignOverlay.open(campaignUI, CraftingBenchScreen::new, afterClose);
    }

    public static boolean openMode(CampaignUIAPI campaignUI, BenchMode benchMode, BenchModeSupport.OpenTarget openTarget, Consumer<String> opened,
                                   Runnable afterClose) {
        return CampaignOverlay.open(campaignUI, host -> new CraftingBenchScreen(host, benchMode, openTarget, opened), afterClose);
    }
}
