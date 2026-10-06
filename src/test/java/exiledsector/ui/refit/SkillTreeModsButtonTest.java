package exiledsector.ui.refit;

import com.fs.starfarer.api.Global;
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
import exiledsector.ui.SkillTreeRefitButton;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.input.Keyboard;
import org.mockito.ArgumentCaptor;
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
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private ModWidget widget;
    private ButtonAPI buildIn;
    private PositionAPI placement;
    private CustomPanelAPI container;
    private ButtonAPI button;
    private ArgumentCaptor<CustomUIPanelPlugin> plugin;

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

        SettingsAPI settings = mock(SettingsAPI.class);
        plugin = ArgumentCaptor.forClass(CustomUIPanelPlugin.class);
        when(settings.createCustom(anyFloat(), anyFloat(), plugin.capture())).thenAnswer(invocation -> {
            when(container.getPlugin()).thenReturn(invocation.getArgument(2));
            return container;
        });
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        globalMock.when(Global::getSector).thenReturn(mock(SectorAPI.class, Mockito.RETURNS_DEEP_STUBS));
        refitButtonMock = Mockito.mockStatic(SkillTreeRefitButton.class);
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class);
        buttonUnderHullMods(true);
    }

    @AfterEach
    void tearDown() {
        lunaSettingsMock.close();
        refitButtonMock.close();
        globalMock.close();
        SkillTreeModsButton.resetForTests();
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

        verify(widget, times(1)).addComponent(any());
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

        plugin.getValue().buttonPressed(SkillTreeModsButton.BUTTON_ID);
        plugin.getValue().buttonPressed("somethingElse");

        refitButtonMock.verify(() -> SkillTreeRefitButton.openPanel(null), times(1));
    }

    @Test
    void turningTheSettingOffHidesTheButtonWithoutRemovingItSinceOtherModsMayAnchorOnIt() {
        SkillTreeModsButton.attach(widget);
        widget.children.add(container);
        buttonUnderHullMods(false);

        SkillTreeModsButton.attach(widget);
        plugin.getValue().buttonPressed(SkillTreeModsButton.BUTTON_ID);

        verify(widget, never()).removeComponent(any());
        verify(container).setOpacity(0f);
        verify(button).setEnabled(false);
        refitButtonMock.verify(() -> SkillTreeRefitButton.openPanel(null), never());

        buttonUnderHullMods(true);
        SkillTreeModsButton.attach(widget);

        verify(container).setOpacity(1f);
        verify(widget, times(1)).addComponent(any());
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

    private void buttonUnderHullMods(boolean value) {
        lunaSettingsMock.when(() -> LunaSettings.getBoolean("exiledSector", RefitButtonConfig.FIELD_ID)).thenReturn(value);
    }
}
