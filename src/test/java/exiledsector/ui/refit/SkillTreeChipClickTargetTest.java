package exiledsector.ui.refit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class SkillTreeChipClickTargetTest {

    public abstract static class RowList implements UIComponentAPI {
        public final List<Object> items = new ArrayList<>();

        public List<Object> getItems() {
            return items;
        }
    }

    private MockedStatic<Global> globalMock;
    private MockedStatic<SkillTreeRefitButton> buttonMock;
    private UIPanelAPI widget;
    private RowList list;
    private UIComponentAPI chip;
    private CustomUIPanelPlugin plugin;

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
        widget = mock(UIPanelAPI.class);
        PositionAPI widgetPosition = position(0, 0, 400, 600);
        when(widget.getPosition()).thenReturn(widgetPosition);
        when(widget.addComponent(any())).thenReturn(mock(PositionAPI.class));

        list = mock(RowList.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        PositionAPI listPosition = position(10, 100, 300, 200);
        doReturn(listPosition).when(list).getPosition();
        chip = mock(UIComponentAPI.class);
        PositionAPI chipPosition = position(10, 260, 300, 40);
        when(chip.getPosition()).thenReturn(chipPosition);
        list.items.add(chip);

        SettingsAPI settings = mock(SettingsAPI.class);
        ArgumentCaptor<CustomUIPanelPlugin> captor = ArgumentCaptor.forClass(CustomUIPanelPlugin.class);
        when(settings.createCustom(anyFloat(), anyFloat(), captor.capture())).thenReturn(mock(CustomPanelAPI.class));
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
        InputEventAPI down = lmb(true, 50, 280);
        InputEventAPI up = lmb(false, 50, 280);
        plugin.processInput(List.of(down, up));

        buttonMock.verify(() -> SkillTreeRefitButton.openPanel(down));
        verify(down).consume();
        verify(up).consume();
    }

    @Test
    void aReleaseConsumedElsewhereStillEndsTheSwallowedClick() {
        InputEventAPI down = lmb(true, 50, 280);
        InputEventAPI consumedUp = lmb(false, 50, 280);
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
    void aChipScrolledOutOfTheListIsNotClickable() {
        PositionAPI scrolledAway = position(10, 320, 300, 40);
        when(chip.getPosition()).thenReturn(scrolledAway);
        InputEventAPI down = lmb(true, 50, 340);
        plugin.processInput(List.of(down));

        verify(down, never()).consume();
    }

    @Test
    void aChipRemovedFromTheListIsNotClickable() {
        list.items.clear();
        InputEventAPI down = lmb(true, 50, 280);
        plugin.processInput(List.of(down));

        verify(down, never()).consume();
    }

    @Test
    void theClickIsLeftAloneWhenThePanelCannotOpen() {
        buttonMock.when(() -> SkillTreeRefitButton.openPanel(any())).thenReturn(false);
        InputEventAPI down = lmb(true, 50, 280);
        InputEventAPI up = lmb(false, 50, 280);
        plugin.processInput(List.of(down, up));

        verify(down, never()).consume();
        verify(up, never()).consume();
    }

    @Test
    void aBrokenOpenerDisablesClickToOpenInsteadOfCrashing() {
        buttonMock.when(() -> SkillTreeRefitButton.openPanel(any())).thenThrow(new NoSuchMethodError("lunalib changed"));
        InputEventAPI first = lmb(true, 50, 280);
        plugin.processInput(List.of(first));
        InputEventAPI second = lmb(true, 50, 280);
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
