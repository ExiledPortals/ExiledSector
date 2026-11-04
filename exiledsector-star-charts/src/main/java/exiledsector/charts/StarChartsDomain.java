package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import exiledsector.compat.CompatTarget;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.layout.abyss.AbyssArt;
import exiledsector.charts.effect.ChartEnemyInflationListener;
import exiledsector.charts.effect.ChartEnemyShipEffects;
import exiledsector.charts.effect.ChartPlayerModifierSource;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.loot.StarChartLootSource;
import exiledsector.charts.objective.ChartRewardLootSource;
import exiledsector.charts.objective.GatewayUpgradeRewards;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.run.ChartTransverseJump;
import exiledsector.charts.wormhole.ColonyFreeAtlas;
import exiledsector.charts.wormhole.LimboBlueprintCache;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.compat.FoilerCompat;
import exiledsector.compat.UnknownSkiesCompat;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.effects.ShipEffectSource;
import exiledsector.i18n.I18n;
import exiledsector.i18n.SpecLocalisation;
import exiledsector.i18n.Translation;
import exiledsector.items.Engrams;
import exiledsector.items.AtlasCatalystItems;
import exiledsector.items.CraftingMaterialFamily;
import exiledsector.items.ItemDataRules;
import exiledsector.items.ItemTypeSupport;
import exiledsector.items.MaterialItems;
import exiledsector.items.StarChart;
import exiledsector.items.StarChartCipher;
import exiledsector.items.StarChartDataRules;
import exiledsector.items.StarChartItems;
import exiledsector.items.WormholeTechItems;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.LootSource;
import exiledsector.ui.cargo.CatalystIconFx;
import exiledsector.ui.chart.ForgeCraftingBench;
import exiledsector.ui.chart.StarChartCraftingBench;
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
        return List.of(ChartPlayerModifierSource.INSTANCE, ChartEnemyShipEffects.INSTANCE);
    }

    @Override
    public List<LootSource> lootSources() {
        return List.of(StarChartLootSource.INSTANCE, ChartRewardLootSource.INSTANCE);
    }

    @Override
    public List<FleetDifficulty> fleetDifficulties() {
        return List.of(StarChartLootSource.INSTANCE);
    }

    @Override
    public void onApplicationLoad() {
        StarChartCipher.registerAll();
        ChartAnomalies.all().forEach(ChartAnomaly::preloadTextures);
        AbyssArt.preload();
        CatalystIconFx.preload();
        I18n.forGameText(() -> {
            for (StarChartCipher currency : StarChartCipher.values()) {
                MaterialItems.localise(currency.itemId());
            }
            WormholeGateway.INDUSTRY_IDS.forEach(SpecLocalisation::industry);
        });
    }

    @Override
    public void registerSettings() {
        String chartsTab = StarChartSettings.settingsTab();
        StarChartSettings.SWITCH.addSettings("exiledSector_starChartsHeader", "settings.starCharts.header",
                "settings.starCharts.enabled.name", "settings.starCharts.enabled.tooltip", chartsTab);
        for (StarChartSettings.WormholeEntries entries : StarChartSettings.ENTRIES) {
            SettingsCreator.addInt(MOD_ID, entries.fieldId(), Translation.text(entries.nameKey()), Translation.text(entries.tooltipKey()),
                    entries.defaultEntries(), StarChartSettings.MIN_ENTRIES, StarChartSettings.MAX_ENTRIES, chartsTab);
        }
        SettingsCreator.addInt(MOD_ID, StarChartSettings.FLEET_LIMIT_FIELD_ID,
                Translation.text("settings.starCharts.fleetLimit.name"), Translation.text("settings.starCharts.fleetLimit.tooltip"),
                StarChartSettings.DEFAULT_FLEET_LIMIT, StarChartSettings.MIN_FLEET_LIMIT, StarChartSettings.MAX_FLEET_LIMIT, chartsTab);
        SettingsCreator.addBoolean(MOD_ID, StarChartSettings.NO_COLONY_FIELD_ID,
                Translation.text("settings.starCharts.noColony.name"), Translation.text("settings.starCharts.noColony.tooltip"),
                StarChartSettings.DEFAULT_NO_COLONY, chartsTab);
        SettingsCreator.addBoolean(MOD_ID, StarChartSettings.DEBUG_TOOLS_FIELD_ID,
                Translation.text("settings.starCharts.debugTools.name"), Translation.text("settings.starCharts.debugTools.tooltip"),
                StarChartSettings.DEFAULT_DEBUG_TOOLS, chartsTab);
    }

    @Override
    public void reloadSettings() {
        StarChartSettings.reloadSettings();
        ColonyFreeAtlas.onSettingsChanged();
    }

    @Override
    public void loadData() {
        StarCharts.loadData();
    }

    @Override
    public void loadItems() {
        StarChartItems.load();
        Engrams.load();
        BiomeModifiers.load();
        ChartAnomalies.all().forEach(ChartAnomaly::loadItems);
        I18n.forGameText(Engrams::localise);
        I18n.forGameText(AtlasCatalystItems::localise);
        I18n.forGameText(WormholeTechItems::localise);
    }

    @Override
    public void configureXStream(XStream xstream) {
        ChartSaveAliases.register(xstream);
    }

    @Override
    public void afterSaveRepair() {
        Global.getSector().getListenerManager().addListener(new WormholeGatewayOption(), true);
        Global.getSector().getListenerManager().addListener(new ChartEnemyInflationListener(), true);
    }

    @Override
    public void afterSocketCustody() {
        StarCharts.onGameLoad(Global.getSector());
        if (enabled()) {
            LimboBlueprintCache.onGameLoad(Global.getSector());
            GatewayUpgradeRewards.onGameLoad(Global.getSector());
        }
        ColonyFreeAtlas.onGameLoad(Global.getSector());
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
    public List<CraftingMaterialFamily> hullCraftingBenchCurrencies() {
        return List.copyOf(Engrams.all());
    }

    @Override
    public List<ItemDataRules> itemDataRules() {
        return List.of(StarChartDataRules.INSTANCE);
    }

    @Override
    public List<BenchModeSupport> benchModes() {
        return List.of(StarChartCraftingBench.INSTANCE, ForgeCraftingBench.INSTANCE);
    }

    @Override
    public List<CompatTarget> compatTargets() {
        return List.of(VaultsOfKnowledgeCompat.TARGET, FoilerCompat.TERRAIN_FX, FoilerCompat.HYDROFOIL, UnknownSkiesCompat.TARGET);
    }
}
