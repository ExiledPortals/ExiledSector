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
import exiledsector.socketables.HullUpgradesSetting;
import exiledsector.ui.HullUpgradesRefitButton;
import exiledsector.ui.SkillTreeRefitButton;
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

class SkillTreeModsButtonTest {

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
    private MockedStatic<HullUpgradesRefitButton> hullUpgradesButtonMock;
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

    @BeforeEach
    void setUp() {
        SkillTreeModsButton.resetForTests();
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

        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.createCustom(anyFloat(), anyFloat(), any())).thenAnswer(invocation -> {
            CustomUIPanelPlugin created = invocation.getArgument(2);
            CustomPanelAPI target = created instanceof HullUpgradesModsButton ? hullUpgradesContainer : container;
            if (created instanceof HullUpgradesModsButton) {
                hullUpgradesPlugin = created;
            } else {
                skillTreePlugin = created;
            }
            when(target.getPlugin()).thenReturn(created);
            return target;
        });
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(mock(SectorAPI.class, Mockito.RETURNS_DEEP_STUBS));
        refitButtonMock = Mockito.mockStatic(SkillTreeRefitButton.class);
        hullUpgradesButtonMock = Mockito.mockStatic(HullUpgradesRefitButton.class);
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        buttonUnderHullMods(true);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
        hullUpgradesButtonMock.close();
        refitButtonMock.close();
        globalMock.close();
        SkillTreeModsButton.resetForTests();
        HullUpgradesModsButton.resetForTests();
        HullUpgradesSetting.setEnabledForTests(HullUpgradesSetting.DEFAULT_ENABLED);
    }

    @Test
    void theButtonSitsOneSlotBelowBuildInWithBuildInsSizeAndTheKHotkey() {
        SkillTreeModsButton.attach(widget);

        verify(container).createUIElement(150f, 25f, false);
        verify(button).setShortcut(Keyboard.KEY_K, true);
        verify(placement).belowMid(buildIn, SkillTreeModsButton.GAP_BELOW_BUILD_IN);
        verify(placement).setXAlignOffset(SkillTreeModsButton.X_ALIGN_OFFSET);
    }

    @Test
    void withExoticaInstalledTheButtonDropsOneSlotToMakeRoomForTheExoticaButton() {
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(modManager.isModEnabled(ExoticaCompat.MOD_ID)).thenReturn(true);
        when(Global.getSettings().getModManager()).thenReturn(modManager);

        SkillTreeModsButton.attach(widget);

        verify(placement).belowMid(buildIn, 31f);
        verify(placement).setXAlignOffset(SkillTreeModsButton.X_ALIGN_OFFSET);
    }

    @Test
    void theButtonIsStyledLikeTheOtherHullModButtons() {
        TooltipMakerAPI element = container.createUIElement(150f, 25f, false);
        SkillTreeModsButton.attach(widget);

        verify(element).setButtonFontOrbitron20();
        verify(element).addButton(any(), eq(SkillTreeModsButton.BUTTON_ID), any(), any(), eq(Alignment.MID), eq(CutStyle.BOTTOM),
                eq(150f), eq(25f), eq(0f));
    }

    @Test
    void aWidgetThatAlreadyHasTheButtonDoesNotGetASecondOne() {
        SkillTreeModsButton.attach(widget);
        widget.children.add(container);

        SkillTreeModsButton.attach(widget);

        verify(widget, times(2)).addComponent(any());
    }

    @Test
    void aRefitScreenWithoutABuildInButtonGetsNoButton() {
        widget.perm = null;

        SkillTreeModsButton.attach(widget);

        verify(widget, never()).addComponent(any());
    }

    @Test
    void pressingTheButtonOpensTheSkillTree() {
        SkillTreeModsButton.attach(widget);

        skillTreePlugin.buttonPressed(SkillTreeModsButton.BUTTON_ID);
        skillTreePlugin.buttonPressed("somethingElse");

        refitButtonMock.verify(() -> SkillTreeRefitButton.openPanel(null), times(1));
    }

    @Test
    void turningTheSettingOffHidesTheButtonWithoutRemovingItSinceOtherModsMayAnchorOnIt() {
        SkillTreeModsButton.attach(widget);
        widget.children.add(container);
        buttonUnderHullMods(false);

        SkillTreeModsButton.attach(widget);
        skillTreePlugin.buttonPressed(SkillTreeModsButton.BUTTON_ID);

        verify(widget, never()).removeComponent(any());
        verify(container).setOpacity(0f);
        verify(button).setEnabled(false);
        refitButtonMock.verify(() -> SkillTreeRefitButton.openPanel(null), never());

        buttonUnderHullMods(true);
        SkillTreeModsButton.attach(widget);

        verify(container).setOpacity(1f);
        verify(widget, times(2)).addComponent(any());
    }

    @Test
    void withTheSettingOffANewRefitScreenGetsNoButton() {
        buttonUnderHullMods(false);

        SkillTreeModsButton.attach(widget);

        verify(widget, never()).addComponent(any());
    }

    @Test
    void theAdditionalOptionsDropdownListsTheSkillTreeOnlyWhenTheButtonIsOff() {
        refitButtonMock.close();
        try {
            assertFalse(new SkillTreeRefitButton().shouldShow(null, null, null));
            buttonUnderHullMods(false);
            assertTrue(new SkillTreeRefitButton().shouldShow(null, null, null));
        } finally {
            refitButtonMock = Mockito.mockStatic(SkillTreeRefitButton.class);
        }
    }

    @Test
    void theHullUpgradesButtonSitsRightBelowTheSkillTreeButtonWithTheLHotkey() {
        SkillTreeModsButton.attach(widget);

        verify(hullUpgradesContainer).createUIElement(150f, 25f, false);
        verify(hullUpgradesButton).setShortcut(Keyboard.KEY_L, true);
        verify(placement).belowMid(container, HullUpgradesModsButton.GAP_BELOW_SKILL_TREE);
    }

    @Test
    void pressingTheHullUpgradesButtonOpensTheHullUpgradesScreen() {
        SkillTreeModsButton.attach(widget);

        hullUpgradesPlugin.buttonPressed(HullUpgradesModsButton.BUTTON_ID);
        hullUpgradesPlugin.buttonPressed(SkillTreeModsButton.BUTTON_ID);

        hullUpgradesButtonMock.verify(() -> HullUpgradesRefitButton.openPanel(null), times(1));
        refitButtonMock.verify(() -> SkillTreeRefitButton.openPanel(null), never());
    }

    @Test
    void switchingHullUpgradesOffHidesItsButtonWithoutRemovingIt() {
        SkillTreeModsButton.attach(widget);
        widget.children.add(container);
        widget.children.add(hullUpgradesContainer);
        HullUpgradesSetting.setEnabledForTests(false);

        SkillTreeModsButton.attach(widget);
        hullUpgradesPlugin.buttonPressed(HullUpgradesModsButton.BUTTON_ID);

        verify(widget, never()).removeComponent(any());
        verify(hullUpgradesContainer).setOpacity(0f);
        verify(hullUpgradesButton).setEnabled(false);
        hullUpgradesButtonMock.verify(() -> HullUpgradesRefitButton.openPanel(null), never());
    }

    private void buttonUnderHullMods(boolean value) {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", RefitButtonConfig.FIELD_ID)).thenReturn(value);
    }
}
