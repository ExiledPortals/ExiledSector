package exiledsector.ui;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipCardClickTargetTest {

    private final ShipCardTooltip tooltip = mock(ShipCardTooltip.class);
    private final PositionAPI cardPosition = position(0f, 0f, 128f, 128f);
    private final Runnable clicked = mock(Runnable.class);
    private boolean tooltipAllowed = true;
    private ShipCardClickTarget target;

    @BeforeEach
    void setUp() {
        target = new ShipCardClickTarget(clicked, () -> true, false);
        target.positionChanged(cardPosition);
        target.clipTo(position(0f, 64f, 128f, 400f));
        target.showTooltip(tooltip, () -> tooltipAllowed);
    }

    @Test
    void pointingAtTheVisiblePartOfTheCardShowsTheTooltip() {
        target.processInput(List.of(move(10, 100)));

        verify(tooltip).show(cardPosition);
    }

    @Test
    void pointingAtTheScrolledOutPartOfTheCardShowsNoTooltip() {
        target.processInput(List.of(move(10, 30)));

        verify(tooltip, never()).show(cardPosition);
        verify(tooltip).hide();
    }

    @Test
    void leavingTheCardHidesTheTooltip() {
        target.processInput(List.of(move(10, 100)));
        target.processInput(List.of(move(300, 100)));

        InOrder order = inOrder(tooltip);
        order.verify(tooltip).show(cardPosition);
        order.verify(tooltip).hide();
    }

    @Test
    void scrollingTheListHidesTheTooltip() {
        target.processInput(List.of(move(10, 100)));
        target.processInput(List.of(scroll(10, 100)));

        InOrder order = inOrder(tooltip);
        order.verify(tooltip).show(cardPosition);
        order.verify(tooltip).hide();
    }

    @Test
    void clickingHidesTheTooltipUntilThePointerLeavesAndReturns() {
        target.processInput(List.of(move(10, 100)));
        target.processInput(List.of(press(10, 100)));
        target.processInput(List.of(move(12, 100)));

        verify(clicked).run();
        verify(tooltip, times(1)).show(cardPosition);

        target.processInput(List.of(move(300, 100)));
        target.processInput(List.of(move(10, 100)));

        verify(tooltip, times(2)).show(cardPosition);
    }

    @Test
    void aCardThatMayNotShowItsTooltipKeepsItHidden() {
        tooltipAllowed = false;

        target.processInput(List.of(move(10, 100)));

        verify(tooltip, never()).show(cardPosition);
        verify(tooltip).hide();
    }

    private static PositionAPI position(float x, float y, float width, float height) {
        PositionAPI position = mock(PositionAPI.class);
        when(position.getX()).thenReturn(x);
        when(position.getY()).thenReturn(y);
        when(position.getWidth()).thenReturn(width);
        when(position.getHeight()).thenReturn(height);
        return position;
    }

    private static InputEventAPI move(int x, int y) {
        InputEventAPI event = at(x, y);
        when(event.isMouseMoveEvent()).thenReturn(true);
        return event;
    }

    private static InputEventAPI scroll(int x, int y) {
        InputEventAPI event = at(x, y);
        when(event.isMouseScrollEvent()).thenReturn(true);
        return event;
    }

    private static InputEventAPI press(int x, int y) {
        InputEventAPI event = at(x, y);
        when(event.isLMBDownEvent()).thenReturn(true);
        return event;
    }

    private static InputEventAPI at(int x, int y) {
        InputEventAPI event = mock(InputEventAPI.class);
        when(event.getX()).thenReturn(x);
        when(event.getY()).thenReturn(y);
        return event;
    }
}
