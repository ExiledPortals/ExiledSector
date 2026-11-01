package exiledsector.items;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import exiledsector.SectorMarkets;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class MaterialMigration {

    private static final Logger LOG = Logger.getLogger(MaterialMigration.class);

    private MaterialMigration() {
    }

    public static void run(List<CargoAPI> savedCargoes) {
        int convertedCount = 0;
        for (CargoAPI cargo : savedCargoes) {
            convertedCount += convert(cargo);
        }
        if (convertedCount > 0) {
            LOG.info("Converted " + convertedCount + " hull item materials from commodities to special items");
        }
    }

    public static int convert(CargoAPI cargo) {
        if (cargo == null) {
            return 0;
        }
        int convertedCount = 0;
        for (String itemId : MaterialItems.ids()) {
            int legacyQuantity = Math.round(cargo.getCommodityQuantity(itemId));
            if (legacyQuantity > 0) {
                cargo.removeCommodity(itemId, legacyQuantity);
                MaterialItems.add(cargo, itemId, legacyQuantity);
                convertedCount += legacyQuantity;
            }
        }
        return convertedCount;
    }

    public static List<CargoAPI> savedCargoes(SectorAPI sector) {
        List<CargoAPI> cargoes = new ArrayList<>();
        if (sector == null) {
            return cargoes;
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet != null) {
            cargoes.add(playerFleet.getCargo());
        }
        SectorMarkets.forEach(sector, market -> {
            for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
                CargoAPI submarketCargo = submarket.getCargoNullOk();
                if (submarketCargo != null) {
                    cargoes.add(submarketCargo);
                }
            }
        });
        return cargoes;
    }
}
