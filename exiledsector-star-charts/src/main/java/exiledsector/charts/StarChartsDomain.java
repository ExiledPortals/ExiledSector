package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import exiledlib.compat.CompatTarget;
import exiledsector.charts.anomaly.debris.AnomalousDebrisTerrainPlugin;
import exiledsector.charts.layout.abyss.AbyssArt;
import exiledsector.charts.effect.ChartFleetConditionSource;
import exiledsector.charts.loot.StarChartLootSource;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.run.ChartTransverseJump;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.compat.FoilerCompat;
import exiledsector.compat.UnknownSkiesCompat;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.effects.ShipEffectSource;
import exiledlib.i18n.I18n;
import exiledlib.i18n.SpecLocalisation;
import exiledlib.i18n.Translation;
import exiledsector.items.AnomalousDebrisCurrencies;
import exiledsector.items.AtlasCatalystItems;
import exiledsector.items.ItemCurrency;
import exiledsector.items.ItemDataRules;
import exiledsector.items.ItemTypeSupport;
import exiledsector.items.MaterialItems;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartCurrency;
import exiledsector.items.StarChartDataRules;
import exiledsector.items.StarChartItems;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.LootSource;
import exiledsector.ui.cargo.CatalystIconFx;
import exiledsector.ui.chart.StarChartBench;
import exiledsector.ui.chart.WormholeGatewayOption;
import exiledsector.ui.socket.BenchModeSupport;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import java.util.List;

import static exiledsector.ExiledSector.MOD_ID;

public final class StarChartsDomain implements ExiledSectorDomain {

    @Override
    public boolean enabled() {
        return StarChartSettings.enabled() || Global.getSector() != null && ChartRuns.hasRun();
    }

    @Override
    public List<ShipEffectSource> shipEffectSources() {
        return List.of(ChartFleetConditionSource.INSTANCE);
    }

    @Override
    public List<LootSource> lootSources() {
        return List.of(StarChartLootSource.INSTANCE);
    }

    @Override
    public List<FleetDifficulty> fleetDifficulties() {
        return List.of(StarChartLootSource.INSTANCE);
    }

    @Override
    public void onApplicationLoad() {
        StarChartCurrency.registerAll();
        AnomalousDebrisTerrainPlugin.preloadTextures();
        AbyssArt.preload();
        CatalystIconFx.preload();
        I18n.forGameText(() -> {
            for (StarChartCurrency currency : StarChartCurrency.values()) {
                MaterialItems.localise(currency.itemId());
            }
            SpecLocalisation.industry(WormholeGateway.INDUSTRY_ID);
        });
    }

    @Override
    public void registerSettings() {
        String chartsTab = StarChartSettings.settingsTab();
        StarChartSettings.SWITCH.addSettings("exiledSector_starChartsHeader", "settings.starCharts.header",
                "settings.starCharts.enabled.name", "settings.starCharts.enabled.tooltip", chartsTab);
        SettingsCreator.addInt(MOD_ID, StarChartSettings.ENTRIES_FIELD_ID,
                Translation.text("settings.starCharts.entries.name"), Translation.text("settings.starCharts.entries.tooltip"),
                StarChartSettings.DEFAULT_ENTRIES, StarChartSettings.MIN_ENTRIES, StarChartSettings.MAX_ENTRIES, chartsTab);
        SettingsCreator.addInt(MOD_ID, StarChartSettings.FLEET_LIMIT_FIELD_ID,
                Translation.text("settings.starCharts.fleetLimit.name"), Translation.text("settings.starCharts.fleetLimit.tooltip"),
                StarChartSettings.DEFAULT_FLEET_LIMIT, StarChartSettings.MIN_FLEET_LIMIT, StarChartSettings.MAX_FLEET_LIMIT, chartsTab);
        SettingsCreator.addBoolean(MOD_ID, StarChartSettings.OPEN_ANYWHERE_FIELD_ID,
                Translation.text("chart.ui.settings.openAnywhere.name"), Translation.text("chart.ui.settings.openAnywhere.tooltip"),
                StarChartSettings.DEFAULT_OPEN_ANYWHERE, chartsTab);
        SettingsCreator.addBoolean(MOD_ID, StarChartSettings.DEBUG_TOOLS_FIELD_ID,
                Translation.text("chart.ui.settings.debugTools.name"), Translation.text("chart.ui.settings.debugTools.tooltip"),
                StarChartSettings.DEFAULT_DEBUG_TOOLS, chartsTab);
    }

    @Override
    public void reloadSettings() {
        StarChartSettings.reloadSettings();
    }

    @Override
    public void loadData() {
        StarCharts.loadData();
    }

    @Override
    public void loadItems() {
        StarChartItems.load();
        AnomalousDebrisCurrencies.load();
        I18n.forGameText(AnomalousDebrisCurrencies::localise);
        I18n.forGameText(AtlasCatalystItems::localise);
    }

    @Override
    public void configureXStream(XStream xstream) {
        ChartSaveAliases.register(xstream);
    }

    @Override
    public void afterSaveRepair() {
        Global.getSector().getListenerManager().addListener(new WormholeGatewayOption(), true);
    }

    @Override
    public void afterSocketCustody() {
        StarCharts.onGameLoad(Global.getSector());
    }

    @Override
    public void beforeGameSave() {
        ChartTransverseJump.restoreBeforeSave(Global.getSector().getPlayerFleet());
    }

    @Override
    public List<ItemTypeSupport> itemTypes() {
        return List.of(StarChart.SUPPORT);
    }

    @Override
    public List<ItemCurrency> hullBenchCurrencies() {
        return List.copyOf(AnomalousDebrisCurrencies.all());
    }

    @Override
    public List<ItemDataRules> itemDataRules() {
        return List.of(StarChartDataRules.INSTANCE);
    }

    @Override
    public List<BenchModeSupport> benchModes() {
        return List.of(StarChartBench.INSTANCE);
    }

    @Override
    public List<CompatTarget> compatTargets() {
        return List.of(VaultsOfKnowledgeCompat.TARGET, FoilerCompat.TERRAIN_FX, FoilerCompat.HYDROFOIL, UnknownSkiesCompat.TARGET);
    }
}
