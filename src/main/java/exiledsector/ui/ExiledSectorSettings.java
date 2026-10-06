package exiledsector.ui;

import exiledsector.compat.SalvageSiteCompat;
import exiledsector.i18n.I18n;
import exiledsector.i18n.LanguageSetting;
import exiledsector.i18n.Translation;
import exiledsector.skills.npc.NpcLevelTable;
import exiledsector.skills.npc.NpcTreeConfig;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.skilleffect.MaxChainCountConfig;
import exiledsector.skills.tags.AreaToggles;
import exiledsector.skills.unlock.HiddenNodeDisplayConfig;
import exiledsector.skills.unlock.UnlockConditionOverrides;
import exiledsector.ui.inspect.NpcInspectConfig;
import exiledsector.ui.refit.RefitButtonConfig;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import static exiledsector.ExiledSectorModPlugin.MOD_ID;

public final class ExiledSectorSettings {

    private static final String MAIN_TAB = "";

    private ExiledSectorSettings() {
    }

    static String npcScalingTab() {
        return Translation.text("settings.tab.npcScaling");
    }

    public static void registerLanguage() {
        I18n.forGameText(() -> {
            SettingsCreator.addHeader(MOD_ID, "exiledSector_header", Translation.text("settings.header"), MAIN_TAB);
            SettingsCreator.addRadio(MOD_ID, LanguageSetting.FIELD_ID, Translation.text("settings.language.name"),
                    Translation.text("settings.language.tooltip"), LanguageSetting.AUTO, String.join(",", LanguageSetting.OPTIONS), MAIN_TAB);
        });
        SettingsCreator.refresh(MOD_ID);
    }

    public static void register() {
        I18n.forGameText(() -> {
            registerGeneral();
            registerNpcScaling();
        });
        SettingsCreator.refresh(MOD_ID);
    }

