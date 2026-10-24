package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import java.awt.Color;

public final class CampaignMessages {

    private CampaignMessages() {
    }

    public static boolean available() {
        return campaignUi() != null;
    }

    public static void plain(String message, Color color) {
        CampaignUIAPI campaignUi = campaignUi();
        if (campaignUi != null) {
            campaignUi.addMessage(message, color);
        }
    }

    public static void highlighted(String message, Color color, String firstHighlight, String secondHighlight,
                                   Color firstHighlightColor, Color secondHighlightColor) {
        CampaignUIAPI campaignUi = campaignUi();
        if (campaignUi != null) {
            campaignUi.addMessage(VanillaText.escapeFormat(message), color, firstHighlight, secondHighlight, firstHighlightColor, secondHighlightColor);
        }
    }

    private static CampaignUIAPI campaignUi() {
        SectorAPI sector = Global.getSector();
        return sector == null ? null : sector.getCampaignUI();
    }
}
