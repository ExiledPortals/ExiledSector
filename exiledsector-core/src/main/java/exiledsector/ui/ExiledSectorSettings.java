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
import exiledsector.items.CraftingMaterials;
import exiledsector.items.ItemDrops;
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
            SettingsCreator.addHeader(MOD_ID, "exiledSector_header", Translation.text("settings.interface.header"), MAIN_TAB);
            SettingsCreator.addRadio(MOD_ID, LanguageSetting.fieldId(), Translation.text("settings.language.name"),
                    Translation.text("settings.language.tooltip"), LanguageSetting.AUTO, String.join(",", LanguageSetting.OPTIONS), MAIN_TAB);
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
        ModSettingsListener.onReload(ExiledSectorDomain.class, () -> ExiledSector.domains().forEach(ExiledSectorDomain::reloadSettings));
        domains.forEach(ExiledSectorDomain::reloadSettings);
    }

    private static void registerInterface() {
        SettingsCreator.addBoolean(MOD_ID, RefitButtonConfig.FIELD_ID,
                Translation.text("settings.refit.buttonUnderHullMods.name"), Translation.text("settings.refit.buttonUnderHullMods.tooltip"),
                RefitButtonConfig.DEFAULT, MAIN_TAB);
        SettingsCreator.addKeybind(MOD_ID, RefitButtonConfig.BENCH_KEYBIND_FIELD_ID,
                Translation.text("settings.refit.craftingBenchKey.name"), Translation.text("settings.refit.craftingBenchKey.tooltip"),
                RefitButtonConfig.DEFAULT_BENCH_KEY, MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, ModSounds.ENABLED_FIELD_ID,
                Translation.text("settings.sounds.enabled.name"), Translation.text("settings.sounds.enabled.tooltip"),
                ModSounds.DEFAULT_ENABLED, MAIN_TAB);
        SettingsCreator.addRadio(MOD_ID, ItemAnimationSetting.FIELD_ID, Translation.text("settings.hullItems.itemAnimations.name"),
                Translation.text("settings.hullItems.itemAnimations.tooltip"), ItemAnimationSetting.FULL,
                String.join(",", ItemAnimationSetting.OPTIONS), MAIN_TAB);
    }

    private static void registerItems() {
        String itemsTab = itemsTab();
        SettingsCreator.addHeader(MOD_ID, "exiledSector_socketStorageHeader", Translation.text("settings.hullItems.header"), itemsTab);
        SettingsCreator.addBoolean(MOD_ID, SalvageSiteCompat.DROPS_FIELD_ID,
                Translation.text("settings.hullItems.otherModDrops.name"), Translation.text("settings.hullItems.otherModDrops.tooltip"),
                SalvageSiteCompat.DEFAULT_DROPS, itemsTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_craftingHeader", Translation.text("settings.crafting.header"), itemsTab);
        SettingsCreator.addInt(MOD_ID, ItemDrops.BATTLE_FEEDSTOCK_FIELD_ID,
                Translation.text("settings.hullItems.battleFeedstock.name"), Translation.text("settings.hullItems.battleFeedstock.tooltip"),
                ItemDrops.DEFAULT_BATTLE_FEEDSTOCK_PER_100_DP, 0, ItemDrops.MAX_BATTLE_PARTS_PER_100_DP, itemsTab);
        SettingsCreator.addBoolean(MOD_ID, CraftingMaterials.STORE_FIELD_ID,
                Translation.text("settings.hullItems.storeMaterials.name"), Translation.text("settings.hullItems.storeMaterials.tooltip"),
                CraftingMaterials.DEFAULT_STORE, itemsTab);
    }

    private static void registerCombat() {
        String combatTab = combatTab();
        SettingsCreator.addHeader(MOD_ID, "exiledSector_combatEffectsHeader", Translation.text("settings.combatEffects.header"), combatTab);
        SettingsCreator.addBoolean(MOD_ID, CombatGlow.enabledFieldId(),
                Translation.text("settings.combatEffects.graphicsLib.name"), Translation.text("settings.combatEffects.graphicsLib.tooltip"),
                CombatGlow.DEFAULT_ENABLED, combatTab);
        SettingsCreator.addInt(MOD_ID, MaxRefractionCountConfig.fieldId(),
                Translation.text("settings.energyChain.maxChainCount.name"), Translation.text("settings.energyChain.maxChainCount.tooltip"),
                MaxRefractionCountConfig.DEFAULT, 1, 20, combatTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_diminishingHeader", Translation.text("settings.diminishing.header"), combatTab);
        SettingsCreator.addText(MOD_ID, "exiledSector_diminishingAbout", Translation.text("settings.diminishing.about"), combatTab);
        SettingsCreator.addInt(MOD_ID, DiminishingReturns.efficiencyFieldId(),
                Translation.text("settings.diminishing.efficiency.name"), Translation.text("settings.diminishing.efficiency.tooltip"),
                DiminishingReturns.DEFAULT_EFFICIENCY_PERCENT, 0, 100, combatTab);
        for (DiminishedStat stat : DiminishedStat.values()) {
            SettingsCreator.addInt(MOD_ID, stat.thresholdFieldId(),
                    Translation.text("settings.diminishing.threshold." + stat.name()), "",
                    stat.defaultThresholdPercent(), 0, DiminishingReturns.MAX_THRESHOLD_PERCENT, combatTab);
        }
    }
}
