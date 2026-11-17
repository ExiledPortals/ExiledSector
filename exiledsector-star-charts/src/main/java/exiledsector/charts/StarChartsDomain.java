package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import exiledsector.compat.CompatTarget;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.layout.abyss.AbyssArt;
import exiledsector.charts.effect.ChartEnemyInflationListener;
import exiledsector.charts.effect.ChartEnemyShipEffects;
import exiledsector.charts.effect.ChartFleetDifficulty;
import exiledsector.charts.effect.ChartPlayerModifierSource;
import exiledsector.charts.loot.BiomeModifiers;
import exiledsector.charts.loot.ChartStorageTransfer;
import exiledsector.charts.loot.StarChartLootSource;
import exiledsector.charts.objective.BossDefeatLootSource;
import exiledsector.charts.objective.ChartObjectives;
import exiledsector.charts.officer.scar.ScarEffectSource;
import exiledsector.charts.phasing.DisplacementTheorem;
import exiledsector.charts.phasing.PhaseDisplacementGate;
import exiledsector.charts.phasing.PhasedPlanetTraits;
import exiledsector.charts.run.ChartRuns;
import exiledsector.charts.run.ChartTransverseJump;
import exiledsector.charts.wormhole.ColonyFreeAtlas;
import exiledsector.charts.wormhole.GatewayUpgradeRewards;
import exiledsector.charts.wormhole.LimboBlueprintCache;
import exiledsector.charts.wormhole.WormholeGateway;
import exiledsector.compat.DiyPlanetsCompat;
import exiledsector.compat.FoilerCompat;
import exiledsector.compat.RuthlessSectorCompat;
import exiledsector.compat.UnknownSkiesCompat;
import exiledsector.compat.VaultsOfKnowledgeCompat;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.effects.PlayerEngagementPipeline;
import exiledsector.effects.ShipEffectSource;
import exiledsector.i18n.I18n;
import exiledsector.i18n.SpecLocalisation;
import exiledsector.i18n.Translation;
import exiledsector.items.crafting.material.Engrams;
import exiledsector.items.starchart.AtlasCatalystItems;
import exiledsector.charts.officer.scar.ScarBattleResults;
import exiledsector.items.crafting.BiomeCraft;
import exiledsector.items.crafting.material.DomainMaterial;
import exiledsector.items.crafting.material.InfusedFeedstock;
import exiledsector.items.ItemDataRules;
import exiledsector.items.ItemTypeSupport;
import exiledsector.items.crafting.material.MaterialItems;
import exiledsector.items.starchart.StarChart;
import exiledsector.items.crafting.material.StarChartCipher;
import exiledsector.items.starchart.StarChartDataRules;
import exiledsector.items.starchart.StarChartItems;
import exiledsector.items.starchart.WormholeTechItems;
import exiledsector.loot.FleetDifficulty;
import exiledsector.loot.LootSource;
import exiledsector.loot.LootTakenListener;
import exiledsector.ui.cargo.BiomeItemIconLooks;
import exiledsector.ui.cargo.CatalystIconFx;
import exiledsector.ui.cargo.InfusedFeedstockIconLooks;
import exiledsector.ui.cargo.ItemIconLooks;
import exiledsector.ui.inspect.CodexModifierTable;
import exiledsector.ui.chart.ForgeCraftingBench;
import exiledsector.ui.chart.StarChartCraftingBench;
import exiledsector.ui.chart.WormholeGatewayOption;
import exiledsector.ui.socket.BenchModeSupport;
import exiledsector.ui.socket.BenchTray;
import exiledsector.ui.socket.EngramTray;
import exiledsector.ui.socket.InfusedCraftingTray;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static exiledsector.ExiledSector.MOD_ID;

public final class StarChartsDomain implements ExiledSectorDomain {

    @Override
    public boolean enabled() {
        return StarChartSettings.enabled() || Global.getSector() != null && ChartRuns.hasRun();
    }

    @Override
    public List<ShipEffectSource> shipEffectSources() {
        return List.of(ChartPlayerModifierSource.INSTANCE, ChartEnemyShipEffects.INSTANCE, ScarEffectSource.INSTANCE);
    }

    @Override
    public List<ItemIconLooks> itemIconLooks() {
        return List.of(BiomeItemIconLooks.INSTANCE, InfusedFeedstockIconLooks.INSTANCE);
    }

    @Override
    public List<LootSource> lootSources() {
        return List.of(StarChartLootSource.INSTANCE, BossDefeatLootSource.INSTANCE);
    }

    @Override
    public Map<PlayerEngagementPipeline.Stage, PlayerEngagementPipeline.Step> engagementSteps() {
        return Map.of(PlayerEngagementPipeline.Stage.RECORD_OBJECTIVES, ChartObjectives::afterPlayerEngagement);
    }

    @Override
    public List<LootTakenListener> lootTakenListeners() {
        return List.of(new ChartStorageTransfer());
    }

    @Override
    public List<FleetDifficulty> fleetDifficulties() {
        return List.of(ChartFleetDifficulty.INSTANCE);
    }

    @Override
    public void onApplicationLoad() {
        StarChartCipher.registerAll();
        ChartAnomalies.all().forEach(ChartAnomaly::preloadTextures);
        AbyssArt.preload();
        CatalystIconFx.preload();
        I18n.forGameText(() -> {
            for (StarChartCipher craftingItem : StarChartCipher.values()) {
                MaterialItems.localise(craftingItem.itemId());
            }
            for (InfusedFeedstock feedstock : InfusedFeedstock.values()) {
                MaterialItems.localise(feedstock.itemId());
            }
            WormholeGateway.INDUSTRY_IDS.forEach(SpecLocalisation::industry);
            SpecLocalisation.industry(PhaseDisplacementGate.INDUSTRY_ID);
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
        I18n.forGameText(DisplacementTheorem::localise);
        I18n.forGameText(PhasedPlanetTraits::localise);
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
        ScarBattleResults.clear();
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
    public List<BenchTray> hullCraftingBenchTrays() {
        return List.of(EngramTray.INSTANCE, InfusedCraftingTray.INSTANCE);
    }

    @Override
    public List<CodexModifierTable> codexModifierTables() {
        return List.of(BiomeModifiers.CODEX_TABLE, Engrams.CODEX_TABLE);
    }

    @Override
    public List<String> craftingBenchFeedstockIds() {
        return BiomeCraft.feedstockIds();
    }

    @Override
    public List<DomainMaterial> materialItems() {
        return Arrays.stream(InfusedFeedstock.values()).map(feedstock -> new DomainMaterial(feedstock.itemId(), InfusedFeedstock.TIER, feedstock.color())).toList();
    }

    @Override
    public List<ItemDataRules> itemDataRules() {
        return List.of(StarChartDataRules.INSTANCE);
    }

    @Override
    public List<BenchModeSupport> benchModes() {
        return List.of(StarChartCraftingBench.INSTANCE, ForgeCraftingBench.DERELICT_FORGE, ForgeCraftingBench.ABYSSAL_RESEARCH_COMPLEX);
    }

    @Override
    public List<CompatTarget> compatTargets() {
        return List.of(VaultsOfKnowledgeCompat.TARGET, FoilerCompat.TERRAIN_FX, FoilerCompat.HYDROFOIL, UnknownSkiesCompat.TARGET,
                RuthlessSectorCompat.TARGET, DiyPlanetsCompat.TARGET);
    }
}
