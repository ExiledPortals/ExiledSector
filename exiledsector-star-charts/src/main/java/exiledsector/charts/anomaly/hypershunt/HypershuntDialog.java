package exiledsector.charts.anomaly.hypershunt;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import exiledsector.i18n.Translation;
import exiledsector.charts.anomaly.GuardedSiteDialog;
import exiledsector.ui.VanillaText;

public class HypershuntDialog extends GuardedSiteDialog {

    @Override
    protected String descriptionKey() {
        return "chart.hypershunt.description";
    }

    @Override
    protected String guardedKey() {
        return "chart.hypershunt.guarded";
    }

    @Override
    protected CampaignFleetAPI musterGuards() {
        return HypershuntGuards.muster(site(), CoronalHypershunt.liveRunFor(site()));
    }

    @Override
    protected void showSite() {
        options().clearOptions();
        VanillaText.plainPara(text(), Translation.gameText("chart.hypershunt.beyondRepair"));
        addLeaveOption();
    }
}
