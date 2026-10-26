package exiledsector.ui;

import exiledlib.ModSettingsListener;
import exiledlib.compat.CombatGlow;
import exiledlib.i18n.I18n;
import exiledlib.i18n.LanguageSetting;
import exiledlib.i18n.Translation;
import exiledlib.skills.DiminishedStat;
import exiledlib.skills.DiminishingReturns;
import exiledlib.skills.skilleffect.onhit.MaxChainCountConfig;
import exiledsector.compat.SalvageSiteCompat;
import exiledsector.domain.DomainSettingsListener;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.items.CraftingMaterials;
import exiledsector.items.ItemDrops;
import exiledsector.items.ItemRoller;
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
        ModSettingsListener.ensureRegistered(DiminishingReturns.SettingsListener.class, DiminishingReturns.SettingsListener::new);
        ModSettingsListener.ensureRegistered(ItemRoller.SettingsListener.class, ItemRoller.SettingsListener::new);
        ItemRoller.reloadSettings();
        ModSettingsListener.ensureRegistered(ItemAnimationSetting.SettingsListener.class, ItemAnimationSetting.SettingsListener::new);
        ItemAnimationSetting.reloadSettings();
        ModSettingsListener.ensureRegistered(DomainSettingsListener.class, DomainSettingsListener::new);
        domains.forEach(ExiledSectorDomain::reloadSettings);
    }

    private static void registerInterface() {
        SettingsCreator.addBoolean(MOD_ID, RefitButtonConfig.FIELD_ID,
                Translation.text("settings.refit.buttonUnderHullMods.name"), Translation.text("settings.refit.buttonUnderHullMods.tooltip"),
                RefitButtonConfig.DEFAULT, MAIN_TAB);
        SettingsCreator.addKeybind(MOD_ID, RefitButtonConfig.BENCH_KEYBIND_FIELD_ID,
                Translation.text("settings.refit.benchKey.name"), Translation.text("settings.refit.benchKey.tooltip"),
                RefitButtonConfig.DEFAULT_BENCH_KEY, MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, SkillTreeSounds.ENABLED_FIELD_ID,
                Translation.text("settings.sounds.enabled.name"), Translation.text("settings.sounds.enabled.tooltip"),
                SkillTreeSounds.DEFAULT_ENABLED, MAIN_TAB);
        SettingsCreator.addRadio(MOD_ID, ItemAnimationSetting.FIELD_ID, Translation.text("settings.socketStorage.itemAnimations.name"),
                Translation.text("settings.socketStorage.itemAnimations.tooltip"), ItemAnimationSetting.FULL,
                String.join(",", ItemAnimationSetting.OPTIONS), MAIN_TAB);
    }

    private static void registerItems() {
        String itemsTab = itemsTab();
        SettingsCreator.addHeader(MOD_ID, "exiledSector_socketStorageHeader", Translation.text("settings.socketStorage.header"), itemsTab);
        SettingsCreator.addBoolean(MOD_ID, SalvageSiteCompat.DROPS_FIELD_ID,
                Translation.text("settings.socketStorage.otherModDrops.name"), Translation.text("settings.socketStorage.otherModDrops.tooltip"),
                SalvageSiteCompat.DEFAULT_DROPS, itemsTab);
        SettingsCreator.addBoolean(MOD_ID, ItemRoller.averageRollsFieldId(),
                Translation.text("settings.socketStorage.averageRolls.name"), Translation.text("settings.socketStorage.averageRolls.tooltip"),
                ItemRoller.DEFAULT_AVERAGE_ROLLS, itemsTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_craftingHeader", Translation.text("settings.crafting.header"), itemsTab);
        SettingsCreator.addInt(MOD_ID, ItemDrops.BATTLE_PARTS_FIELD_ID,
                Translation.text("settings.socketStorage.battleParts.name"), Translation.text("settings.socketStorage.battleParts.tooltip"),
                ItemDrops.DEFAULT_BATTLE_PARTS_PER_100_DP, 0, ItemDrops.MAX_BATTLE_PARTS_PER_100_DP, itemsTab);
        SettingsCreator.addBoolean(MOD_ID, CraftingMaterials.STORE_FIELD_ID,
                Translation.text("settings.socketStorage.storeMaterials.name"), Translation.text("settings.socketStorage.storeMaterials.tooltip"),
                CraftingMaterials.DEFAULT_STORE, itemsTab);
    }

    private static void registerCombat() {
        String combatTab = combatTab();
        SettingsCreator.addHeader(MOD_ID, "exiledSector_combatEffectsHeader", Translation.text("settings.combatEffects.header"), combatTab);
        SettingsCreator.addBoolean(MOD_ID, CombatGlow.enabledFieldId(),
                Translation.text("settings.combatEffects.graphicsLib.name"), Translation.text("settings.combatEffects.graphicsLib.tooltip"),
                CombatGlow.DEFAULT_ENABLED, combatTab);
        SettingsCreator.addInt(MOD_ID, MaxChainCountConfig.fieldId(),
                Translation.text("settings.energyChain.maxChainCount.name"), Translation.text("settings.energyChain.maxChainCount.tooltip"),
                MaxChainCountConfig.DEFAULT, 1, 20, combatTab);

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
