package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.DockedStorage;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.atlas.AtlasCatalysts;
import exiledsector.charts.atlas.AtlasData;
import exiledsector.charts.content.ChartBosses;
import exiledsector.charts.content.ChartColonyItems;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.effect.ChartEffects;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.layout.feature.ChartFeatures;
import exiledsector.charts.loot.ChartDrops;
import exiledsector.charts.loot.ChartLoot;
import exiledsector.charts.loot.ChartMaterials;
import exiledsector.charts.officer.OfficerRanks;
import exiledsector.charts.officer.reward.OfficerRewardTable;
import exiledsector.charts.officer.turn.OfficerTurnTables;
import exiledsector.charts.run.ChartBlockedAbilities;
import exiledsector.charts.run.ChartCampaignPlugin;
import exiledsector.charts.run.ChartContentListener;
import exiledsector.charts.run.ChartRunScript;
import exiledsector.charts.run.cleanup.ChartDefenderTallies;
import exiledsector.charts.run.cleanup.ChartLoadSweep;
import exiledsector.charts.run.diagnostics.ChartSoakScript;
import exiledsector.charts.tree.AtlasTreeData;
import exiledsector.items.ItemStore;
import exiledsector.items.RetiredUniqueCharts;
import exiledsector.ui.chart.ChartInfoTab;
import org.apache.log4j.Logger;

public final class StarCharts {

    private static final Logger LOG = Logger.getLogger(StarCharts.class);

    private StarCharts() {
    }

    public static void onGameLoad(SectorAPI sector) {
        ChartLoadSweep.sweep(sector);
        ChartDefenderTallies.register(sector);
        sector.unregisterPlugin(ChartCampaignPlugin.ID);
        sector.registerPlugin(new ChartCampaignPlugin());
        sector.removeTransientScriptsOfClass(ChartRunScript.class);
        sector.removeTransientScriptsOfClass(ChartSoakScript.class);
        sector.addTransientScript(new ChartRunScript());
        if (sector.getListenerManager() != null) {
            sector.getListenerManager().removeListenerOfClass(ChartContentListener.class);
            sector.getListenerManager().addListener(new ChartContentListener(), true);
            sector.getListenerManager().removeListenerOfClass(ChartInfoTab.class);
            sector.getListenerManager().addListener(new ChartInfoTab(), true);
        }
        RetiredUniqueCharts.removeFrom(sector);
        absorbCharts(sector);
    }

    public static int absorbCharts(SectorAPI sector) {
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        try {
            ItemStore existing = ItemStore.starChartsIfCreated();
            if (existing != null) {
                existing.freezeNames();
            }
            int movedCount = 0;
            for (CargoAPI cargo : DockedStorage.withPlayerCargo(playerFleet == null ? null : playerFleet.getCargo())) {
                movedCount += ItemStore.carriesStarCharts(cargo) ? ItemStore.starCharts().absorbFrom(cargo) : 0;
            }
            return movedCount;
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
        ChartMaterials.load();
        OfficerRewardTable.load();
        ChartPois.load();
        ChartLoot.load();
        ChartColonyItems.load();
        ChartFactions.load();
        ChartBosses.load();
        ChartAnomalies.load();
        AtlasCatalysts.load();
        ChartAnomalies.all().forEach(ChartAnomaly::loadData);
        ChartFeatures.load();
        AtlasData.load();
        OfficerRanks.load();
        OfficerTurnTables.load();
        AtlasTreeData.load();
    }
}
