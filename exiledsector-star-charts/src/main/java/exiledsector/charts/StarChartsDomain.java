package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import exiledsector.ModSettingsListener;
import exiledsector.compat.CompatTarget;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.effects.ShipEffectSource;
import exiledsector.i18n.I18n;
import exiledsector.i18n.SpecLocalisation;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemDataRules;
import exiledsector.items.ItemTypeSupport;
import exiledsector.items.MaterialItems;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartCurrency;
import exiledsector.items.StarChartDataRules;
import exiledsector.items.StarChartItems;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.LootSource;
import exiledsector.ui.ExiledSectorSettings;
import exiledsector.ui.chart.StarChartBench;
import exiledsector.ui.chart.WormholeGatewayOption;
import exiledsector.ui.socket.BenchModeSupport;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import java.util.List;

import static exiledsector.ExiledSector.MOD_ID;

public final class StarChartsDomain implements ExiledSectorDomain {

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
        I18n.forGameText(() -> {
            for (StarChartCurrency currency : StarChartCurrency.values()) {
                MaterialItems.localise(currency.itemId());
            }
            SpecLocalisation.industry(WormholeGateway.ID);
        });
    }

    @Override
    public void registerSettings() {
        SettingsCreator.addHeader(MOD_ID, "exiledSector_starChartsHeader", Translation.text("settings.starCharts.header"), ExiledSectorSettings.MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, StarChartSettings.ENABLED_FIELD_ID,
                Translation.text("settings.starCharts.enabled.name"), Translation.text("settings.starCharts.enabled.tooltip"),
                StarChartSettings.DEFAULT_ENABLED, ExiledSectorSettings.MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, StarChartSettings.ENTRIES_FIELD_ID,
                Translation.text("settings.starCharts.entries.name"), Translation.text("settings.starCharts.entries.tooltip"),
                StarChartSettings.DEFAULT_ENTRIES, StarChartSettings.MIN_ENTRIES, StarChartSettings.MAX_ENTRIES, ExiledSectorSettings.MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, StarChartSettings.FLEET_LIMIT_FIELD_ID,
                Translation.text("settings.starCharts.fleetLimit.name"), Translation.text("settings.starCharts.fleetLimit.tooltip"),
                StarChartSettings.DEFAULT_FLEET_LIMIT, StarChartSettings.MIN_FLEET_LIMIT, StarChartSettings.MAX_FLEET_LIMIT, ExiledSectorSettings.MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, StarChartSettings.OPEN_ANYWHERE_FIELD_ID,
                Translation.text("chart.ui.settings.openAnywhere.name"), Translation.text("chart.ui.settings.openAnywhere.tooltip"),
                StarChartSettings.DEFAULT_OPEN_ANYWHERE, ExiledSectorSettings.MAIN_TAB);
        ModSettingsListener.ensureRegistered(StarChartSettings.SettingsListener.class, StarChartSettings.SettingsListener::new);
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
    public List<ItemTypeSupport> itemTypes() {
        return List.of(StarChart.SUPPORT);
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
        return List.of(VaultsOfKnowledgeCompat.TARGET);
    }
}
