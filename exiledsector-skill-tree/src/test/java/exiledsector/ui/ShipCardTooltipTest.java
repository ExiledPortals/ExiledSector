package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ShipCardTooltipTest {

    private static final ScreenRect HOST = new ScreenRect(100f, 50f, 1600f, 900f);

    @Test
    void theTooltipOpensRightOfTheCardWithMatchingTops() {
        ScreenRect card = new ScreenRect(120f, 600f, 128f, 128f);

        ShipCardTooltip.Placement placement = ShipCardTooltip.place(card, 450f, 300f, HOST);

        assertEquals(120f + 128f + ShipCardTooltip.GAP - HOST.left(), placement.left(), 0.001f);
        assertEquals(600f + 128f - 300f - HOST.bottom(), placement.bottom(), 0.001f);
    }

    @Test
    void aCardNearTheBottomLiftsTheTooltipOntoTheScreen() {
        ScreenRect card = new ScreenRect(120f, 60f, 128f, 128f);

        ShipCardTooltip.Placement placement = ShipCardTooltip.place(card, 450f, 500f, HOST);

        assertEquals(0f, placement.bottom(), 0.001f);
    }

    @Test
    void aCardNearTheRightEdgeOpensTheTooltipOnItsLeft() {
        ScreenRect card = new ScreenRect(1500f, 400f, 128f, 128f);

        ShipCardTooltip.Placement placement = ShipCardTooltip.place(card, 450f, 300f, HOST);

        assertEquals(1500f - ShipCardTooltip.GAP - 450f - HOST.left(), placement.left(), 0.001f);
    }

    @Test
    void aTooltipTallerThanTheScreenStartsAtTheScreenBottom() {
        ScreenRect card = new ScreenRect(120f, 600f, 128f, 128f);

        ShipCardTooltip.Placement placement = ShipCardTooltip.place(card, 450f, 1200f, HOST);

        assertEquals(0f, placement.bottom(), 0.001f);
    }

    @Test
    void theEmptyTooltipNeverShows() {
        ShipCardTooltip.NONE.show(null);

        assertFalse(ShipCardTooltip.NONE.isShown());
    }
}
