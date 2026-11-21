package exiledsector;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

public final class DockedStorage {

    private DockedStorage() {
    }

    public static List<CargoAPI> withPlayerCargo(CargoAPI playerCargo) {
        List<CargoAPI> cargoes = new ArrayList<>();
        if (playerCargo != null) {
            cargoes.add(playerCargo);
        }
        CargoAPI storageCargo = cargoOrNull();
        if (storageCargo != null) {
            cargoes.add(storageCargo);
        }
        return cargoes;
    }

    public static CargoAPI cargoOrNull() {
        MarketAPI dockedMarket = dockedMarketOrNull();
        if (dockedMarket == null || !Misc.playerHasStorageAccess(dockedMarket)) {
            return null;
        }
        CargoAPI storageCargo = Misc.getStorageCargo(dockedMarket);
        return storageCargo == PlayerFleet.cargoOrNull() ? null : storageCargo;
    }

    private static MarketAPI dockedMarketOrNull() {
        SectorAPI sector = Global.getSector();
        CampaignUIAPI campaignUI = sector == null ? null : sector.getCampaignUI();
        InteractionDialogAPI dialog = campaignUI == null ? null : campaignUI.getCurrentInteractionDialog();
        SectorEntityToken dockedEntity = dialog == null ? null : dialog.getInteractionTarget();
        return dockedEntity == null ? null : dockedEntity.getMarket();
    }
}
