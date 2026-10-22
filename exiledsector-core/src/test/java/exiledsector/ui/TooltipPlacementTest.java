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
}
