package exiledsector.ui;

import exiledsector.ExiledSector;
import exiledsector.ModSettingsListener;
import exiledsector.ModSounds;
import exiledsector.compat.CombatGlow;
import exiledsector.i18n.I18n;
import exiledsector.i18n.LanguageSetting;
import exiledsector.i18n.Translation;
import exiledsector.skills.DiminishedStat;
import exiledsector.skills.DiminishingReturns;
import exiledsector.skills.skilleffect.onhit.MaxRefractionCountConfig;
import exiledsector.compat.SalvageSiteCompat;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.crafting.material.CraftingMaterials;
import exiledsector.ui.cargo.ItemAnimationSetting;
import exiledsector.ui.refit.RefitButtonConfig;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import java.util.List;

import static exiledsector.ExiledSector.MOD_ID;

public final class ExiledSectorSettings {

    public static final String MAIN_TAB = "";

    private ExiledSectorSettings() {
    }

    public static String itemsTab() {
        return Translation.text("settings.tab.items");
    }

    static String combatTab() {
        return Translation.text("settings.tab.combat");
    }

    public static void registerLanguage() {
        I18n.forGameText(() -> {
            SettingsPage mainPage = new SettingsPage(MAIN_TAB);
            mainPage.header("exiledSector_header", "settings.interface.header");
            mainPage.radioSetting(LanguageSetting.fieldId(), "settings.language", LanguageSetting.AUTO, LanguageSetting.OPTIONS);
        });
        SettingsCreator.refresh(MOD_ID);
    }

    public static void register(List<? extends ExiledSectorDomain> domains) {
        I18n.forGameText(() -> {
            registerInterface();
            domains.forEach(ExiledSectorDomain::registerSettings);
            registerItems();
            registerCombat();
        });
        SettingsCreator.refresh(MOD_ID);
        ModSettingsListener.onReload(DiminishingReturns.class, DiminishingReturns::reloadSettings);
        ModSettingsListener.onReload(ItemAnimationSetting.class, ItemAnimationSetting::reloadSettings);
        ItemAnimationSetting.reloadSettings();
        ModSettingsListener.onReload(ModSounds.class, ModSounds::reloadSettings);
        ModSounds.reloadSettings();
        ModSettingsListener.onReload(TooltipTextSize.class, TooltipTextSize::reloadSettings);
        TooltipTextSize.reloadSettings();
        ModSettingsListener.onReload(ExiledSectorDomain.class, () -> ExiledSector.domains().forEach(ExiledSectorDomain::reloadSettings));
        domains.forEach(ExiledSectorDomain::reloadSettings);
    }

    private static void registerInterface() {
        SettingsPage mainPage = new SettingsPage(MAIN_TAB);
        mainPage.booleanSetting(RefitButtonConfig.FIELD_ID, "settings.refit.buttonUnderHullMods", RefitButtonConfig.DEFAULT);
        mainPage.keybindSetting(RefitButtonConfig.BENCH_KEYBIND_FIELD_ID, "settings.refit.craftingBenchKey", RefitButtonConfig.DEFAULT_BENCH_KEY);
        mainPage.keybindSetting(RefitButtonConfig.SKILL_TREE_KEYBIND_FIELD_ID, "settings.refit.skillTreeKey",
                RefitButtonConfig.DEFAULT_SKILL_TREE_KEY);
        mainPage.keybindSetting(RefitButtonConfig.HULL_ITEMS_KEYBIND_FIELD_ID, "settings.refit.hullItemsKey",
                RefitButtonConfig.DEFAULT_HULL_ITEMS_KEY);
        mainPage.booleanSetting(ModSounds.ENABLED_FIELD_ID, "settings.sounds.enabled", ModSounds.DEFAULT_ENABLED);
        mainPage.booleanSetting(TooltipTextSize.LARGE_FIELD_ID, "settings.largeTooltipText", TooltipTextSize.DEFAULT_LARGE);
        mainPage.radioSetting(ItemAnimationSetting.FIELD_ID, "settings.hullItems.itemAnimations", ItemAnimationSetting.FULL,
                ItemAnimationSetting.OPTIONS);
    }

    private static void registerItems() {
        SettingsPage itemsPage = new SettingsPage(itemsTab());
        itemsPage.header("exiledSector_socketStorageHeader", "settings.hullItems.header");
        itemsPage.booleanSetting(SalvageSiteCompat.DROPS_FIELD_ID, "settings.hullItems.otherModDrops", SalvageSiteCompat.DEFAULT_DROPS);

        itemsPage.header("exiledSector_craftingHeader", "settings.crafting.header");
        itemsPage.booleanSetting(CraftingMaterials.STORE_FIELD_ID, "settings.hullItems.storeMaterials", CraftingMaterials.DEFAULT_STORE);
    }

    private static void registerCombat() {
        SettingsPage combatPage = new SettingsPage(combatTab());
        combatPage.header("exiledSector_combatEffectsHeader", "settings.combatEffects.header");
        combatPage.booleanSetting(CombatGlow.enabledFieldId(), "settings.combatEffects.graphicsLib", CombatGlow.DEFAULT_ENABLED);
        combatPage.intSetting(MaxRefractionCountConfig.fieldId(), "settings.energyChain.maxChainCount", MaxRefractionCountConfig.DEFAULT, 1, 20);

        combatPage.header("exiledSector_diminishingHeader", "settings.diminishing.header");
        combatPage.text("exiledSector_diminishingAbout", "settings.diminishing.about");
        combatPage.intSetting(DiminishingReturns.efficiencyFieldId(), "settings.diminishing.efficiency",
                DiminishingReturns.DEFAULT_EFFICIENCY_PERCENT, 0, 100);
        for (DiminishedStat stat : DiminishedStat.values()) {
            combatPage.intSetting(stat.thresholdFieldId(), SettingsPage.Label.untipped("settings.diminishing.threshold." + stat.name()),
                    stat.defaultThresholdPercent(), 0, DiminishingReturns.MAX_THRESHOLD_PERCENT);
        }
    }
}
