package exiledsector;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.thoughtworks.xstream.XStream;
import exiledlib.HostMod;
import exiledsector.charts.StarChartsDomain;
import exiledsector.compat.CompatChecks;
import exiledlib.compat.CompatTarget;
import exiledlib.compat.GraphicsLibCompat;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.effects.ItemLootListener;
import exiledsector.effects.ItemSalvageListener;
import exiledsector.effects.MarketItemStock;
import exiledsector.effects.PlayerEngagementPipeline;
import exiledsector.effects.ResolvedTree;
import exiledsector.effects.SkillTreeHullMod;
import exiledsector.effects.TechMiningItemListener;
import exiledlib.i18n.I18n;
import exiledlib.i18n.LanguageSetting;
import exiledlib.i18n.Languages;
import exiledlib.i18n.SpecLocalisation;
import exiledsector.items.HullUpgradesDomain;
import exiledsector.items.ItemCraftingCosts;
import exiledsector.items.ItemDataIntegrity;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemIdAliases;
import exiledsector.items.ItemImplicits;
import exiledsector.items.ItemModifiers;
import exiledsector.items.ItemNames;
import exiledsector.items.ItemSaveAliases;
import exiledsector.items.ItemStore;
import exiledsector.items.MaterialItems;
import exiledsector.items.MaterialMigration;
import exiledsector.items.SocketCustody;
import exiledsector.items.SocketLossListener;
import exiledsector.items.SocketTypeMigration;
import exiledsector.items.UniqueSourceCheck;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillTreeDomain;
import exiledlib.skills.skilleffect.CsvIdList;
import exiledlib.skills.skilleffect.EffectAliases;
import exiledlib.skills.skilleffect.FleetCrewLedger;
import exiledlib.skills.skilleffect.FleetWideEffects;
import exiledsector.ui.ExiledSectorSettings;
import exiledsector.ui.ModificationBenchRefitButton;
import exiledsector.ui.cargo.CargoCraftingTabListener;
import exiledsector.ui.cargo.LootAlertListener;
import exiledsector.ui.util.TexturePreloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExiledSectorModPlugin extends BaseModPlugin {

    public static final HostMod HOST = HostMod.register(ExiledSector.HOST);
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
        loadLanguage();
        Global.getLogger(ExiledSectorModPlugin.class).info(ExiledSector.LOG_TAG + " loaded");
        localiseGameText();
        DOMAINS.forEach(ExiledSectorDomain::onApplicationLoad);
        ModificationBenchRefitButton.addButton();
        ExiledSectorSettings.register(DOMAINS);
        EffectAliases.load();
        CsvIdList.loadAll();
        DOMAINS.forEach(ExiledSectorDomain::loadData);
        ItemIdAliases.load();
        ItemModifiers.load();
        ItemDefinitions.load();
        ItemImplicits.load();
        DOMAINS.forEach(ExiledSectorDomain::loadItems);
        ItemDataIntegrity.logLoaded();
        ItemCraftingCosts.load();
        ItemNames.load();
        ItemDrops.load();
        UniqueSourceCheck.logUnreachableSources();
        CompatChecks.logAtStartup(compatTargets());
        GraphicsLibCompat.loadLightData(LIGHT_DATA_PATH);
        TexturePreloader.preloadAll();
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
    }

    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        ResolvedTree.clearCache();
        FleetCrewLedger.discardPending();
        List<CargoAPI> savedCargoes = MaterialMigration.savedCargoes(sector);
        DOMAINS.forEach(domain -> domain.onGameLoad(newGame, savedCargoes));
        ItemStore.get().freezeNames();
        MaterialMigration.run(savedCargoes);
        ShipSkillDataManager.removeBlankRecords();
        FleetWideEffects.markPhaseFieldStale();
        DOMAINS.forEach(ExiledSectorDomain::afterSaveRepair);
        sector.getListenerManager().addListener(new CargoCraftingTabListener(), true);
        SocketLossListener socketLossListener = new SocketLossListener();
        sector.getListenerManager().addListener(socketLossListener, true);
        ItemLootListener itemLootListener = new ItemLootListener();
        sector.addTransientListener(itemLootListener);
        sector.getListenerManager().addListener(itemLootListener, true);
        sector.addTransientListener(PlayerEngagementPipeline.create(socketLossListener, itemLootListener));
        sector.getListenerManager().addListener(new ItemSalvageListener(), true);
        sector.getListenerManager().addListener(new TechMiningItemListener(), true);
        sector.getListenerManager().addListener(new LootAlertListener(), true);
        sector.getListenerManager().addListener(new MarketItemStock(), true);
        DOMAINS.forEach(ExiledSectorDomain::afterLootListeners);
        SocketTypeMigration.run();
        SocketCustody.reconcile();
        DOMAINS.forEach(ExiledSectorDomain::afterSocketCustody);
    }
}
