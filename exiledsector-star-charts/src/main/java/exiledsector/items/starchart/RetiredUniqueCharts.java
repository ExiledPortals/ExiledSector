package exiledsector.items.starchart;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import exiledsector.items.Item;
import exiledsector.items.ItemData;
import exiledsector.items.ItemStore;
import exiledsector.items.custody.SavedCargo;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RetiredUniqueCharts {

    static final Set<String> DEFINITION_IDS = Set.of("unique_chart_silent_reach", "unique_chart_boss_vigil");
    private static final Logger LOG = Logger.getLogger(RetiredUniqueCharts.class);

    private RetiredUniqueCharts() {
    }

    public static int removeFrom(SectorAPI sector) {
        try {
            int removedCount = removeFrom(ItemStore.starChartsIfCreated());
            removedCount += SavedCargo.removeStacks(playerAndStorageCargoes(sector), RetiredUniqueCharts::holdsRetiredChart);
            if (removedCount > 0) {
                LOG.info("[ExiledSector] Removed " + removedCount + " retired unique Star Charts");
            }
            return removedCount;
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Removing retired unique Star Charts failed", e);
            return 0;
        }
    }

    static int removeFrom(ItemStore store) {
        if (store == null) {
            return 0;
        }
        List<Item> retired = store.owned().stream().filter(item -> isRetired(item.definitionId())).toList();
        retired.forEach(store::remove);
        return retired.size();
    }

    static int removeFrom(CargoAPI cargo) {
        return SavedCargo.removeStacks(cargo, RetiredUniqueCharts::holdsRetiredChart);
    }

    private static List<CargoAPI> playerAndStorageCargoes(SectorAPI sector) {
        List<CargoAPI> cargoes = new ArrayList<>();
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        cargoes.add(playerFleet == null ? null : playerFleet.getCargo());
        if (sector.getEconomy() != null) {
            for (MarketAPI market : sector.getEconomy().getMarketsCopy()) {
                SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
                cargoes.add(storage == null ? null : storage.getCargo());
            }
        }
        return cargoes;
    }

    private static boolean holdsRetiredChart(CargoStackAPI stack) {
        SpecialItemData special = stack.getSpecialDataIfSpecial();
        ItemData chart = special == null || !ItemData.STAR_CHART_ITEM_ID.equals(special.getId()) ? null : ItemData.ofStarChart(special);
        return chart != null && isRetired(chart.definitionId());
    }

    private static boolean isRetired(String definitionId) {
        return definitionId != null && DEFINITION_IDS.contains(definitionId);
    }
}
