package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OfficerBoardTooltipTest {

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
    void theShipKeepsRoomForWeaponsAtEachEnd() {
        assertEquals(100f * (1f + OfficerBoardTooltip.SHIP_OVERHANG_SHARE * 2f), OfficerBoardTooltip.withOverhang(100f), 1e-4f);
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
