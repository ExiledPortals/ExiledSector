package exiledsector.items;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class MaterialMigration {

    private static final Logger LOG = Logger.getLogger(MaterialMigration.class);

    private MaterialMigration() {
    }

    public static void run(SectorAPI sector) {
        if (sector == null) {
            return;
        }
        int convertedCount = 0;
        for (CargoAPI cargo : cargoes(sector)) {
            convertedCount += convert(cargo);
        }
        if (convertedCount > 0) {
            LOG.info("Converted " + convertedCount + " hull item materials from commodities to special items");
        }
    }

    static int convert(CargoAPI cargo) {
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

    private static List<CargoAPI> cargoes(SectorAPI sector) {
        List<CargoAPI> cargoes = new ArrayList<>();
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet != null) {
            cargoes.add(playerFleet.getCargo());
        }
        Set<MarketAPI> visitedMarkets = Collections.newSetFromMap(new IdentityHashMap<>());
        if (sector.getEconomy() != null) {
            sector.getEconomy().getMarketsCopy().forEach(market -> addSubmarkets(market, visitedMarkets, cargoes));
        }
        List<LocationAPI> locations = new ArrayList<>(sector.getStarSystems());
        locations.add(sector.getHyperspace());
        for (LocationAPI location : locations) {
            if (location == null) {
                continue;
            }
            for (SectorEntityToken entity : location.getAllEntities()) {
                addSubmarkets(entity.getMarket(), visitedMarkets, cargoes);
            }
        }
        return cargoes;
    }

    private static void addSubmarkets(MarketAPI market, Set<MarketAPI> visitedMarkets, List<CargoAPI> cargoes) {
        if (market == null || !visitedMarkets.add(market)) {
            return;
        }
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (submarket.getCargo() != null) {
                cargoes.add(submarket.getCargo());
            }
        }
    }
}
