package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.charts.anomaly.skirmish.SkirmishFactions;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.items.ItemStore;
import org.apache.log4j.Logger;

public final class StarCharts {

    private static final Logger LOG = Logger.getLogger(StarCharts.class);

    private StarCharts() {
    }

    public static void onGameLoad(SectorAPI sector) {
        ChartLoadSweep.sweep(sector);
        sector.unregisterPlugin(ChartCampaignPlugin.ID);
        sector.registerPlugin(new ChartCampaignPlugin());
        sector.removeTransientScriptsOfClass(ChartRunScript.class);
        sector.removeTransientScriptsOfClass(ChartSoakScript.class);
        sector.addTransientScript(new ChartRunScript());
        if (sector.getListenerManager() != null) {
            sector.getListenerManager().removeListenerOfClass(ChartContentListener.class);
            sector.getListenerManager().addListener(new ChartContentListener(), true);
        }
        absorbCharts(sector);
    }

    public static int absorbCharts(SectorAPI sector) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        try {
            ItemStore existing = ItemStore.starChartsIfCreated();
            if (existing != null) {
                existing.freezeNames();
            }
            CargoAPI cargo = playerFleet == null ? null : playerFleet.getCargo();
            return ItemStore.carriesStarCharts(cargo) ? ItemStore.starCharts().absorbFrom(cargo) : 0;
        } catch (RuntimeException e) {
            LOG.error("[ExiledSector] Moving Star Charts from cargo into storage failed", e);
            return 0;
        }
    }

    public static void loadData() {
        ChartEffects.register();
        ChartBlockedAbilities.load();
        ChartTiers.load();
        ChartDrops.load();
        ChartPois.load();
        ChartLoot.load();
        ChartColonyItems.load();
        ChartFactions.load();
        ChartBosses.load();
        ChartAnomalies.load();
        SkirmishFactions.load();
        ChartFeatures.load();
        AtlasData.load();
    }
}
