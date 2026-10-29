package exiledsector.ui;

import exiledsector.ModSettingsListener;
import exiledsector.effects.ModuleShareSetting;
import exiledsector.effects.NpcBonusScale;
import exiledsector.i18n.Translation;
import exiledsector.items.AnchorJanusGate;
import exiledsector.items.UniqueSubroutinesSetting;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.npc.NpcTreeConfig;
import exiledsector.skills.npc.NpcTreeSize;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.tags.AreaToggles;
import exiledsector.skills.unlock.HiddenNodeDisplayConfig;
import exiledsector.skills.unlock.UnlockConditionOverrides;
import exiledsector.ui.inspect.NpcInspectConfig;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import static exiledsector.ExiledSector.MOD_ID;

public final class SkillTreeSettings {

    private SkillTreeSettings() {
    }

    static String skillTreeTab() {
        return Translation.text("settings.tab.skillTree");
    }

    static String npcTab() {
        return Translation.text("settings.tab.npc");
    }

    public static void register() {
        registerSkillTree();
        registerNpc();
        ModSettingsListener.ensureRegistered(NpcBonusScale.SettingsListener.class, NpcBonusScale.SettingsListener::new);
    }

    private static void registerSkillTree() {
        String treeTab = skillTreeTab();
        SkillTreeSetting.SWITCH.addSettings("exiledSector_skillTreeHeader", "settings.skillTree.header",
                "settings.skillTree.enabled.name", "settings.skillTree.enabled.tooltip", treeTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_levelHeader", Translation.text("settings.level.header"), treeTab);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.MAX_LEVEL_FIELD_ID,
                Translation.text("settings.level.maxLevel"), "", ShipLevelConfig.DEFAULT_MAX_LEVEL, 1, 200, treeTab);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.XP_BASE_FIELD_ID,
                Translation.text("settings.level.xpBase"), "", ShipLevelConfig.DEFAULT_XP_BASE, 1, 10000, treeTab);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.XP_GROWTH_PERCENT_FIELD_ID,
                Translation.text("settings.level.xpGrowth.name"), Translation.text("settings.level.xpGrowth.tooltip"),
                ShipLevelConfig.DEFAULT_XP_GROWTH_PERCENT, 0, 200, treeTab);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.XP_GROWTH_CUTOFF_LEVEL_FIELD_ID,
                Translation.text("settings.level.xpGrowthCutoff.name"), Translation.text("settings.level.xpGrowthCutoff.tooltip"),
                ShipLevelConfig.DEFAULT_XP_GROWTH_CUTOFF_LEVEL, 1, 200, treeTab);
        SettingsCreator.addDouble(MOD_ID, ShipLevelConfig.XP_PER_DEPLOYMENT_POINT_FIELD_ID,
                Translation.text("settings.level.xpPerDeploymentPoint"), "", ShipLevelConfig.DEFAULT_XP_PER_DEPLOYMENT_POINT,
                0.0, 100.0, treeTab);
        SettingsCreator.addDouble(MOD_ID, ShipLevelConfig.XP_LOSS_MULTIPLIER_FIELD_ID,
                Translation.text("settings.level.xpLossMultiplier"), "", ShipLevelConfig.DEFAULT_XP_LOSS_MULTIPLIER, 0.0, 1.0, treeTab);
        SettingsCreator.addDouble(MOD_ID, ShipLevelConfig.XP_DIFFICULTY_STRENGTH_FIELD_ID,
                Translation.text("settings.level.xpDifficultyStrength.name"), Translation.text("settings.level.xpDifficultyStrength.tooltip"),
                ShipLevelConfig.DEFAULT_XP_DIFFICULTY_STRENGTH, 0.0, 5.0, treeTab);
        SettingsCreator.addDouble(MOD_ID, ShipLevelConfig.XP_DIFFICULTY_MAX_MULTIPLIER_FIELD_ID,
                Translation.text("settings.level.xpDifficultyMaxMultiplier.name"),
                Translation.text("settings.level.xpDifficultyMaxMultiplier.tooltip"),
                ShipLevelConfig.DEFAULT_XP_DIFFICULTY_MAX_MULTIPLIER, 1.0, 20.0, treeTab);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.CATCH_UP_PERCENT_PER_LEVEL_FIELD_ID,
                Translation.text("settings.level.catchUpPerLevel.name"), Translation.text("settings.level.catchUpPerLevel.tooltip"),
                ShipLevelConfig.DEFAULT_CATCH_UP_PERCENT_PER_LEVEL, 0, 100, treeTab);
        SettingsCreator.addDouble(MOD_ID, ShipLevelConfig.CATCH_UP_MAX_MULTIPLIER_FIELD_ID,
                Translation.text("settings.level.catchUpMaxMultiplier.name"), "",
                ShipLevelConfig.DEFAULT_CATCH_UP_MAX_MULTIPLIER, 1.0, 20.0, treeTab);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.LEVEL_FLOOR_PERCENT_FIELD_ID,
                Translation.text("settings.level.levelFloorPercent.name"), Translation.text("settings.level.levelFloorPercent.tooltip"),
                ShipLevelConfig.DEFAULT_LEVEL_FLOOR_PERCENT, 0, 100, treeTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_opCostHeader", Translation.text("settings.opCost.header"), treeTab);
        SettingsCreator.addInt(MOD_ID, ShipLevelConfig.MAX_ALLOCATED_NODES_FIELD_ID,
                Translation.text("settings.level.maxAllocatedNodes.name"), Translation.text("settings.level.maxAllocatedNodes.tooltip"),
                ShipLevelConfig.DEFAULT_MAX_ALLOCATED_NODES, 1, 500, treeTab);
        SettingsCreator.addInt(MOD_ID, SkillNodeOpCost.FRIGATE_FIELD_ID,
                Translation.text("settings.opCost.frigate"), "", SkillNodeOpCost.DEFAULT_FRIGATE, 0, 100, treeTab);
        SettingsCreator.addInt(MOD_ID, SkillNodeOpCost.DESTROYER_FIELD_ID,
                Translation.text("settings.opCost.destroyer"), "", SkillNodeOpCost.DEFAULT_DESTROYER, 0, 100, treeTab);
        SettingsCreator.addInt(MOD_ID, SkillNodeOpCost.CRUISER_FIELD_ID,
                Translation.text("settings.opCost.cruiser"), "", SkillNodeOpCost.DEFAULT_CRUISER, 0, 100, treeTab);
        SettingsCreator.addInt(MOD_ID, SkillNodeOpCost.CAPITAL_FIELD_ID,
                Translation.text("settings.opCost.capital"), "", SkillNodeOpCost.DEFAULT_CAPITAL, 0, 100, treeTab);
        SettingsCreator.addInt(MOD_ID, SkillNodeOpCost.UNDEFINED_FIELD_ID,
                Translation.text("settings.opCost.other"), "", SkillNodeOpCost.DEFAULT_UNDEFINED, 0, 100, treeTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_modulesHeader", Translation.text("settings.modules.header"), treeTab);
        SettingsCreator.addInt(MOD_ID, ModuleShareSetting.FIELD_ID,
                Translation.text("settings.modules.share.name"), Translation.text("settings.modules.share.tooltip"),
                ModuleShareSetting.DEFAULT_PERCENT, 0, ModuleShareSetting.MAX_PERCENT, treeTab);
        SettingsCreator.addInt(MOD_ID, ModuleShareSetting.STATION_FIELD_ID,
                Translation.text("settings.modules.stationShare.name"), Translation.text("settings.modules.stationShare.tooltip"),
                ModuleShareSetting.DEFAULT_STATION_PERCENT, 0, ModuleShareSetting.MAX_PERCENT, treeTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_unlockConditionsHeader", Translation.text("settings.unlock.header"), treeTab);
        SettingsCreator.addBoolean(MOD_ID, UnlockConditionOverrides.DISABLE_BLUEPRINT_FIELD_ID,
                Translation.text("settings.unlock.blueprint"), "", UnlockConditionOverrides.DEFAULT_DISABLED, treeTab);
        SettingsCreator.addBoolean(MOD_ID, UnlockConditionOverrides.DISABLE_CHARACTER_STAT_FIELD_ID,
                Translation.text("settings.unlock.characterStat"), "", UnlockConditionOverrides.DEFAULT_DISABLED, treeTab);
        SettingsCreator.addBoolean(MOD_ID, UnlockConditionOverrides.DISABLE_MIN_SHIP_LEVEL_FIELD_ID,
                Translation.text("settings.unlock.minShipLevel"), "", UnlockConditionOverrides.DEFAULT_DISABLED, treeTab);
        SettingsCreator.addBoolean(MOD_ID, UnlockConditionOverrides.DISABLE_MEMORY_FLAG_FIELD_ID,
                Translation.text("settings.unlock.gameState"), "", UnlockConditionOverrides.DEFAULT_DISABLED, treeTab);
        SettingsCreator.addBoolean(MOD_ID, HiddenNodeDisplayConfig.SHOW_HIDDEN_NODES_FIELD_ID,
                Translation.text("settings.hiddenNodes.show.name"), Translation.text("settings.hiddenNodes.show.tooltip"),
                HiddenNodeDisplayConfig.DEFAULT_SHOW_HIDDEN_NODES, treeTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_treeItemsHeader", Translation.text("settings.treeItems.header"), treeTab);
        SettingsCreator.addInt(MOD_ID, UniqueSubroutinesSetting.UNLOCK_LEVEL_FIELD_ID,
                Translation.text("settings.socketStorage.uniqueSubroutinesLevel.name"),
                Translation.text("settings.socketStorage.uniqueSubroutinesLevel.tooltip"),
                UniqueSubroutinesSetting.DEFAULT_UNLOCK_LEVEL, UniqueSubroutinesSetting.MIN_UNLOCK_LEVEL, UniqueSubroutinesSetting.MAX_UNLOCK_LEVEL,
                treeTab);
        SettingsCreator.addBoolean(MOD_ID, AnchorJanusGate.ALLOW_BEFORE_JANUS_FIELD_ID,
                Translation.text("settings.unlock.anchorsBeforeJanus"), Translation.text("settings.unlock.anchorsBeforeJanus.tooltip"),
                AnchorJanusGate.DEFAULT_ALLOW_BEFORE_JANUS, treeTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_areasHeader", Translation.text("settings.areas.header"), treeTab);
        SettingsCreator.addText(MOD_ID, "exiledSector_areasAbout", Translation.text("settings.areas.about"), treeTab);
        SettingsCreator.addRadio(MOD_ID, AreaToggles.LOST_SECTOR_FIELD_ID, Translation.text("settings.areas.lostSector.name"),
                Translation.text("settings.areas.lostSector.tooltip"), AreaToggles.AUTO, String.join(",", AreaToggles.OPTIONS), treeTab);
        SettingsCreator.addRadio(MOD_ID, AreaToggles.RANDOM_ASSORTMENT_FIELD_ID, Translation.text("settings.areas.randomAssortment.name"),
                Translation.text("settings.areas.randomAssortment.tooltip"), AreaToggles.AUTO, String.join(",", AreaToggles.REQUIRED_MOD_OPTIONS), treeTab);
        SettingsCreator.addRadio(MOD_ID, AreaToggles.NIGHTCROSS_FIELD_ID, Translation.text("settings.areas.nightcross.name"),
                Translation.text("settings.areas.nightcross.tooltip"), AreaToggles.AUTO, String.join(",", AreaToggles.OPTIONS), treeTab);
        SettingsCreator.addRadio(MOD_ID, AreaToggles.TANIA_FIELD_ID, Translation.text("settings.areas.tania.name"),
                Translation.text("settings.areas.tania.tooltip"), AreaToggles.AUTO, String.join(",", AreaToggles.REQUIRED_MOD_OPTIONS), treeTab);
    }

    private static void registerNpc() {
        String npcTab = npcTab();
        SettingsCreator.addHeader(MOD_ID, "exiledSector_npcTreesHeader", Translation.text("settings.npc.header"), npcTab);
        SettingsCreator.addText(MOD_ID, "exiledSector_npcTreesAbout", Translation.text("settings.npc.about"), npcTab);
        SettingsCreator.addBoolean(MOD_ID, NpcTreeConfig.ENABLED_FIELD_ID,
                Translation.text("settings.npc.enabled.name"), "", NpcTreeConfig.DEFAULT_ENABLED, npcTab);
        SettingsCreator.addBoolean(MOD_ID, NpcTreeConfig.OFFICERED_SHIPS_FIELD_ID,
                Translation.text("settings.npc.officeredShips.name"), "", NpcTreeConfig.DEFAULT_OFFICERED_SHIPS, npcTab);
        SettingsCreator.addBoolean(MOD_ID, NpcTreeConfig.FLAGSHIP_FIELD_ID,
                Translation.text("settings.npc.flagship.name"), "", NpcTreeConfig.DEFAULT_FLAGSHIP, npcTab);
        SettingsCreator.addInt(MOD_ID, NpcTreeConfig.OTHER_SHIP_CHANCE_FIELD_ID,
                Translation.text("settings.npc.otherShipChance.name"), Translation.text("settings.npc.otherShipChance.tooltip"),
                NpcTreeConfig.DEFAULT_OTHER_SHIP_CHANCE_PERCENT, 0, 100, npcTab);
        SettingsCreator.addBoolean(MOD_ID, NpcTreeConfig.STATIONS_FIELD_ID,
                Translation.text("settings.npc.stations.name"), Translation.text("settings.npc.stations.tooltip"),
                NpcTreeConfig.DEFAULT_STATIONS, npcTab);
        SettingsCreator.addKeybind(MOD_ID, NpcInspectConfig.KEYBIND_FIELD_ID,
                Translation.text("settings.npc.inspectKey.name"), Translation.text("settings.npc.inspectKey.tooltip"),
                NpcInspectConfig.DEFAULT_KEY, npcTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_npcTreeSizeHeader", Translation.text("settings.npcTreeSize.header"), npcTab);
        SettingsCreator.addText(MOD_ID, "exiledSector_npcTreeSizeAbout", Translation.text("settings.npcTreeSize.about"), npcTab);
        SettingsCreator.addInt(MOD_ID, NpcTreeSize.MIN_PERCENT_FIELD_ID,
                Translation.text("settings.npcTreeSize.min.name"), "",
                NpcTreeSize.DEFAULT_MIN_PERCENT, NpcTreeSize.LOWEST_PERCENT, NpcTreeSize.HIGHEST_PERCENT, npcTab);
        SettingsCreator.addInt(MOD_ID, NpcTreeSize.MAX_PERCENT_FIELD_ID,
                Translation.text("settings.npcTreeSize.max.name"), "",
                NpcTreeSize.DEFAULT_MAX_PERCENT, NpcTreeSize.LOWEST_PERCENT, NpcTreeSize.HIGHEST_PERCENT, npcTab);
        SettingsCreator.addRadio(MOD_ID, NpcTreeConfig.LEVEL_MODE_FIELD_ID, Translation.text("settings.npcTreeSize.mode.name"),
                Translation.text("settings.npcTreeSize.mode.tooltip"), NpcTreeConfig.YOUR_SHIPS,
                String.join(",", NpcTreeConfig.LEVEL_MODE_OPTIONS), npcTab);
        SettingsCreator.addInt(MOD_ID, NpcTreeConfig.FIXED_MIN_PERCENT_FIELD_ID,
                Translation.text("settings.npcTreeSize.fixedMin.name"), Translation.text("settings.npcTreeSize.fixedMin.tooltip"),
                NpcTreeConfig.DEFAULT_FIXED_MIN_PERCENT, 0, 100, npcTab);
        SettingsCreator.addBoolean(MOD_ID, NpcTreeConfig.RECOVERED_START_BLANK_FIELD_ID,
                Translation.text("settings.npcTreeSize.startBlank.name"), Translation.text("settings.npcTreeSize.startBlank.tooltip"),
                NpcTreeConfig.DEFAULT_RECOVERED_START_BLANK, npcTab);

        SettingsCreator.addHeader(MOD_ID, "exiledSector_npcBonusScaleHeader", Translation.text("settings.npcBonusScale.header"), npcTab);
        SettingsCreator.addText(MOD_ID, "exiledSector_npcBonusScaleAbout", Translation.text("settings.npcBonusScale.about"), npcTab);
        SettingsCreator.addInt(MOD_ID, NpcBonusScale.LOW_TREE_POWER_FIELD_ID,
                Translation.text("settings.npcBonusScale.lowTreePower.name"), Translation.text("settings.npcBonusScale.lowTreePower.tooltip"),
                NpcBonusScale.DEFAULT_LOW_TREE_POWER, 0, 300, npcTab);
        SettingsCreator.addInt(MOD_ID, NpcBonusScale.HIGH_TREE_POWER_FIELD_ID,
                Translation.text("settings.npcBonusScale.highTreePower.name"), Translation.text("settings.npcBonusScale.highTreePower.tooltip"),
                NpcBonusScale.DEFAULT_HIGH_TREE_POWER, 1, 300, npcTab);
        SettingsCreator.addDouble(MOD_ID, NpcBonusScale.MAX_MULTIPLIER_FIELD_ID,
                Translation.text("settings.npcBonusScale.maxMultiplier.name"), Translation.text("settings.npcBonusScale.maxMultiplier.tooltip"),
                NpcBonusScale.DEFAULT_MAX_MULTIPLIER, 1.0, 10.0, npcTab);
    }
}
