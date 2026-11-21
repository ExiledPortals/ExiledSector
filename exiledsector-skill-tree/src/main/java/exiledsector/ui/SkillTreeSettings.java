package exiledsector.ui;

import exiledsector.ModSettingsListener;
import exiledsector.effects.ModuleShareSetting;
import exiledsector.effects.NpcBonusScale;
import exiledsector.i18n.Translation;
import exiledsector.items.anchor.AnchorJanusGate;
import exiledsector.items.subroutine.UniqueSubroutinesSetting;
import exiledsector.skills.IntegrationCostSetting;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.npc.NpcLevelCategory;
import exiledsector.skills.npc.NpcTreeConfig;
import exiledsector.skills.npc.NpcTreeSize;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.progression.SkillNodeOpCost;
import exiledsector.skills.template.AutoAllocateSetting;
import exiledsector.skills.tags.VolumeToggles;
import exiledsector.skills.unlock.HiddenNodeDisplayConfig;
import exiledsector.skills.unlock.UnlockConditionOverrides;
import exiledsector.ui.inspect.NpcInspectConfig;

import static exiledsector.ui.SettingsPage.Label.untipped;

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
        ModSettingsListener.onReload(NpcBonusScale.class, NpcBonusScale::reloadSettings);
        ModSettingsListener.onReload(AutoAllocateSetting.class, AutoAllocateSetting::reloadSettings);
        AutoAllocateSetting.reloadSettings();
    }

    private static void registerSkillTree() {
        String treeTab = skillTreeTab();
        SettingsPage treePage = new SettingsPage(treeTab);
        SkillTreeSetting.SWITCH.addSettings("exiledSector_skillTreeHeader", "settings.skillTree.header",
                "settings.skillTree.enabled.name", "settings.skillTree.enabled.tooltip", treeTab);

        treePage.header("exiledSector_levelHeader", "settings.level.header");
        treePage.intSetting(ShipLevelConfig.MAX_LEVEL_FIELD_ID, untipped("settings.level.maxLevel"),
                ShipLevelConfig.DEFAULT_MAX_LEVEL, 1, 200);
        treePage.intSetting(ShipLevelConfig.XP_BASE_FIELD_ID, untipped("settings.level.xpBase"),
                ShipLevelConfig.DEFAULT_XP_BASE, 1, 10000);
        treePage.intSetting(ShipLevelConfig.XP_GROWTH_PERCENT_FIELD_ID, "settings.level.xpGrowth",
                ShipLevelConfig.DEFAULT_XP_GROWTH_PERCENT, 0, 200);
        treePage.intSetting(ShipLevelConfig.XP_GROWTH_CUTOFF_LEVEL_FIELD_ID, "settings.level.xpGrowthCutoff",
                ShipLevelConfig.DEFAULT_XP_GROWTH_CUTOFF_LEVEL, 0, 200);
        treePage.intSetting(ShipLevelConfig.XP_GROWTH_RAMP_START_LEVEL_FIELD_ID, "settings.level.xpGrowthRampStart",
                ShipLevelConfig.DEFAULT_XP_GROWTH_RAMP_START_LEVEL, 1, 200);
        treePage.doubleSetting(ShipLevelConfig.XP_GROWTH_RAMP_PERCENT_FIELD_ID, "settings.level.xpGrowthRamp",
                ShipLevelConfig.DEFAULT_XP_GROWTH_RAMP_PERCENT, 0.0, 10.0);
        treePage.doubleSetting(ShipLevelConfig.XP_PER_DEPLOYMENT_POINT_FIELD_ID, untipped("settings.level.xpPerDeploymentPoint"),
                ShipLevelConfig.DEFAULT_XP_PER_DEPLOYMENT_POINT, 0.0, 100.0);
        treePage.doubleSetting(ShipLevelConfig.XP_LOSS_MULTIPLIER_FIELD_ID, untipped("settings.level.xpLossMultiplier"),
                ShipLevelConfig.DEFAULT_XP_LOSS_MULTIPLIER, 0.0, 1.0);
        treePage.doubleSetting(ShipLevelConfig.XP_DIFFICULTY_STRENGTH_FIELD_ID, "settings.level.xpDifficultyStrength",
                ShipLevelConfig.DEFAULT_XP_DIFFICULTY_STRENGTH, 0.0, 5.0);
        treePage.doubleSetting(ShipLevelConfig.XP_DIFFICULTY_MAX_MULTIPLIER_FIELD_ID, "settings.level.xpDifficultyMaxMultiplier",
                ShipLevelConfig.DEFAULT_XP_DIFFICULTY_MAX_MULTIPLIER, 1.0, 20.0);
        treePage.intSetting(ShipLevelConfig.CATCH_UP_PERCENT_PER_LEVEL_FIELD_ID, "settings.level.catchUpPerLevel",
                ShipLevelConfig.DEFAULT_CATCH_UP_PERCENT_PER_LEVEL, 0, 100);
        treePage.doubleSetting(ShipLevelConfig.CATCH_UP_MAX_MULTIPLIER_FIELD_ID, untipped("settings.level.catchUpMaxMultiplier.name"),
                ShipLevelConfig.DEFAULT_CATCH_UP_MAX_MULTIPLIER, 1.0, 20.0);
        treePage.intSetting(ShipLevelConfig.LEVEL_FLOOR_PERCENT_FIELD_ID, "settings.level.levelFloorPercent",
                ShipLevelConfig.DEFAULT_LEVEL_FLOOR_PERCENT, 0, 100);

        treePage.header("exiledSector_opCostHeader", "settings.opCost.header");
        treePage.intSetting(ShipLevelConfig.MAX_ALLOCATED_NODES_FIELD_ID, "settings.level.maxAllocatedNodes",
                ShipLevelConfig.DEFAULT_MAX_ALLOCATED_NODES, 1, 500);
        treePage.intSetting(SkillNodeOpCost.FRIGATE_FIELD_ID, untipped("settings.opCost.frigate"), SkillNodeOpCost.DEFAULT_FRIGATE, 0, 100);
        treePage.intSetting(SkillNodeOpCost.DESTROYER_FIELD_ID, untipped("settings.opCost.destroyer"), SkillNodeOpCost.DEFAULT_DESTROYER, 0, 100);
        treePage.intSetting(SkillNodeOpCost.CRUISER_FIELD_ID, untipped("settings.opCost.cruiser"), SkillNodeOpCost.DEFAULT_CRUISER, 0, 100);
        treePage.intSetting(SkillNodeOpCost.CAPITAL_FIELD_ID, untipped("settings.opCost.capital"), SkillNodeOpCost.DEFAULT_CAPITAL, 0, 100);
        treePage.intSetting(SkillNodeOpCost.UNDEFINED_FIELD_ID, untipped("settings.opCost.other"), SkillNodeOpCost.DEFAULT_UNDEFINED, 0, 100);
        treePage.radioSetting(IntegrationCostSetting.FIELD_ID, "settings.integrationCost", IntegrationCostSetting.DEFAULT,
                IntegrationCostSetting.OPTIONS);
        treePage.booleanSetting(AutoAllocateSetting.FREE_ALLOCATIONS_ONLY_FIELD_ID, "settings.autoAllocate.freeAllocationsOnly",
                AutoAllocateSetting.DEFAULT_FREE_ALLOCATIONS_ONLY);

        treePage.header("exiledSector_modulesHeader", "settings.modules.header");
        treePage.intSetting(ModuleShareSetting.FIELD_ID, "settings.modules.share",
                ModuleShareSetting.DEFAULT_PERCENT, 0, ModuleShareSetting.MAX_PERCENT);
        treePage.intSetting(ModuleShareSetting.STATION_FIELD_ID, "settings.modules.stationShare",
                ModuleShareSetting.DEFAULT_STATION_PERCENT, 0, ModuleShareSetting.MAX_PERCENT);

        treePage.header("exiledSector_unlockConditionsHeader", "settings.unlock.header");
        treePage.booleanSetting(UnlockConditionOverrides.DISABLE_BLUEPRINT_FIELD_ID, untipped("settings.unlock.blueprint"),
                UnlockConditionOverrides.DEFAULT_DISABLED);
        treePage.booleanSetting(UnlockConditionOverrides.DISABLE_CHARACTER_STAT_FIELD_ID, untipped("settings.unlock.characterStat"),
                UnlockConditionOverrides.DEFAULT_DISABLED);
        treePage.booleanSetting(UnlockConditionOverrides.DISABLE_MIN_SHIP_LEVEL_FIELD_ID, untipped("settings.unlock.minShipLevel"),
                UnlockConditionOverrides.DEFAULT_DISABLED);
        treePage.booleanSetting(UnlockConditionOverrides.DISABLE_MEMORY_FLAG_FIELD_ID, untipped("settings.unlock.gameState"),
                UnlockConditionOverrides.DEFAULT_DISABLED);
        treePage.booleanSetting(HiddenNodeDisplayConfig.SHOW_HIDDEN_NODES_FIELD_ID, "settings.hiddenNodes.show",
                HiddenNodeDisplayConfig.DEFAULT_SHOW_HIDDEN_NODES);

        treePage.header("exiledSector_treeItemsHeader", "settings.treeItems.header");
        UniqueSubroutinesSetting.UNLOCK_LEVEL.addSetting(treePage, "settings.hullItems.uniqueSubroutinesLevel");
        treePage.booleanSetting(AnchorJanusGate.ALLOW_BEFORE_JANUS_FIELD_ID,
                new SettingsPage.Label("settings.unlock.anchorsBeforeJanus", "settings.unlock.anchorsBeforeJanus.tooltip"),
                AnchorJanusGate.DEFAULT_ALLOW_BEFORE_JANUS);

        treePage.header("exiledSector_areasHeader", "settings.volumes.header");
        treePage.text("exiledSector_areasAbout", "settings.volumes.about");
        treePage.radioSetting(VolumeToggles.LOST_SECTOR_FIELD_ID, "settings.volumes.lostSector", VolumeToggles.AUTO, VolumeToggles.OPTIONS);
        treePage.radioSetting(VolumeToggles.RANDOM_ASSORTMENT_FIELD_ID, "settings.volumes.randomAssortment", VolumeToggles.AUTO,
                VolumeToggles.REQUIRED_MOD_OPTIONS);
        treePage.radioSetting(VolumeToggles.NIGHTCROSS_FIELD_ID, "settings.volumes.nightcross", VolumeToggles.AUTO, VolumeToggles.OPTIONS);
        treePage.radioSetting(VolumeToggles.TANIA_FIELD_ID, "settings.volumes.tania", VolumeToggles.AUTO, VolumeToggles.REQUIRED_MOD_OPTIONS);
    }

    private static void registerNpc() {
        SettingsPage npcPage = new SettingsPage(npcTab());
        npcPage.header("exiledSector_npcTreesHeader", "settings.npc.header");
        npcPage.text("exiledSector_npcTreesAbout", "settings.npc.about");
        npcPage.booleanSetting(NpcTreeConfig.ENABLED_FIELD_ID, untipped("settings.npc.enabled.name"), NpcTreeConfig.DEFAULT_ENABLED);
        npcPage.booleanSetting(NpcTreeConfig.OFFICERED_SHIPS_FIELD_ID, untipped("settings.npc.officeredShips.name"),
                NpcTreeConfig.DEFAULT_OFFICERED_SHIPS);
        npcPage.booleanSetting(NpcTreeConfig.FLAGSHIP_FIELD_ID, untipped("settings.npc.flagship.name"), NpcTreeConfig.DEFAULT_FLAGSHIP);
        npcPage.intSetting(NpcTreeConfig.OTHER_SHIP_CHANCE_FIELD_ID, "settings.npc.otherShipChance",
                NpcTreeConfig.DEFAULT_OTHER_SHIP_CHANCE_PERCENT, 0, 100);
        npcPage.booleanSetting(NpcTreeConfig.STATIONS_FIELD_ID, "settings.npc.stations", NpcTreeConfig.DEFAULT_STATIONS);
        npcPage.keybindSetting(NpcInspectConfig.KEYBIND_FIELD_ID, "settings.npc.inspectKey", NpcInspectConfig.DEFAULT_KEY);

        npcPage.header("exiledSector_npcTreeSizeHeader", "settings.npcTreeSize.header");
        npcPage.text("exiledSector_npcTreeSizeAbout", "settings.npcTreeSize.about");
        npcPage.intSetting(NpcTreeSize.MIN_PERCENT_FIELD_ID, untipped("settings.npcTreeSize.min.name"),
                NpcTreeSize.DEFAULT_MIN_PERCENT, NpcTreeSize.LOWEST_PERCENT, NpcTreeSize.HIGHEST_PERCENT);
        npcPage.intSetting(NpcTreeSize.MAX_PERCENT_FIELD_ID, untipped("settings.npcTreeSize.max.name"),
                NpcTreeSize.DEFAULT_MAX_PERCENT, NpcTreeSize.LOWEST_PERCENT, NpcTreeSize.HIGHEST_PERCENT);
        npcPage.radioSetting(NpcTreeConfig.LEVEL_MODE_FIELD_ID, "settings.npcTreeSize.mode", NpcTreeConfig.YOUR_SHIPS,
                NpcTreeConfig.LEVEL_MODE_OPTIONS);
        npcPage.intSetting(NpcTreeConfig.FIXED_MIN_PERCENT_FIELD_ID, "settings.npcTreeSize.fixedMin",
                NpcTreeConfig.DEFAULT_FIXED_MIN_PERCENT, 0, 100);
        npcPage.booleanSetting(NpcTreeConfig.RECOVERED_START_BLANK_FIELD_ID, "settings.npcTreeSize.startBlank",
                NpcTreeConfig.DEFAULT_RECOVERED_START_BLANK);

        npcPage.header("exiledSector_npcLevelCapsHeader", "settings.npcLevelCaps.header");
        npcPage.text("exiledSector_npcLevelCapsAbout", "settings.npcLevelCaps.about");
        npcPage.booleanSetting(NpcTreeConfig.LEVEL_CAPS_FIELD_ID, untipped("settings.npcLevelCaps.enabled.name"), NpcTreeConfig.DEFAULT_LEVEL_CAPS);
        for (NpcLevelCategory category : NpcLevelCategory.values()) {
            npcPage.intSetting(category.capFieldId(), untipped("settings.npcLevelCaps." + category.csvId() + ".name"),
                    category.defaultCap(), 0, 200);
        }

        npcPage.header("exiledSector_npcBonusScaleHeader", "settings.npcBonusScale.header");
        npcPage.text("exiledSector_npcBonusScaleAbout", "settings.npcBonusScale.about");
        npcPage.intSetting(NpcBonusScale.LOW_TREE_POWER_FIELD_ID, "settings.npcBonusScale.lowTreePower",
                NpcBonusScale.DEFAULT_LOW_TREE_POWER, 0, 300);
        npcPage.intSetting(NpcBonusScale.HIGH_TREE_POWER_FIELD_ID, "settings.npcBonusScale.highTreePower",
                NpcBonusScale.DEFAULT_HIGH_TREE_POWER, 1, 300);
        npcPage.doubleSetting(NpcBonusScale.MAX_MULTIPLIER_FIELD_ID, "settings.npcBonusScale.maxMultiplier",
                NpcBonusScale.DEFAULT_MAX_MULTIPLIER, 1.0, 10.0);
    }
}
