package exiledsector.ui.cargo;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;

public final class FreshLoot {

    private FreshLoot() {
    }

    public static boolean isFreshLoot(CargoStackAPI stack) {
        CargoAPI cargo = stack == null ? null : stack.getCargo();
        SectorAPI sector = Global.getSector();
        if (cargo == null || sector == null || sector.getCampaignUI() == null) {
            return false;
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet != null && cargo == playerFleet.getCargo()) {
            return false;
        }
        InteractionDialogAPI dialog = sector.getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null) {
            return false;
        }
        SectorEntityToken target = dialog.getInteractionTarget();
        MarketAPI market = target == null ? null : target.getMarket();
        if (market != null) {
            for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
                if (submarket.getCargo() == cargo) {
                    return false;
                }
            }
        }
        return true;
    }
}
