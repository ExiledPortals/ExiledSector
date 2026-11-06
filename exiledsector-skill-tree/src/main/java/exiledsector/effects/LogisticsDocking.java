package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.loading.IndustrySpecAPI;

public final class LogisticsDocking {

    private LogisticsDocking() {
    }

    public static boolean canChangeNow() {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
        InteractionDialogAPI dialog = campaignUI == null ? null : campaignUI.getCurrentInteractionDialog();
        SectorEntityToken target = dialog == null ? null : dialog.getInteractionTarget();
        return hasSpaceportOrStation(target == null ? null : target.getMarket());
    }

    static boolean hasSpaceportOrStation(MarketAPI market) {
        if (market == null) {
            return false;
        }
        for (Industry industry : market.getIndustries()) {
            IndustrySpecAPI spec = industry.getSpec();
            if (spec != null && (spec.hasTag(Industries.TAG_STATION) || spec.hasTag(Industries.TAG_SPACEPORT))) {
                return true;
            }
        }
        return false;
    }
}
