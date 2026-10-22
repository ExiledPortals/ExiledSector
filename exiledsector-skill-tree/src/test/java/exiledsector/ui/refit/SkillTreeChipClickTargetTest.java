package exiledsector.ui.refit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import exiledsector.ui.SkillTreeRefitButton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class SkillTreeChipClickTargetTest {

    public abstract static class Widget implements UIPanelAPI {
        public final List<Object> children = new ArrayList<>();

        public List<Object> getChildrenNonCopy() {
            return children;
        }
    }

    public abstract static class RowList implements UIComponentAPI {
        public final List<Object> items = new ArrayList<>();

        public List<Object> getItems() {
            return items;
        }
    }

    public abstract static class ChipRow implements UIComponentAPI {
        public final List<Object> children = new ArrayList<>();

        public List<Object> getChildrenNonCopy() {
            return children;
        }
    }

    private static final float ICON_X = 290;
    private static final float ICON_Y = 280;

    private MockedStatic<Global> globalMock;
    private MockedStatic<SkillTreeRefitButton> buttonMock;
    private Widget widget;
    private RowList list;
    private ChipRow chip;
    private ButtonAPI icon;
    private CustomUIPanelPlugin plugin;

    private static ButtonAPI button(String text, PositionAPI position) {
        ButtonAPI button = mock(ButtonAPI.class);
        when(button.getText()).thenReturn(text);
        when(button.getPosition()).thenReturn(position);
        return button;
    }

    private static PositionAPI position(float x, float y, float width, float height) {
        PositionAPI position = mock(PositionAPI.class);
        when(position.getX()).thenReturn(x);
        when(position.getY()).thenReturn(y);
        when(position.getWidth()).thenReturn(width);
        when(position.getHeight()).thenReturn(height);
        return position;
    }

    private static InputEventAPI lmb(boolean down, float x, float y) {
        InputEventAPI event = mock(InputEventAPI.class);
        when(event.isLMBDownEvent()).thenReturn(down);
        when(event.isLMBUpEvent()).thenReturn(!down);
        when(event.getX()).thenReturn((int) x);
        when(event.getY()).thenReturn((int) y);
        return event;
    }

    @BeforeEach
    void setUp() {
        PhantomHullModRefitHider.resetForTests();
        widget = mock(Widget.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        PositionAPI widgetPosition = position(0, 0, 400, 600);
        doReturn(widgetPosition).when(widget).getPosition();
        doAnswer(call -> {
            widget.children.add(call.getArgument(0));
            return mock(PositionAPI.class);
        }).when(widget).addComponent(any());

        list = mock(RowList.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        PositionAPI listPosition = position(10, 100, 300, 200);
        doReturn(listPosition).when(list).getPosition();
        chip = mock(ChipRow.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        PositionAPI chipPosition = position(10, 260, 300, 40);
        doReturn(chipPosition).when(chip).getPosition();
        icon = button(null, position(271, 263, 34, 34));
        chip.children.add(mock(LabelAPI.class));
        chip.children.add(button("-", position(240, 268, 24, 24)));
        chip.children.add(icon);
        list.items.add(chip);

        SettingsAPI settings = mock(SettingsAPI.class);
        ArgumentCaptor<CustomUIPanelPlugin> captor = ArgumentCaptor.forClass(CustomUIPanelPlugin.class);
        when(settings.createCustom(anyFloat(), anyFloat(), captor.capture())).thenAnswer(call -> {
            CustomPanelAPI panel = mock(CustomPanelAPI.class);
            when(panel.getPlugin()).thenReturn(call.getArgument(2));
            when(panel.getPosition()).thenReturn(mock(PositionAPI.class));
            return panel;
        });
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        buttonMock = Mockito.mockStatic(SkillTreeRefitButton.class);
        buttonMock.when(() -> SkillTreeRefitButton.openPanel(any())).thenReturn(true);

        SkillTreeChipClickTarget.attach(widget, list, chip);
        plugin = captor.getValue();
    }

    @AfterEach
    void tearDown() {
        buttonMock.close();
        globalMock.close();
        PhantomHullModRefitHider.resetForTests();
    }

    @Test
    void clickingTheChipOpensTheTreeAndSwallowsTheClickAndItsRelease() {
        InputEventAPI down = lmb(true, ICON_X, ICON_Y);
        InputEventAPI up = lmb(false, ICON_X, ICON_Y);
        plugin.processInput(List.of(down, up));

        buttonMock.verify(() -> SkillTreeRefitButton.openPanel(down));
        verify(down).consume();
        verify(up).consume();
    }

    @Test
    void aReleaseConsumedElsewhereStillEndsTheSwallowedClick() {
        InputEventAPI down = lmb(true, ICON_X, ICON_Y);
        InputEventAPI consumedUp = lmb(false, ICON_X, ICON_Y);
        when(consumedUp.isConsumed()).thenReturn(true);
        plugin.processInput(List.of(down, consumedUp));

        InputEventAPI laterUp = lmb(false, 50, 150);
        plugin.processInput(List.of(laterUp));

        verify(laterUp, never()).consume();
    }

    @Test
    void clicksOutsideTheChipAreLeftToTheGame() {
        InputEventAPI down = lmb(true, 50, 150);
        InputEventAPI up = lmb(false, 50, 150);
        plugin.processInput(List.of(down, up));

        buttonMock.verify(() -> SkillTreeRefitButton.openPanel(any()), never());
        verify(down, never()).consume();
        verify(up, never()).consume();
    }

    @Test
    void clickingTheChipsNameOrTheRestOfItsRowIsLeftToTheGame() {
        InputEventAPI onName = lmb(true, 50, ICON_Y);
        InputEventAPI onRemoveButton = lmb(true, 250, ICON_Y);
        plugin.processInput(List.of(onName, onRemoveButton));

        buttonMock.verify(() -> SkillTreeRefitButton.openPanel(any()), never());
        verify(onName, never()).consume();
        verify(onRemoveButton, never()).consume();
    }

    @Test
    void aChipScrolledOutOfTheListIsNotClickable() {
        PositionAPI scrolledAway = position(271, 323, 34, 34);
        when(icon.getPosition()).thenReturn(scrolledAway);
        InputEventAPI down = lmb(true, ICON_X, 340);
        plugin.processInput(List.of(down));

        verify(down, never()).consume();
    }

    @Test
    void aChipWithoutAnIconGetsNoClickTarget() {
        widget.children.clear();
        ChipRow iconless = mock(ChipRow.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        iconless.children.add(mock(LabelAPI.class));

        SkillTreeChipClickTarget.attach(widget, list, iconless);

        verify(widget, Mockito.times(1)).addComponent(any());
    }

    @Test
    void laterPassesRetargetTheSameOverlayAndNeverRemoveItSoOtherModsAnchoredOnItKeepWorking() {
        SkillTreeChipClickTarget.attach(widget, list, chip);
        SkillTreeChipClickTarget.attach(widget, list, null);

        verify(widget, Mockito.times(1)).addComponent(any());
        verify(widget, never()).removeComponent(any());
        InputEventAPI inert = lmb(true, ICON_X, ICON_Y);
        plugin.processInput(List.of(inert));
        verify(inert, never()).consume();

        SkillTreeChipClickTarget.attach(widget, list, chip);
        InputEventAPI live = lmb(true, ICON_X, ICON_Y);
        plugin.processInput(List.of(live));
        verify(live).consume();
    }
    @Test
    void aChipRemovedFromTheListIsNotClickable() {
        list.items.clear();
        InputEventAPI down = lmb(true, ICON_X, ICON_Y);
        plugin.processInput(List.of(down));

        verify(down, never()).consume();
    }

    @Test
    void theClickIsLeftAloneWhenThePanelCannotOpen() {
        buttonMock.when(() -> SkillTreeRefitButton.openPanel(any())).thenReturn(false);
        InputEventAPI down = lmb(true, ICON_X, ICON_Y);
        InputEventAPI up = lmb(false, ICON_X, ICON_Y);
        plugin.processInput(List.of(down, up));

        verify(down, never()).consume();
        verify(up, never()).consume();
    }

    @Test
    void aBrokenOpenerDisablesClickToOpenInsteadOfCrashing() {
        buttonMock.when(() -> SkillTreeRefitButton.openPanel(any())).thenThrow(new NoSuchMethodError("lunalib changed"));
        InputEventAPI first = lmb(true, ICON_X, ICON_Y);
        plugin.processInput(List.of(first));
        InputEventAPI second = lmb(true, ICON_X, ICON_Y);
        plugin.processInput(List.of(second));

        verify(first, never()).consume();
        buttonMock.verify(() -> SkillTreeRefitButton.openPanel(any()), Mockito.times(1));
    }

    @Test
    void containsIncludesTheEdges() {
        PositionAPI box = position(10, 20, 30, 40);
        assertTrue(SkillTreeChipClickTarget.contains(box, 10, 20));
        assertTrue(SkillTreeChipClickTarget.contains(box, 40, 60));
        assertFalse(SkillTreeChipClickTarget.contains(box, 41, 60));
        assertFalse(SkillTreeChipClickTarget.contains(null, 10, 20));
    }
}
