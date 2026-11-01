package exiledsector;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

public final class SectorMarkets {

    private SectorMarkets() {
    }

    public static void forEach(SectorAPI sector, Consumer<MarketAPI> visitor) {
        if (sector == null) {
            return;
        }
        Set<MarketAPI> visitedMarkets = Collections.newSetFromMap(new IdentityHashMap<>());
        if (sector.getEconomy() != null) {
            sector.getEconomy().getMarketsCopy().forEach(market -> visitOnce(market, visitedMarkets, visitor));
        }
        for (LocationAPI location : sector.getAllLocations()) {
            for (SectorEntityToken entity : location.getAllEntities()) {
                visitOnce(entity.getMarket(), visitedMarkets, visitor);
            }
        }
    }

    private static void visitOnce(MarketAPI market, Set<MarketAPI> visitedMarkets, Consumer<MarketAPI> visitor) {
        if (market != null && visitedMarkets.add(market)) {
            visitor.accept(market);
        }
    }
}