    private static void registerGeneral() {
        SettingsCreator.addText(MOD_ID, "exiledSector_about", Translation.text("settings.about"), MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_refitHeader", Translation.text("settings.refit.header"), MAIN_TAB);
        SettingsCreator.addText(MOD_ID, "exiledSector_refitAbout", Translation.text("settings.refit.about"), MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, RefitButtonConfig.FIELD_ID,
                Translation.text("settings.refit.buttonUnderHullMods.name"), Translation.text("settings.refit.buttonUnderHullMods.tooltip"),
                RefitButtonConfig.DEFAULT, MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_socketStorageHeader", Translation.text("settings.socketStorage.header"), MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, SalvageSiteCompat.DROPS_FIELD_ID,
                Translation.text("settings.socketStorage.otherModDrops.name"), Translation.text("settings.socketStorage.otherModDrops.tooltip"),
                SalvageSiteCompat.DEFAULT_DROPS, MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_opCostHeader", Translation.text("settings.opCost.header"), MAIN_TAB);
        SettingsCreator.addText(MOD_ID, "exiledSector_opCostAbout", Translation.text("settings.opCost.about"), MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, SkillNodeOpCost.FRIGATE_FIELD_ID,
                Translation.text("settings.opCost.frigate"), "", SkillNodeOpCost.DEFAULT_FRIGATE, 0, 100, MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, SkillNodeOpCost.DESTROYER_FIELD_ID,
                Translation.text("settings.opCost.destroyer"), "", SkillNodeOpCost.DEFAULT_DESTROYER, 0, 100, MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, SkillNodeOpCost.CRUISER_FIELD_ID,
                Translation.text("settings.opCost.cruiser"), "", SkillNodeOpCost.DEFAULT_CRUISER, 0, 100, MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, SkillNodeOpCost.CAPITAL_FIELD_ID,
                Translation.text("settings.opCost.capital"), "", SkillNodeOpCost.DEFAULT_CAPITAL, 0, 100, MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, SkillNodeOpCost.UNDEFINED_FIELD_ID,
                Translation.text("settings.opCost.other"), "", SkillNodeOpCost.DEFAULT_UNDEFINED, 0, 100, MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_levelHeader", Translation.text("settings.level.header"), MAIN_TAB);
        SettingsCreator.addText(MOD_ID, "exiledSector_levelAbout", Translation.text("settings.level.about"), MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.MAX_LEVEL_FIELD_ID,
                Translation.text("settings.level.maxLevel"), "", ShipLevelConfig.DEFAULT_MAX_LEVEL, 1, 200, MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.XP_BASE_FIELD_ID,
                Translation.text("settings.level.xpBase"), "", ShipLevelConfig.DEFAULT_XP_BASE, 1, 10000, MAIN_TAB);
        SettingsCreator.addDouble(MOD_ID, ShipLevelConfig.XP_GROWTH_FIELD_ID,
                Translation.text("settings.level.xpGrowth.name"), Translation.text("settings.level.xpGrowth.tooltip"),
                ShipLevelConfig.DEFAULT_XP_GROWTH, 1.0, 3.0, MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.XP_GROWTH_CUTOFF_LEVEL_FIELD_ID,
                Translation.text("settings.level.xpGrowthCutoff.name"), Translation.text("settings.level.xpGrowthCutoff.tooltip"),
                ShipLevelConfig.DEFAULT_XP_GROWTH_CUTOFF_LEVEL, 1, 200, MAIN_TAB);
        SettingsCreator.addDouble(MOD_ID, ShipLevelConfig.XP_PER_DEPLOYMENT_POINT_FIELD_ID,
                Translation.text("settings.level.xpPerDeploymentPoint"), "", ShipLevelConfig.DEFAULT_XP_PER_DEPLOYMENT_POINT,
                0.0, 100.0, MAIN_TAB);
        SettingsCreator.addDouble(MOD_ID, ShipLevelConfig.XP_LOSS_MULTIPLIER_FIELD_ID,
                Translation.text("settings.level.xpLossMultiplier"), "", ShipLevelConfig.DEFAULT_XP_LOSS_MULTIPLIER, 0.0, 1.0, MAIN_TAB);
        SettingsCreator.addDouble(MOD_ID, ShipLevelConfig.XP_DIFFICULTY_STRENGTH_FIELD_ID,
                Translation.text("settings.level.xpDifficultyStrength.name"), Translation.text("settings.level.xpDifficultyStrength.tooltip"),
                ShipLevelConfig.DEFAULT_XP_DIFFICULTY_STRENGTH, 0.0, 5.0, MAIN_TAB);
        SettingsCreator.addDouble(MOD_ID, ShipLevelConfig.XP_DIFFICULTY_MAX_MULTIPLIER_FIELD_ID,
                Translation.text("settings.level.xpDifficultyMaxMultiplier.name"),
                Translation.text("settings.level.xpDifficultyMaxMultiplier.tooltip"),
                ShipLevelConfig.DEFAULT_XP_DIFFICULTY_MAX_MULTIPLIER, 1.0, 20.0, MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.MAX_ALLOCATED_NODES_FIELD_ID,
                Translation.text("settings.level.maxAllocatedNodes.name"), Translation.text("settings.level.maxAllocatedNodes.tooltip"),
                ShipLevelConfig.DEFAULT_MAX_ALLOCATED_NODES, 1, 500, MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_hiddenNodesHeader", Translation.text("settings.hiddenNodes.header"), MAIN_TAB);
        SettingsCreator.addText(MOD_ID, "exiledSector_hiddenNodesAbout", Translation.text("settings.hiddenNodes.about"), MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, HiddenNodeDisplayConfig.SHOW_HIDDEN_NODES_FIELD_ID,
                Translation.text("settings.hiddenNodes.show.name"), Translation.text("settings.hiddenNodes.show.tooltip"),
                HiddenNodeDisplayConfig.DEFAULT_SHOW_HIDDEN_NODES, MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_unlockConditionsHeader", Translation.text("settings.unlock.header"), MAIN_TAB);
        SettingsCreator.addText(MOD_ID, "exiledSector_unlockConditionsAbout", Translation.text("settings.unlock.about"), MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, UnlockConditionOverrides.DISABLE_BLUEPRINT_FIELD_ID,
                Translation.text("settings.unlock.blueprint"), "", UnlockConditionOverrides.DEFAULT_DISABLED, MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, UnlockConditionOverrides.DISABLE_CHARACTER_STAT_FIELD_ID,
                Translation.text("settings.unlock.characterStat"), "", UnlockConditionOverrides.DEFAULT_DISABLED, MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, UnlockConditionOverrides.DISABLE_MIN_SHIP_LEVEL_FIELD_ID,
                Translation.text("settings.unlock.minShipLevel"), "", UnlockConditionOverrides.DEFAULT_DISABLED, MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, UnlockConditionOverrides.DISABLE_MEMORY_FLAG_FIELD_ID,
                Translation.text("settings.unlock.gameState"), "", UnlockConditionOverrides.DEFAULT_DISABLED, MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_areasHeader", Translation.text("settings.areas.header"), MAIN_TAB);
        SettingsCreator.addText(MOD_ID, "exiledSector_areasAbout", Translation.text("settings.areas.about"), MAIN_TAB);
        SettingsCreator.addRadio(MOD_ID, AreaToggles.LOST_SECTOR_FIELD_ID, Translation.text("settings.areas.lostSector.name"),
                Translation.text("settings.areas.lostSector.tooltip"), AreaToggles.AUTO, String.join(",", AreaToggles.OPTIONS), MAIN_TAB);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_energyChainHeader", Translation.text("settings.energyChain.header"), MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, MaxChainCountConfig.FIELD_ID,
                Translation.text("settings.energyChain.maxChainCount.name"), Translation.text("settings.energyChain.maxChainCount.tooltip"),
                MaxChainCountConfig.DEFAULT, 1, 20, MAIN_TAB);
    }

    private static void registerNpcScaling() {
        String tab = npcScalingTab();
        SettingsCreator.addHeader(MOD_ID, "exiledSector_npcTreesHeader", Translation.text("settings.npc.header"), tab);
        SettingsCreator.addText(MOD_ID, "exiledSector_npcTreesAbout", Translation.text("settings.npc.about"), tab);
        SettingsCreator.addBoolean(MOD_ID, NpcTreeConfig.ENABLED_FIELD_ID,
                Translation.text("settings.npc.enabled.name"), Translation.text("settings.npc.enabled.tooltip"),
                NpcTreeConfig.DEFAULT_ENABLED, tab);
        SettingsCreator.addBoolean(MOD_ID, NpcTreeConfig.OFFICERED_SHIPS_FIELD_ID,
                Translation.text("settings.npc.officeredShips.name"), Translation.text("settings.npc.officeredShips.tooltip"),
                NpcTreeConfig.DEFAULT_OFFICERED_SHIPS, tab);
        SettingsCreator.addBoolean(MOD_ID, NpcTreeConfig.FLAGSHIP_FIELD_ID,
                Translation.text("settings.npc.flagship.name"), Translation.text("settings.npc.flagship.tooltip"),
                NpcTreeConfig.DEFAULT_FLAGSHIP, tab);
        SettingsCreator.addInt(MOD_ID, NpcTreeConfig.OTHER_SHIP_CHANCE_FIELD_ID,
                Translation.text("settings.npc.otherShipChance.name"), Translation.text("settings.npc.otherShipChance.tooltip"),
                NpcTreeConfig.DEFAULT_OTHER_SHIP_CHANCE_PERCENT, 0, 100, tab);
        SettingsCreator.addKeybind(MOD_ID, NpcInspectConfig.KEYBIND_FIELD_ID,
                Translation.text("settings.npc.inspectKey.name"), Translation.text("settings.npc.inspectKey.tooltip"),
                NpcInspectConfig.DEFAULT_KEY, tab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_npcNodesHeader", Translation.text("settings.npcNodes.header"), tab);
        SettingsCreator.addText(MOD_ID, "exiledSector_npcNodesAbout", Translation.text("settings.npcNodes.about"), tab);
        for (int level = NpcLevelTable.MIN_PLAYER_LEVEL; level <= NpcLevelTable.MAX_PLAYER_LEVEL; level++) {
            boolean andAbove = level == NpcLevelTable.MAX_PLAYER_LEVEL;
            SettingsCreator.addHeader(MOD_ID, "exiledSector_npcLevel" + level + "Header",
                    Translation.msg(andAbove ? "settings.npcNodes.levelHeaderAndAbove" : "settings.npcNodes.levelHeader")
                            .arg("level", level).text(), tab);
            SettingsCreator.addInt(MOD_ID, NpcLevelTable.minNodesFieldId(level), Translation.text("settings.npcNodes.min.name"),
                    Translation.msg(andAbove ? "settings.npcNodes.min.tooltipAndAbove" : "settings.npcNodes.min.tooltip")
                            .arg("level", level).text(),
                    NpcLevelTable.defaultMinNodes(level), NpcLevelTable.MIN_NODES, NpcLevelTable.MAX_NODES, tab);
            SettingsCreator.addInt(MOD_ID, NpcLevelTable.maxNodesFieldId(level), Translation.text("settings.npcNodes.max.name"),
                    Translation.msg(andAbove ? "settings.npcNodes.max.tooltipAndAbove" : "settings.npcNodes.max.tooltip")
                            .arg("level", level).text(),
                    NpcLevelTable.defaultMaxNodes(level), NpcLevelTable.MIN_NODES, NpcLevelTable.MAX_NODES, tab);
        }
    }
}
