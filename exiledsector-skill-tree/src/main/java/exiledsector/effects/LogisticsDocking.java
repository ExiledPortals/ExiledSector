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
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;

import java.util.List;

public final class LogisticsDocking {

    private LogisticsDocking() {
    }

    public static boolean canChangeNow() {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
        InteractionDialogAPI dialog = campaignUI == null ? null : campaignUI.getCurrentInteractionDialog();
        SectorEntityToken target = dialog == null ? null : dialog.getInteractionTarget();
        if (hasSpaceportOrStation(target == null ? null : target.getMarket())) {
            return true;
        }
        SectorEntityToken playerFleet = PlayerFleet.orNull();
        if (playerFleet == null || playerFleet.getContainingLocation() == null) {
            return false;
        }
        return nearSpaceportOrStation(playerFleet, Misc.getMarketsInLocation(playerFleet.getContainingLocation()), Misc.getBattleJoinRange());
    }

    static boolean nearSpaceportOrStation(SectorEntityToken fleet, List<MarketAPI> localMarkets, float range) {
        for (MarketAPI market : localMarkets) {
            if (hasSpaceportOrStation(market) && withinRange(fleet, market, range)) {
                return true;
            }
        }
        return false;
    }

    private static boolean withinRange(SectorEntityToken fleet, MarketAPI market, float range) {
        if (withinRange(fleet, market.getPrimaryEntity(), range)) {
            return true;
        }
        for (SectorEntityToken entity : market.getConnectedEntities()) {
            if (withinRange(fleet, entity, range)) {
                return true;
            }
        }
        return false;
    }

    private static boolean withinRange(SectorEntityToken fleet, SectorEntityToken entity, float range) {
        return entity != null && entity.getContainingLocation() == fleet.getContainingLocation()
                && Misc.getDistance(fleet.getLocation(), entity.getLocation()) - entity.getRadius() - fleet.getRadius() <= range;
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
