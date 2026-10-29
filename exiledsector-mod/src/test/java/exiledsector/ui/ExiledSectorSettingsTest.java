package exiledsector.ui;

import exiledsector.ExiledSectorModPlugin;
import exiledsector.ui.cargo.ItemAnimationSetting;
import exiledsector.charts.StarChartSettings;
import exiledsector.i18n.I18n;
import exiledsector.i18n.LanguageSetting;
import exiledsector.i18n.PseudoLeaks;
import exiledsector.i18n.PseudoLocale;
import exiledsector.i18n.RealCatalogue;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.skills.npc.NpcTreeConfig;
import exiledsector.skills.npc.NpcTreeSize;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.tags.AreaToggles;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.items.UniqueSubroutinesSetting;
import exiledsector.ui.inspect.NpcInspectConfig;
import exiledsector.ui.refit.RefitButtonConfig;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class ExiledSectorSettingsTest {

    private MockedStatic<LunaSettings.SettingsCreator> settingsCreatorMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private final List<String> registeredTexts = new ArrayList<>();
    private final Map<String, List<String>> headersByTab = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        settingsCreatorMock = Mockito.mockStatic(LunaSettings.SettingsCreator.class, invocation -> {
            int argumentCount = invocation.getArguments().length;
            if (argumentCount > 1 && !invocation.getMethod().getName().equals("refresh")) {
                List<String> tabHeaders = headersByTab.computeIfAbsent(invocation.getArgument(argumentCount - 1), tab -> new ArrayList<>());
                if (invocation.getMethod().getName().equals("addHeader")) {
                    tabHeaders.add(invocation.getArgument(1));
                }
            }
            for (Object argument : invocation.getArguments()) {
                if (argument instanceof String text) {
                    registeredTexts.add(text);
                }
            }
            return Mockito.RETURNS_DEFAULTS.answer(invocation);
        });
        ExiledSectorSettings.registerLanguage();
        ExiledSectorSettings.register(ExiledSectorModPlugin.DOMAINS);
    }

    @AfterEach
    void tearDown() {
        settingsCreatorMock.close();
        lunaSettingsMock.close();
    }

    @Test
    void registersTheNpcTreeSizePercentagesOnTheNpcTab() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addInt(eq("exiledSector"),
                eq(NpcTreeSize.MIN_PERCENT_FIELD_ID), anyString(), anyString(),
                eq(NpcTreeSize.DEFAULT_MIN_PERCENT), eq(NpcTreeSize.LOWEST_PERCENT), eq(NpcTreeSize.HIGHEST_PERCENT),
                eq(SkillTreeSettings.npcTab())));
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addInt(eq("exiledSector"),
                eq(NpcTreeSize.MAX_PERCENT_FIELD_ID), anyString(), anyString(),
                eq(NpcTreeSize.DEFAULT_MAX_PERCENT), eq(NpcTreeSize.LOWEST_PERCENT), eq(NpcTreeSize.HIGHEST_PERCENT),
                eq(SkillTreeSettings.npcTab())));
    }

    @Test
    void registersTheNpcTreeTogglesOnTheNpcTab() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addBoolean(eq("exiledSector"),
                eq(NpcTreeConfig.ENABLED_FIELD_ID), anyString(), anyString(), eq(true), eq(SkillTreeSettings.npcTab())));
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addBoolean(eq("exiledSector"),
                eq(NpcTreeConfig.OFFICERED_SHIPS_FIELD_ID), anyString(), anyString(), eq(true), eq(SkillTreeSettings.npcTab())));
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addBoolean(eq("exiledSector"),
                eq(NpcTreeConfig.FLAGSHIP_FIELD_ID), anyString(), anyString(), eq(true), eq(SkillTreeSettings.npcTab())));
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addInt(eq("exiledSector"),
                eq(NpcTreeConfig.OTHER_SHIP_CHANCE_FIELD_ID), anyString(), anyString(), eq(30), eq(0), eq(100),
                eq(SkillTreeSettings.npcTab())));
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addBoolean(eq("exiledSector"),
                eq(NpcTreeConfig.STATIONS_FIELD_ID), anyString(), anyString(), eq(true), eq(SkillTreeSettings.npcTab())));
    }

    @Test
    void registersTheBattleDifficultyXpSettingsWithDefaultsThatMatchVanilla() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addDouble(eq("exiledSector"),
                eq(ShipLevelConfig.XP_DIFFICULTY_STRENGTH_FIELD_ID), anyString(), anyString(), eq(1.0), eq(0.0), eq(5.0), eq(SkillTreeSettings.skillTreeTab())));
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addDouble(eq("exiledSector"),
                eq(ShipLevelConfig.XP_DIFFICULTY_MAX_MULTIPLIER_FIELD_ID), anyString(), anyString(), eq(6.0), eq(1.0), eq(20.0), eq(SkillTreeSettings.skillTreeTab())));
    }

    @Test
    void registersTheSkillTreeToggleSwitchedOnByDefault() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addBoolean(eq("exiledSector"),
                eq(SkillTreeSetting.ENABLED_FIELD_ID), anyString(), anyString(), eq(true), eq(SkillTreeSettings.skillTreeTab())));
    }

    @Test
    void registersTheHullUpgradesToggleSwitchedOnByDefault() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addBoolean(eq("exiledSector"),
                eq(HullUpgradesSetting.ENABLED_FIELD_ID), anyString(), anyString(), eq(true), eq(ExiledSectorSettings.itemsTab())));
    }

    @Test
    void registersTheHullUpgradeAndUniqueSubroutineLevelsDefaultingToFifteen() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addInt(eq("exiledSector"),
                eq(HullUpgradesSetting.UNLOCK_LEVEL_FIELD_ID), anyString(), anyString(), eq(15), eq(1), eq(15), eq(ExiledSectorSettings.itemsTab())));
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addInt(eq("exiledSector"),
                eq(UniqueSubroutinesSetting.UNLOCK_LEVEL_FIELD_ID), anyString(), anyString(), eq(15), eq(1), eq(15), eq(SkillTreeSettings.skillTreeTab())));
    }

    @Test
    void registersTheStarChartsToggleSwitchedOnByDefault() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addBoolean(eq("exiledSector"),
                eq(StarChartSettings.ENABLED_FIELD_ID), anyString(), anyString(), eq(true), eq(StarChartSettings.settingsTab())));
    }

    @Test
    void registersTheLostSectorAreaToggleDefaultingToAuto() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addRadio(eq("exiledSector"),
                eq(AreaToggles.LOST_SECTOR_FIELD_ID), anyString(), anyString(), eq(AreaToggles.AUTO),
                eq(String.join(",", AreaToggles.OPTIONS)), eq(SkillTreeSettings.skillTreeTab())));
    }

    @Test
    void registersTheRandomAssortmentAreaToggleDefaultingToAuto() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addRadio(eq("exiledSector"),
                eq(AreaToggles.RANDOM_ASSORTMENT_FIELD_ID), anyString(), anyString(), eq(AreaToggles.AUTO),
                eq(String.join(",", AreaToggles.REQUIRED_MOD_OPTIONS)), eq(SkillTreeSettings.skillTreeTab())));
    }

    @Test
    void registersTheNightcrossAreaToggleDefaultingToAuto() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addRadio(eq("exiledSector"),
                eq(AreaToggles.NIGHTCROSS_FIELD_ID), anyString(), anyString(), eq(AreaToggles.AUTO),
                eq(String.join(",", AreaToggles.OPTIONS)), eq(SkillTreeSettings.skillTreeTab())));
    }

    @Test
    void registersTheTaniaAreaToggleDefaultingToAuto() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addRadio(eq("exiledSector"),
                eq(AreaToggles.TANIA_FIELD_ID), anyString(), anyString(), eq(AreaToggles.AUTO),
                eq(String.join(",", AreaToggles.REQUIRED_MOD_OPTIONS)), eq(SkillTreeSettings.skillTreeTab())));
    }

    @Test
    void registersTheRefitButtonToggleDefaultingToTheButtonUnderTheHullMods() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addBoolean(eq("exiledSector"),
                eq(RefitButtonConfig.FIELD_ID), anyString(), anyString(), eq(true), eq("")));
    }

    @Test
    void registersTheStarChartEntriesDefaultingToOne() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addInt(eq("exiledSector"),
                eq(StarChartSettings.ENTRIES_FIELD_ID), anyString(), anyString(), eq(1), eq(1), eq(StarChartSettings.MAX_ENTRIES), eq(StarChartSettings.settingsTab())));
    }

    @Test
    void registersTheOpenWormholesAnywhereDevSwitchOffByDefault() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addBoolean(eq("exiledSector"),
                eq(StarChartSettings.OPEN_ANYWHERE_FIELD_ID), anyString(), anyString(), eq(false), eq(StarChartSettings.settingsTab())));
    }

    @Test
    void registersTheStarChartDebugToolsOffByDefault() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addBoolean(eq("exiledSector"),
                eq(StarChartSettings.DEBUG_TOOLS_FIELD_ID), anyString(), anyString(), eq(false), eq(StarChartSettings.settingsTab())));
    }

    @Test
    void noSettingTextContainsAPercentSignBecauseLunaLibFormatsItBeforeDisplaying() {
        assertFalse(registeredTexts.isEmpty());
        for (String text : registeredTexts) {
            assertFalse(text.contains("%"), () -> "LunaLib passes this through String.format: " + text);
        }
    }

    @Test
    void everySettingsTextComesFromTheCatalogue() {
        I18n.install(RealCatalogue.of(PseudoLocale.LOCALE));
        registeredTexts.clear();
        ExiledSectorSettings.registerLanguage();
        ExiledSectorSettings.register(ExiledSectorModPlugin.DOMAINS);
        String options = String.join(",", LanguageSetting.OPTIONS);
        String areaOptions = String.join(",", AreaToggles.OPTIONS);
        for (String text : registeredTexts) {
            boolean untranslated = text.isEmpty() || text.startsWith("exiledSector") || text.equals(options)
                    || LanguageSetting.OPTIONS.contains(text) || text.equals(areaOptions) || text.equals(String.join(",", AreaToggles.REQUIRED_MOD_OPTIONS))
                    || AreaToggles.OPTIONS.contains(text) || ItemAnimationSetting.OPTIONS.contains(text)
                    || text.equals(String.join(",", ItemAnimationSetting.OPTIONS)) || HullUpgradesSetting.POINTS_OPTIONS.contains(text)
                    || text.equals(String.join(",", HullUpgradesSetting.POINTS_OPTIONS));
            assertFalse(!untranslated && PseudoLeaks.hasLeak(text), () -> "Hardcoded settings text: " + text);
        }
    }

    @Test
    void refreshesAfterTheLanguageChoiceAndAgainAfterEverythingElse() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.refresh("exiledSector"), Mockito.times(2));
    }

    @Test
    void offersTheLanguageChoiceAsARadioDefaultingToAuto() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addRadio(eq("exiledSector"), eq(LanguageSetting.fieldId()),
                anyString(), anyString(), eq(LanguageSetting.AUTO), eq("Auto,English,Simplified Chinese"), eq("")));
    }

    @Test
    void ordersTheTabsGeneralThenEachDomainThenItemsAndCombat() {
        assertEquals(List.of(ExiledSectorSettings.MAIN_TAB, SkillTreeSettings.skillTreeTab(), SkillTreeSettings.npcTab(),
                ExiledSectorSettings.itemsTab(), StarChartSettings.settingsTab(), ExiledSectorSettings.combatTab()),
                List.copyOf(headersByTab.keySet()));
    }

    @Test
    void groupsEachTabUnderItsOwnHeaders() {
        assertEquals(List.of("exiledSector_header"), headersByTab.get(ExiledSectorSettings.MAIN_TAB));
        assertEquals(List.of("exiledSector_skillTreeHeader", "exiledSector_levelHeader", "exiledSector_opCostHeader", "exiledSector_modulesHeader",
                "exiledSector_unlockConditionsHeader", "exiledSector_treeItemsHeader", "exiledSector_areasHeader"),
                headersByTab.get(SkillTreeSettings.skillTreeTab()));
        assertEquals(List.of("exiledSector_npcTreesHeader", "exiledSector_npcTreeSizeHeader", "exiledSector_npcBonusScaleHeader"),
                headersByTab.get(SkillTreeSettings.npcTab()));
        assertEquals(List.of("exiledSector_hullUpgradesHeader", "exiledSector_socketStorageHeader", "exiledSector_craftingHeader"),
                headersByTab.get(ExiledSectorSettings.itemsTab()));
        assertEquals(List.of("exiledSector_starChartsHeader"), headersByTab.get(StarChartSettings.settingsTab()));
        assertEquals(List.of("exiledSector_combatEffectsHeader", "exiledSector_diminishingHeader"),
                headersByTab.get(ExiledSectorSettings.combatTab()));
    }

    @Test
    void registersTheInspectKeybindOnTheNpcTab() {
        settingsCreatorMock.verify(() -> LunaSettings.SettingsCreator.addKeybind(eq("exiledSector"),
                eq(NpcInspectConfig.KEYBIND_FIELD_ID), anyString(), anyString(), eq(NpcInspectConfig.DEFAULT_KEY),
                eq(SkillTreeSettings.npcTab())));
    }
}
