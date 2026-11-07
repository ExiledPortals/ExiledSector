package exiledsector.ui.refit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.compat.ExoticaCompat;
import exiledsector.items.HullUpgradesSetting;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.ui.HullItemsRefitButton;
import exiledsector.ui.CraftingBenchRefitButton;
import exiledsector.ui.SkillTreeRefitButton;
import lunalib.lunaRefit.BaseRefitButton;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class RefitButtonColumnTest {

    public abstract static class ModWidget implements UIPanelAPI {
        public final List<Object> children = new ArrayList<>();
        public ButtonAPI perm;

        public ButtonAPI getPerm() {
            return perm;
        }

        public List<Object> getChildrenNonCopy() {
            return children;
        }
    }

    private MockedStatic<Global> globalMock;
    private MockedStatic<SkillTreeRefitButton> refitButtonMock;
    private MockedStatic<HullItemsRefitButton> hullUpgradesButtonMock;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private ModWidget widget;
    private ButtonAPI buildIn;
    private PositionAPI placement;
    private CustomPanelAPI container;
    private ButtonAPI button;
    private CustomUIPanelPlugin skillTreePlugin;
    private CustomUIPanelPlugin hullUpgradesPlugin;
    private CustomPanelAPI hullUpgradesContainer;
    private ButtonAPI hullUpgradesButton;
    private CustomPanelAPI benchContainer;
    private ButtonAPI benchButton;
    private CustomUIPanelPlugin benchPlugin;
    private MockedStatic<CraftingBenchRefitButton> benchButtonMock;
    private int createdButtons;

    @BeforeEach
    void setUp() {
        RefitButtonColumn.resetForTests();
        createdButtons = 0;
        widget = mock(ModWidget.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        buildIn = mock(ButtonAPI.class);
        PositionAPI buildInPosition = mock(PositionAPI.class);
        when(buildInPosition.getWidth()).thenReturn(150f);
        when(buildInPosition.getHeight()).thenReturn(25f);
        when(buildIn.getPosition()).thenReturn(buildInPosition);
        widget.perm = buildIn;
        placement = mock(PositionAPI.class, Mockito.RETURNS_SELF);
        doReturn(placement).when(widget).addComponent(any());

        container = mock(CustomPanelAPI.class);
        TooltipMakerAPI element = mock(TooltipMakerAPI.class);
        when(container.createUIElement(anyFloat(), anyFloat(), eq(false))).thenReturn(element);
        button = mock(ButtonAPI.class);
        when(element.addButton(any(), any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat())).thenReturn(button);

        hullUpgradesContainer = mock(CustomPanelAPI.class);
        TooltipMakerAPI hullUpgradesElement = mock(TooltipMakerAPI.class);
        when(hullUpgradesContainer.createUIElement(anyFloat(), anyFloat(), eq(false))).thenReturn(hullUpgradesElement);
        hullUpgradesButton = mock(ButtonAPI.class);
        when(hullUpgradesElement.addButton(any(), any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat())).thenReturn(hullUpgradesButton);

        benchContainer = mock(CustomPanelAPI.class);
        TooltipMakerAPI benchElement = mock(TooltipMakerAPI.class);
        when(benchContainer.createUIElement(anyFloat(), anyFloat(), eq(false))).thenReturn(benchElement);
        benchButton = mock(ButtonAPI.class);
        when(benchElement.addButton(any(), any(), any(), any(), any(), any(), anyFloat(), anyFloat(), anyFloat())).thenReturn(benchButton);

        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.createCustom(anyFloat(), anyFloat(), any())).thenAnswer(invocation -> {
            CustomUIPanelPlugin created = invocation.getArgument(2);
            createdButtons++;
            CustomPanelAPI target;
            if (createdButtons == 1) {
                skillTreePlugin = created;
                target = container;
            } else if (createdButtons == 2) {
                hullUpgradesPlugin = created;
                target = hullUpgradesContainer;
            } else {
                benchPlugin = created;
                target = benchContainer;
            }
            when(target.getPlugin()).thenReturn(created);
            return target;
        });
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(mock(SectorAPI.class, Mockito.RETURNS_DEEP_STUBS));
        refitButtonMock = Mockito.mockStatic(SkillTreeRefitButton.class);
        hullUpgradesButtonMock = Mockito.mockStatic(HullItemsRefitButton.class);
        benchButtonMock = Mockito.mockStatic(CraftingBenchRefitButton.class);
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", RefitButtonConfig.SKILL_TREE_KEYBIND_FIELD_ID)).thenReturn(null);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", RefitButtonConfig.HULL_ITEMS_KEYBIND_FIELD_ID)).thenReturn(null);
        buttonUnderHullMods(true);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
        hullUpgradesButtonMock.close();
        benchButtonMock.close();
        refitButtonMock.close();
        globalMock.close();
        RefitButtonColumn.resetForTests();
        HullUpgradesSetting.setEnabledForTests(HullUpgradesSetting.DEFAULT_ENABLED);
        SkillTreeSetting.setEnabledForTests(SkillTreeSetting.DEFAULT_ENABLED);
    }

    @Test
    void theButtonSitsOneSlotBelowBuildInWithBuildInsSizeAndTheKHotkey() {
        RefitButtonColumn.attach(widget);

        verify(container).createUIElement(150f, 25f, false);
        verify(button).setShortcut(Keyboard.KEY_K, true);
        verify(placement).belowMid(buildIn, RefitButtonColumn.GAP_BELOW_BUILD_IN);
        verify(placement).setXAlignOffset(RefitButtonColumn.X_ALIGN_OFFSET);
    }

    @Test
    void withExoticaInstalledTheButtonDropsOneSlotToMakeRoomForTheExoticaButton() {
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(modManager.isModEnabled(ExoticaCompat.MOD_ID)).thenReturn(true);
        when(Global.getSettings().getModManager()).thenReturn(modManager);

        RefitButtonColumn.attach(widget);

        verify(placement).belowMid(buildIn, 31f);
        verify(placement).setXAlignOffset(RefitButtonColumn.X_ALIGN_OFFSET);
    }

    @Test
    void theButtonIsStyledLikeTheOtherHullModButtons() {
        TooltipMakerAPI element = container.createUIElement(150f, 25f, false);
        RefitButtonColumn.attach(widget);

        verify(element).setButtonFontOrbitron20();
        verify(element).addButton(any(), eq(SkillTreeRefitButton.COLUMN_BUTTON.id()), any(), any(), eq(Alignment.MID), eq(CutStyle.BOTTOM),
                eq(150f), eq(25f), eq(0f));
    }

    @Test
    void aWidgetThatAlreadyHasTheButtonDoesNotGetASecondOne() {
        RefitButtonColumn.attach(widget);
        widget.children.add(container);

        RefitButtonColumn.attach(widget);

        verify(widget, times(3)).addComponent(any());
    }

    @Test
    void aRefitScreenWithoutABuildInButtonGetsNoButton() {
        widget.perm = null;

        RefitButtonColumn.attach(widget);

        verify(widget, never()).addComponent(any());
    }

    @Test
    void pressingTheButtonOpensTheSkillTree() {
        RefitButtonColumn.attach(widget);

        skillTreePlugin.buttonPressed(SkillTreeRefitButton.COLUMN_BUTTON.id());
        skillTreePlugin.buttonPressed("somethingElse");

        refitButtonMock.verify(() -> SkillTreeRefitButton.openPanel(null), times(1));
    }

    @Test
    void turningTheSettingOffHidesTheButtonWithoutRemovingItSinceOtherModsMayAnchorOnIt() {
        RefitButtonColumn.attach(widget);
        widget.children.add(container);
        buttonUnderHullMods(false);

        RefitButtonColumn.attach(widget);
        skillTreePlugin.buttonPressed(SkillTreeRefitButton.COLUMN_BUTTON.id());

        verify(widget, never()).removeComponent(any());
        verify(container).setOpacity(0f);
        verify(button).setEnabled(false);
        refitButtonMock.verify(() -> SkillTreeRefitButton.openPanel(null), never());

        buttonUnderHullMods(true);
        RefitButtonColumn.attach(widget);

        verify(container).setOpacity(1f);
        verify(widget, times(3)).addComponent(any());
    }

    @Test
    void withTheSettingOffANewRefitScreenGetsNoButton() {
        buttonUnderHullMods(false);

        RefitButtonColumn.attach(widget);

        verify(widget, never()).addComponent(any());
    }

    @Test
    void theAdditionalOptionsDropdownListsEveryRefitScreenOnlyWhenTheButtonsAreOff() {
        refitButtonMock.close();
        hullUpgradesButtonMock.close();
        benchButtonMock.close();
        try {
            List<BaseRefitButton> dropdownButtons = List.of(new SkillTreeRefitButton(), new HullItemsRefitButton(), new CraftingBenchRefitButton());
            dropdownButtons.forEach(dropdownButton -> assertFalse(dropdownButton.shouldShow(null, null, null)));
            buttonUnderHullMods(false);
            dropdownButtons.forEach(dropdownButton -> assertTrue(dropdownButton.shouldShow(null, null, null)));
        } finally {
            refitButtonMock = Mockito.mockStatic(SkillTreeRefitButton.class);
            hullUpgradesButtonMock = Mockito.mockStatic(HullItemsRefitButton.class);
            benchButtonMock = Mockito.mockStatic(CraftingBenchRefitButton.class);
        }
    }

    @Test
    void theHullUpgradesButtonSitsRightBelowTheSkillTreeButtonWithTheLHotkey() {
        RefitButtonColumn.attach(widget);

        verify(hullUpgradesContainer).createUIElement(150f, 25f, false);
        verify(hullUpgradesButton).setShortcut(Keyboard.KEY_L, true);
        verify(placement).belowMid(container, RefitButtonColumn.GAP_BETWEEN_BUTTONS);
    }

    @Test
    void pressingTheHullUpgradesButtonOpensTheHullUpgradesScreen() {
        RefitButtonColumn.attach(widget);

        hullUpgradesPlugin.buttonPressed(HullItemsRefitButton.COLUMN_BUTTON.id());
        hullUpgradesPlugin.buttonPressed(SkillTreeRefitButton.COLUMN_BUTTON.id());

        hullUpgradesButtonMock.verify(() -> HullItemsRefitButton.openPanel(null), times(1));
        refitButtonMock.verify(() -> SkillTreeRefitButton.openPanel(null), never());
    }

    @Test
    void switchingHullUpgradesOffHidesItsButtonWithoutRemovingIt() {
        RefitButtonColumn.attach(widget);
        widget.children.add(container);
        widget.children.add(hullUpgradesContainer);
        HullUpgradesSetting.setEnabledForTests(false);

        RefitButtonColumn.attach(widget);
        hullUpgradesPlugin.buttonPressed(HullItemsRefitButton.COLUMN_BUTTON.id());

        verify(widget, never()).removeComponent(any());
        verify(hullUpgradesContainer).setOpacity(0f);
        verify(hullUpgradesButton).setEnabled(false);
        hullUpgradesButtonMock.verify(() -> HullItemsRefitButton.openPanel(null), never());
    }

    @Test
    void switchingTheSkillTreeOffParksItsButtonOffScreenAndMovesTheOthersUpUnderBuildIn() {
        RefitButtonColumn.attach(widget);
        widget.children.add(container);
        widget.children.add(hullUpgradesContainer);
        widget.children.add(benchContainer);
        SkillTreeSetting.setEnabledForTests(false);

        RefitButtonColumn.attach(widget);
        skillTreePlugin.buttonPressed(SkillTreeRefitButton.COLUMN_BUTTON.id());

        verify(widget, never()).removeComponent(any());
        verify(container).setOpacity(0f);
        verify(button).setEnabled(false);
        verify(placement).inTL(RefitButtonColumn.HIDDEN_OFFSET, RefitButtonColumn.HIDDEN_OFFSET);
        verify(placement, times(2)).belowMid(buildIn, RefitButtonColumn.GAP_BELOW_BUILD_IN);
        verify(placement, times(2)).belowMid(hullUpgradesContainer, RefitButtonColumn.GAP_BETWEEN_BUTTONS);
        refitButtonMock.verify(() -> SkillTreeRefitButton.openPanel(null), never());
    }

    @Test
    void theCraftingBenchButtonSitsRightBelowTheHullItemsButtonWithNoHotkeyByDefault() {
        RefitButtonColumn.attach(widget);

        verify(benchContainer).createUIElement(150f, 25f, false);
        verify(benchButton, never()).setShortcut(anyInt(), anyBoolean());
        verify(placement).belowMid(hullUpgradesContainer, RefitButtonColumn.GAP_BETWEEN_BUTTONS);
    }

    @Test
    void aBoundCraftingKeyBecomesTheBenchButtonShortcut() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", RefitButtonConfig.BENCH_KEYBIND_FIELD_ID)).thenReturn(Keyboard.KEY_J);

        RefitButtonColumn.attach(widget);

        verify(benchButton).setShortcut(Keyboard.KEY_J, true);
    }

    @Test
    void reboundSkillTreeAndHullItemsKeysBecomeTheirButtonsShortcuts() {
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", RefitButtonConfig.SKILL_TREE_KEYBIND_FIELD_ID)).thenReturn(Keyboard.KEY_U);
        lunaSettingsMock.when(() -> LunaSettings.getInt("exiledSector", RefitButtonConfig.HULL_ITEMS_KEYBIND_FIELD_ID)).thenReturn(Keyboard.KEY_O);

        RefitButtonColumn.attach(widget);

        verify(button).setShortcut(Keyboard.KEY_U, true);
        verify(hullUpgradesButton).setShortcut(Keyboard.KEY_O, true);
    }

    @Test
    void pressingTheCraftingBenchButtonOpensTheBench() {
        RefitButtonColumn.attach(widget);

        benchPlugin.buttonPressed(RefitButtonColumn.BENCH_BUTTON.id());
        benchPlugin.buttonPressed(HullItemsRefitButton.COLUMN_BUTTON.id());

        benchButtonMock.verify(() -> CraftingBenchRefitButton.openPanel(null), times(1));
        hullUpgradesButtonMock.verify(() -> HullItemsRefitButton.openPanel(null), never());
    }

    @Test
    void turningTheSettingOffHidesTheBenchButtonWithoutRemovingIt() {
        RefitButtonColumn.attach(widget);
        widget.children.add(container);
        widget.children.add(hullUpgradesContainer);
        widget.children.add(benchContainer);
        buttonUnderHullMods(false);

        RefitButtonColumn.attach(widget);

        verify(widget, never()).removeComponent(any());
        verify(benchContainer).setOpacity(0f);
        verify(benchButton).setEnabled(false);
    }

    private void buttonUnderHullMods(boolean value) {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", RefitButtonConfig.FIELD_ID)).thenReturn(value);
    }
}
