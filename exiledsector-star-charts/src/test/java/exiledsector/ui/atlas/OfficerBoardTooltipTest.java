package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfficerBoardTooltipTest {

    @Test
    void theTooltipSitsRightOfTheCursorWhenItFits() {
        assertEquals(100f + OfficerBoardTooltip.CURSOR_OFFSET, OfficerBoardTooltip.boxLeft(100f, 300f, 1280f));
    }

    @Test
    void theTooltipFlipsLeftNearTheRightEdge() {
        assertEquals(1200f - OfficerBoardTooltip.CURSOR_OFFSET - 300f, OfficerBoardTooltip.boxLeft(1200f, 300f, 1280f));
    }

    @Test
    void theTooltipNeverLeavesTheLeftEdge() {
        assertEquals(OfficerBoardTooltip.SCREEN_MARGIN, OfficerBoardTooltip.boxLeft(300f, 1200f, 1280f));
    }
}
