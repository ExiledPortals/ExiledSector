package exiledsector;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.thoughtworks.xstream.XStream;
import exiledsector.charts.ChartSaveAliases;
import exiledsector.charts.StarCharts;
import exiledsector.compat.AbyssSingularity;
import exiledsector.compat.CompatChecks;
import exiledsector.compat.GraphicsLibCompat;
import exiledsector.compat.NightcrossCompat;
import exiledsector.compat.NightcrossUnlocks;
import exiledsector.compat.RandomAssortmentCompat;
import exiledsector.effects.NpcBonusScale;
import exiledsector.effects.NpcFleetDialogListener;
import exiledsector.effects.NpcFleetInflationListener;
import exiledsector.effects.NpcFleetSweepScript;
import exiledsector.effects.OpReserveHullMods;
import exiledsector.effects.PhantomConflictWatch;
import exiledsector.effects.PhantomHullMods;
import exiledsector.effects.PlayerEngagementPipeline;
import exiledsector.effects.PlayerTreePower;
import exiledsector.effects.ResolvedTree;
import exiledsector.effects.SkillConflictWarningHullMod;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.effects.SkillTreeInstaller;
import exiledsector.effects.ItemLootListener;
import exiledsector.effects.ItemSalvageListener;
import exiledsector.effects.TechMiningItemListener;
import exiledsector.i18n.I18n;
import exiledsector.i18n.LanguageSetting;
import exiledsector.i18n.Languages;
import exiledsector.i18n.Translation;
import exiledsector.persistence.RetiredReserveIds;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.persistence.SkillTreeTemplateStore;
import exiledsector.skills.ShipSections;
import exiledsector.skills.LearnedPhantomConflicts;
import exiledsector.skills.NodeReplacements;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.NpcFactionVolumes;
import exiledsector.skills.skilleffect.CsvIdList;
import exiledsector.skills.skilleffect.EffectAliases;
import exiledsector.skills.skilleffect.FleetCrewLedger;
import exiledsector.skills.skilleffect.FleetWideEffects;
import exiledsector.skills.tags.AreaToggles;
import exiledsector.skills.unlock.NodeGates;
import exiledsector.items.ItemCraftingCosts;
import exiledsector.items.ItemImplicits;
import exiledsector.items.ItemDataIntegrity;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ItemIdAliases;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.MaterialItems;
import exiledsector.items.MaterialMigration;
import exiledsector.items.ItemNames;
import exiledsector.items.NpcItems;
import exiledsector.items.ItemDrops;
import exiledsector.items.SocketCustody;
import exiledsector.items.SocketLossListener;
import exiledsector.items.ItemSaveAliases;
import exiledsector.items.ItemStore;
import exiledsector.items.SocketTypeMigration;
import exiledsector.ui.ExiledSectorSettings;
import exiledsector.ui.HullUpgradesRefitButton;
import exiledsector.ui.SkillTreeRefitButton;
import exiledsector.ui.TreeCameraMemory;
import exiledsector.ui.inspect.NpcTreeInspectInput;
import exiledsector.ui.inspect.SkillTreeCodexListener;
import exiledsector.ui.inspect.ItemCodex;
import exiledsector.ui.refit.PhantomHullModRefitHider;
import exiledsector.ui.util.TexturePreloader;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public class ExiledSectorModPlugin extends BaseModPlugin {

    public static final String LOG_TAG = "ExiledSector";
    public static final String MOD_ID = "exiledSector";
    static final String SHIP_SKILL_DATA_ALIAS = "exiledSector.ShipSkillData";
    public static final List<String> LOCALISED_HULLMODS = List.of(SkillTreeHullMod.ID, SkillConflictWarningHullMod.ID);

    @Override
    public void onApplicationLoad() throws Exception {
        loadLanguage();
        Global.getLogger(ExiledSectorModPlugin.class).info(LOG_TAG + " loaded");
        localiseHullModText();
        SkillTreeRefitButton.addButton();
        HullUpgradesRefitButton.addButton();
        ExiledSectorSettings.register();
        EffectAliases.load();
        SkillTree.load();
        SkillTree.applyDisabledRegions(AreaToggles.disabledRegions());
        PhantomHullMods.install(phantomHullModIds());
        LearnedPhantomConflicts.load();
        CsvIdList.loadAll();
        NpcFactionVolumes.load();
        ItemIdAliases.load();
        ItemModifiers.load();
        ItemDefinitions.load();
        ItemImplicits.load();
        ItemDataIntegrity.logLoaded();
        ItemCraftingCosts.load();
        ItemNames.load();
        NodeReplacements.load();
        NodeGates.load();
        ItemDrops.load();
        CompatChecks.logAtStartup();
        GraphicsLibCompat.loadLightData();
        TexturePreloader.preloadAll(MOD_ID);
    }

    private static Set<String> phantomHullModIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (SkillType type : SkillTree.getAllTypes().values()) {
            ids.addAll(type.getPhantomHullModIds());
        }
        return ids;
    }

    private static void loadLanguage() {
        Locale jvmLocale = Locale.getDefault();
        boolean gameRendersCjk = LanguageSetting.gameRendersCjk(jvmLocale);
        I18n.load(LanguageSetting.resolve(LanguageSetting.AUTO, jvmLocale, gameRendersCjk));
        ExiledSectorSettings.registerLanguage();
        Languages chosenLanguages = LanguageSetting.resolve(LanguageSetting.selected(), jvmLocale, gameRendersCjk);
        if (!chosenLanguages.equals(I18n.languages())) {
            I18n.load(chosenLanguages);
        }
    }

    private static void localiseHullModText() {
        I18n.forGameText(() -> {
            LOCALISED_HULLMODS.forEach(ExiledSectorModPlugin::localise);
            MaterialItems.ids().forEach(ExiledSectorModPlugin::localiseMaterial);
        });
    }

    private static void localiseMaterial(String itemId) {
        SpecialItemSpecAPI materialSpec = MaterialItems.spec(itemId);
        if (materialSpec != null) {
            materialSpec.setName(Translation.data("material." + itemId + ".name", materialSpec.getName()));
            materialSpec.setDesc(Translation.data("material." + itemId + ".description", materialSpec.getDesc()));
        }
    }

    private static void localise(String hullModId) {
        HullModSpecAPI hullModSpec = Global.getSettings().getHullModSpec(hullModId);
        if (hullModSpec == null) {
            return;
        }
        hullModSpec.setDisplayName(Translation.data("hullmod." + hullModId + ".name", hullModSpec.getDisplayName()));
        hullModSpec.setDescriptionFormat(Translation.data("hullmod." + hullModId + ".description", hullModSpec.getDescriptionFormat()));
    }

    private static void forgetUnknownNodes() {
        if (!SkillTree.isLoadedCompletely()) {
            Global.getLogger(ExiledSectorModPlugin.class).warn(LOG_TAG + ": the skill tree did not load completely, so saved allocations were left as they are.");
            return;
        }
        ShipSkillDataManager.replaceRemovedNodes(SkillTree.getDeclaredNodes(), NodeReplacements.all());
        ShipSkillDataManager.forgetUnknownNodes(SkillTree.getAllNodes(), SkillTree.getDeclaredNodes(), SkillTree.getAllTypes(),
                new OwnedShips(), charged -> charged.refundTo(PlayerFleet.cargoOrNull()));
    }

    private static final class OwnedShips implements Predicate<String> {

        private Set<String> ownedShipIds;

        @Override
        public boolean test(String shipId) {
            if (ownedShipIds == null) {
                ownedShipIds = SocketCustody.ownedShipIds();
            }
            return ownedShipIds.contains(shipId);
        }
    }

    @Override
    public void onAboutToLinkCodexEntries() {
        ItemCodex.addEntries();
    }

    @Override
    public void configureXStream(XStream xStream) {
        ItemSaveAliases.register(xStream);
        RetiredReserveIds.register(xStream);
        ChartSaveAliases.register(xStream);
        xStream.alias(SHIP_SKILL_DATA_ALIAS, ShipSkillData.class);
    }

    @Override
    public void onGameLoad(boolean newGame) {
        SkillTree.applyDisabledRegions(AreaToggles.disabledRegions());
        SkillDataResolver.clearCache();
        ResolvedTree.clearCache();
        PhantomConflictWatch.clearPendingReverts();
        TreeCameraMemory.forgetAll();
        FleetCrewLedger.discardPending();
        ShipSections.clearCache();
        NpcBonusScale.invalidate();
        NpcItems.clearCache();
        ItemStore.get().freezeNames();
        MaterialMigration.run(Global.getSector());
        OpReserveHullMods.forgetLegacySlots(Global.getSector().getPersistentData());
        ShipSkillDataManager.removeBlankRecords();
        forgetUnknownNodes();
        SkillTreeTemplateStore.pruneAssignments(ShipSkillDataManager::hasProgress);
        FleetWideEffects.markPhaseFieldStale();
        Global.getSector().removeScriptsOfClass(SkillTreeInstaller.class);
        Global.getSector().addTransientScript(new SkillTreeInstaller());
        Global.getSector().addTransientScript(new NpcFleetSweepScript());
        Global.getSector().addTransientListener(new NpcFleetDialogListener());
        Global.getSector().getListenerManager().addListener(new NpcFleetInflationListener(), true);
        Global.getSector().getListenerManager().addListener(new NpcTreeInspectInput(), true);
        Global.getSector().getListenerManager().addListener(new SkillTreeCodexListener(), true);
        Global.getSector().getListenerManager().addListener(new PhantomHullModRefitHider(), true);
        SocketLossListener socketLossListener = new SocketLossListener();
        Global.getSector().getListenerManager().addListener(socketLossListener, true);
        ItemLootListener itemLootListener = new ItemLootListener();
        Global.getSector().addTransientListener(itemLootListener);
        Global.getSector().getListenerManager().addListener(itemLootListener, true);
        Global.getSector().addTransientListener(PlayerEngagementPipeline.create(socketLossListener, itemLootListener));
        Global.getSector().getListenerManager().addListener(new ItemSalvageListener(), true);
        Global.getSector().getListenerManager().addListener(new TechMiningItemListener(), true);
        if (RandomAssortmentCompat.isModEnabled()) {
            AbyssSingularity.checkSaveOnLoad(Global.getSector());
            Global.getSector().addTransientListener(new AbyssSingularity.BattleListener());
        }
        if (NightcrossCompat.isModEnabled()) {
            NightcrossUnlocks.checkSaveOnLoad(Global.getSector());
            Global.getSector().getListenerManager().addListener(new NightcrossUnlocks.DiscoveryListener(), true);
        }
        SocketTypeMigration.run();
        SocketCustody.reconcile();
        StarCharts.onGameLoad(Global.getSector());
        PlayerTreePower.logNpcScaling();
    }
}
