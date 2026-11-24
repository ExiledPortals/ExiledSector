package exiledsector;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.thoughtworks.xstream.XStream;
import exiledsector.compat.CompatTarget;
import exiledsector.compat.GraphicsLibCompat;
import exiledsector.i18n.I18n;
import exiledsector.i18n.LanguageSetting;
import exiledsector.i18n.Languages;
import exiledsector.i18n.SpecLocalisation;
import exiledsector.items.drop.SalvageSiteDrops;
import exiledsector.skills.skilleffect.CsvIdList;
import exiledsector.skills.skilleffect.DamageTypeMults;
import exiledsector.skills.skilleffect.EffectAliases;
import exiledsector.skills.skilleffect.logistics.FleetCrewLedger;
import exiledsector.skills.skilleffect.logistics.FleetWideEffects;
import exiledsector.skills.skilleffect.logistics.PlayerFleetConditionScript;
import exiledsector.skills.skilleffect.logistics.RecoveryDModAvoidance;
import exiledsector.charts.StarChartsDomain;
import exiledsector.compat.CompatChecks;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.effects.ItemLootListener;
import exiledsector.effects.ItemSalvageListener;
import exiledsector.effects.MarketItemStock;
import exiledsector.effects.PlayerEngagementPipeline;
import exiledsector.effects.ResolvedTree;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.effects.TechMiningItemListener;
import exiledsector.harness.Harness;
import exiledsector.items.drop.CraftingDrops;
import exiledsector.items.hull.HullUpgradesDomain;
import exiledsector.items.crafting.InfusedCraftCosts;
import exiledsector.items.crafting.ItemCraftingCosts;
import exiledsector.items.ItemDataIntegrity;
import exiledsector.items.ItemDefinitions;
import exiledsector.loot.BattleLootRates;
import exiledsector.loot.BattleLootTrigger;
import exiledsector.loot.LootScreenTrigger;
import exiledsector.items.modifier.ItemImplicits;
import exiledsector.items.modifier.ItemModifiers;
import exiledsector.items.name.ItemNames;
import exiledsector.items.ItemIdAliases;
import exiledsector.items.ItemSaveAliases;
import exiledsector.items.ItemStore;
import exiledsector.items.crafting.material.MaterialItems;
import exiledsector.items.RetiredItems;
import exiledsector.items.custody.SavedCargo;
import exiledsector.items.custody.SocketCustody;
import exiledsector.items.custody.SocketLossListener;
import exiledsector.items.hull.SectionFitMigration;
import exiledsector.items.drop.UniqueSourceCheck;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTreeDomain;
import exiledsector.skills.tags.ThemeTags;
import exiledsector.ui.ExiledSectorSettings;
import exiledsector.ui.CraftingBenchRefitButton;
import exiledsector.ui.ShownScreens;
import exiledsector.ui.cargo.CargoCraftingTabListener;
import exiledsector.ui.cargo.LootAlertListener;
import exiledsector.ui.decoration.FleetHulls;
import exiledsector.ui.util.TexturePreloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExiledSectorModPlugin extends BaseModPlugin {

    public static final String LIGHT_DATA_PATH = "data/lights/exiledsector_lights.csv";
    static final String SHIP_SKILL_DATA_ALIAS = "exiledSector.ShipSkillData";
    public static final List<String> LOCALISED_HULLMODS = List.of(SkillTreeHullMod.ID);
    public static final List<ExiledSectorDomain> DOMAINS;

    static {
        ExiledSector.install(List.of(new SkillTreeDomain(), new HullUpgradesDomain(), new StarChartsDomain()));
        DOMAINS = ExiledSector.domains();
    }

    @Override
    public void onApplicationLoad() throws Exception {
        Harness.startIfLaunched();
        loadLanguage();
        Global.getLogger(ExiledSectorModPlugin.class).info(ExiledSector.LOG_TAG + " loaded");
        localiseGameText();
        DOMAINS.forEach(ExiledSectorDomain::onApplicationLoad);
        CraftingBenchRefitButton.addButton();
        ExiledSectorSettings.register(DOMAINS);
        EffectAliases.load();
        CsvIdList.loadAll();
        ThemeTags.load();
        DOMAINS.forEach(ExiledSectorDomain::loadData);
        ItemIdAliases.load();
        ItemModifiers.load();
        ItemDefinitions.load();
        ItemImplicits.load();
        DOMAINS.forEach(ExiledSectorDomain::loadItems);
        ItemDataIntegrity.logLoaded();
        ItemCraftingCosts.load();
        InfusedCraftCosts.load();
        ItemNames.load();
        SalvageSiteDrops.load();
        BattleLootRates.load();
        CraftingDrops.load();
        UniqueSourceCheck.logUnreachableSources();
        CompatChecks.logAtStartup(compatTargets());
        GraphicsLibCompat.loadLightData(LIGHT_DATA_PATH);
        TexturePreloader.preloadAll();
        FleetHulls.preloadHullFiles();
    }

    public static List<CompatTarget> compatTargets() {
        List<CompatTarget> targets = new ArrayList<>(CompatChecks.coreTargets());
        DOMAINS.forEach(domain -> targets.addAll(domain.compatTargets()));
        return targets;
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

    private static void localiseGameText() {
        I18n.forGameText(() -> {
            LOCALISED_HULLMODS.forEach(SpecLocalisation::hullMod);
            MaterialItems.ids().forEach(MaterialItems::localise);
        });
    }

    @Override
    public void onAboutToLinkCodexEntries() {
        DOMAINS.forEach(ExiledSectorDomain::addCodexEntries);
    }

    @Override
    public void configureXStream(XStream xStream) {
        ItemSaveAliases.register(xStream);
        DOMAINS.forEach(domain -> domain.configureXStream(xStream));
        xStream.alias(SHIP_SKILL_DATA_ALIAS, ShipSkillData.class);
        ShipSkillData.SAVED_FIELD_NAMES.forEach((fieldName, savedName) -> xStream.aliasField(savedName, ShipSkillData.class, fieldName));
    }

    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        ResolvedTree.clearCache();
        DamageTypeMults.restoreDefaults();
        FleetCrewLedger.discardPending();
        List<CargoAPI> savedCargoes = SavedCargo.cargoes(sector);
        DOMAINS.forEach(domain -> domain.onGameLoad(newGame, savedCargoes));
        ItemStore.get().freezeNames();
        RetiredItems.removeFrom(sector, savedCargoes);
        ShipSkillDataManager.removeBlankRecords();
        FleetWideEffects.markPhaseFieldStale();
        sector.addTransientScript(new PlayerFleetConditionScript());
        DOMAINS.forEach(ExiledSectorDomain::afterSaveRepair);
        sector.getListenerManager().addListener(new CargoCraftingTabListener(), true);
        SocketLossListener socketLossListener = new SocketLossListener();
        sector.getListenerManager().addListener(socketLossListener, true);
        ItemLootListener itemLootListener = new ItemLootListener();
        sector.addTransientListener(itemLootListener);
        sector.getListenerManager().addListener(itemLootListener, true);
        sector.addTransientListener(new BattleLootTrigger());
        sector.getListenerManager().addListener(new LootScreenTrigger(), true);
        sector.addTransientListener(PlayerEngagementPipeline.create(socketLossListener, itemLootListener));
        sector.getListenerManager().addListener(new ItemSalvageListener(), true);
        sector.getListenerManager().addListener(new TechMiningItemListener(), true);
        sector.getListenerManager().addListener(new LootAlertListener(), true);
        sector.getListenerManager().addListener(new MarketItemStock(), true);
        RecoveryDModAvoidance.install(sector);
        DOMAINS.forEach(ExiledSectorDomain::afterLootListeners);
        SectionFitMigration.run();
        SocketCustody.reconcile();
        DOMAINS.forEach(ExiledSectorDomain::afterSocketCustody);
        ShownScreens.clear();
        Harness.runIfLaunched(sector);
    }

    @Override
    public void beforeGameSave() {
        DOMAINS.forEach(ExiledSectorDomain::beforeGameSave);
    }
}
