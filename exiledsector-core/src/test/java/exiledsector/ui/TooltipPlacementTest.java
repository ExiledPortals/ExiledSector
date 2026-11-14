package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TooltipPlacementTest {

    private static final float SCREEN_WIDTH = 1920f;

    @Test
    void aTooltipThatFitsGoesRightOfTheCursor() {
        assertEquals(518f, SkillTreePanelStyle.tooltipLeft(500f, 400f, SCREEN_WIDTH));
    }

    @Test
    void aTooltipThatWouldCrossTheRightEdgeFlipsLeftOfTheCursor() {
        float left = SkillTreePanelStyle.tooltipLeft(1700f, 400f, SCREEN_WIDTH);

        assertEquals(1282f, left);
        assertTrue(left + 400f <= SCREEN_WIDTH);
    }

    @Test
    void aTooltipTooWideForEitherSideStaysOnScreenFromTheLeft() {
        assertEquals(4f, SkillTreePanelStyle.tooltipLeft(300f, 1000f, 1200f));
    }

    @Test
    void aTooltipThatFitsHangsBelowTheCursor() {
        assertEquals(282f, SkillTreePanelStyle.tooltipBottom(500f, 200f, 768f));
    }

    @Test
    void aTooltipThatWouldCrossTheBottomEdgeStopsOnTheMargin() {
        assertEquals(SkillTreePanelStyle.TOOLTIP_SCREEN_MARGIN, SkillTreePanelStyle.tooltipBottom(100f, 400f, 768f));
    }

    @Test
    void aTooltipTallerThanTheScreenSitsOnTheBottomMargin() {
        assertEquals(SkillTreePanelStyle.TOOLTIP_SCREEN_MARGIN, SkillTreePanelStyle.tooltipBottom(700f, 900f, 768f));
    }
}
