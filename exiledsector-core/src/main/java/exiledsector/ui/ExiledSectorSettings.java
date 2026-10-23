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

    public static void registerLanguage() {
        I18n.forGameText(() -> {
            SettingsCreator.addHeader(MOD_ID, "exiledSector_header", Translation.text("settings.header"), MAIN_TAB);
            SettingsCreator.addRadio(MOD_ID, LanguageSetting.fieldId(), Translation.text("settings.language.name"),
                    Translation.text("settings.language.tooltip"), LanguageSetting.AUTO, String.join(",", LanguageSetting.OPTIONS), MAIN_TAB);
        });
        SettingsCreator.refresh(MOD_ID);
    }

    public static void register(List<? extends ExiledSectorDomain> domains) {
        I18n.forGameText(() -> {
            registerGeneral();
            domains.forEach(ExiledSectorDomain::registerSettings);
        });
        SettingsCreator.refresh(MOD_ID);
        ModSettingsListener.ensureRegistered(DiminishingReturns.SettingsListener.class, DiminishingReturns.SettingsListener::new);
        ModSettingsListener.ensureRegistered(ItemRoller.SettingsListener.class, ItemRoller.SettingsListener::new);
        ItemRoller.reloadSettings();
        ModSettingsListener.ensureRegistered(ItemAnimationSetting.SettingsListener.class, ItemAnimationSetting.SettingsListener::new);
        ItemAnimationSetting.reloadSettings();
        domains.forEach(ExiledSectorDomain::reloadSettings);
    }

    private static void registerGeneral() {
        SettingsCreator.addHeader(MOD_ID, "exiledSector_refitHeader", Translation.text("settings.refit.header"), MAIN_TAB);
        SettingsCreator.addText(MOD_ID, "exiledSector_refitAbout", Translation.text("settings.refit.about"), MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, RefitButtonConfig.FIELD_ID,
                Translation.text("settings.refit.buttonUnderHullMods.name"), Translation.text("settings.refit.buttonUnderHullMods.tooltip"),
                RefitButtonConfig.DEFAULT, MAIN_TAB);
        SettingsCreator.addKeybind(MOD_ID, RefitButtonConfig.BENCH_KEYBIND_FIELD_ID,
                Translation.text("settings.refit.benchKey.name"), Translation.text("settings.refit.benchKey.tooltip"),
                RefitButtonConfig.DEFAULT_BENCH_KEY, MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_soundsHeader", Translation.text("settings.sounds.header"), MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, SkillTreeSounds.ENABLED_FIELD_ID,
                Translation.text("settings.sounds.enabled.name"), Translation.text("settings.sounds.enabled.tooltip"),
                SkillTreeSounds.DEFAULT_ENABLED, MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_combatEffectsHeader", Translation.text("settings.combatEffects.header"), MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, CombatGlow.enabledFieldId(),
                Translation.text("settings.combatEffects.graphicsLib.name"), Translation.text("settings.combatEffects.graphicsLib.tooltip"),
                CombatGlow.DEFAULT_ENABLED, MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_socketStorageHeader", Translation.text("settings.socketStorage.header"), MAIN_TAB);
        SettingsCreator.addRadio(MOD_ID, ItemAnimationSetting.FIELD_ID, Translation.text("settings.socketStorage.itemAnimations.name"),
                Translation.text("settings.socketStorage.itemAnimations.tooltip"), ItemAnimationSetting.FULL,
                String.join(",", ItemAnimationSetting.OPTIONS), MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, SalvageSiteCompat.DROPS_FIELD_ID,
                Translation.text("settings.socketStorage.otherModDrops.name"), Translation.text("settings.socketStorage.otherModDrops.tooltip"),
                SalvageSiteCompat.DEFAULT_DROPS, MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, ItemDrops.BATTLE_PARTS_FIELD_ID,
                Translation.text("settings.socketStorage.battleParts.name"), Translation.text("settings.socketStorage.battleParts.tooltip"),
                ItemDrops.DEFAULT_BATTLE_PARTS_PER_100_DP, 0, ItemDrops.MAX_BATTLE_PARTS_PER_100_DP, MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, CraftingMaterials.STORE_FIELD_ID,
                Translation.text("settings.socketStorage.storeMaterials.name"), Translation.text("settings.socketStorage.storeMaterials.tooltip"),
                CraftingMaterials.DEFAULT_STORE, MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, ItemRoller.averageRollsFieldId(),
                Translation.text("settings.socketStorage.averageRolls.name"), Translation.text("settings.socketStorage.averageRolls.tooltip"),
                ItemRoller.DEFAULT_AVERAGE_ROLLS, MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_diminishingHeader", Translation.text("settings.diminishing.header"), MAIN_TAB);
        SettingsCreator.addText(MOD_ID, "exiledSector_diminishingAbout", Translation.text("settings.diminishing.about"), MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, DiminishingReturns.efficiencyFieldId(),
                Translation.text("settings.diminishing.efficiency.name"), Translation.text("settings.diminishing.efficiency.tooltip"),
                DiminishingReturns.DEFAULT_EFFICIENCY_PERCENT, 0, 100, MAIN_TAB);
        for (DiminishedStat stat : DiminishedStat.values()) {
            SettingsCreator.addInt(MOD_ID, stat.thresholdFieldId(),
                    Translation.text("settings.diminishing.threshold." + stat.name()),
                    Translation.text("settings.diminishing.threshold.tooltip"),
                    stat.defaultThresholdPercent(), 0, DiminishingReturns.MAX_THRESHOLD_PERCENT, MAIN_TAB);
        }

        SettingsCreator.addHeader(MOD_ID, "exiledSector_energyChainHeader", Translation.text("settings.energyChain.header"), MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, MaxChainCountConfig.fieldId(),
                Translation.text("settings.energyChain.maxChainCount.name"), Translation.text("settings.energyChain.maxChainCount.tooltip"),
                MaxChainCountConfig.DEFAULT, 1, 20, MAIN_TAB);
    }
}
