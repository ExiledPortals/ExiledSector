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

    @Test
    void theTooltipHangsBelowTheCursorWhenItFits() {
        assertEquals(500f - 200f - OfficerBoardTooltip.CURSOR_OFFSET, OfficerBoardTooltip.boxBottom(500f, 200f, 768f));
    }

    @Test
    void theTooltipStopsAtTheBottomEdge() {
        assertEquals(OfficerBoardTooltip.SCREEN_MARGIN, OfficerBoardTooltip.boxBottom(100f, 400f, 768f));
    }

    @Test
    void aTooltipTallerThanTheScreenSitsOnTheBottomMargin() {
        assertEquals(OfficerBoardTooltip.SCREEN_MARGIN, OfficerBoardTooltip.boxBottom(700f, 900f, 768f));
    }

    @Test
    void anOverflowingTooltipShrinksItsShipByTheOverflow() {
        assertEquals(180f, OfficerBoardTooltip.fittedShipHeight(240f, 60f));
    }

    @Test
    void theShipNeverShrinksBelowItsMinimumHeight() {
        assertEquals(OfficerBoardTooltip.SHIP_MIN_HEIGHT, OfficerBoardTooltip.fittedShipHeight(240f, 500f));
    }

    @Test
    void aShipAlreadySmallerThanTheMinimumKeepsItsHeight() {
        assertEquals(50f, OfficerBoardTooltip.fittedShipHeight(50f, 100f));
    }

    @Test
    void aSmallShipIsDrawnAtFullSize() {
        assertEquals(1f, OfficerBoardTooltip.shipZoom(30f, 50f, 432f));
    }

    @Test
    void aLongShipShrinksToTheMaximumHeight() {
        assertEquals(OfficerBoardTooltip.SHIP_MAX_HEIGHT / 400f, OfficerBoardTooltip.shipZoom(60f, 200f, 432f), 1e-6f);
    }

    @Test
    void aWideShipShrinksToTheTooltipWidth() {
        assertEquals(432f / 600f, OfficerBoardTooltip.shipZoom(300f, 100f, 432f), 1e-6f);
    }
}
