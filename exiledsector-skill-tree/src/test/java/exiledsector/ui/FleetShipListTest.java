package exiledsector.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FleetShipListTest {

    private static final float CARD_HEIGHT = 100f;
    private static final float ROW_STEP = CARD_HEIGHT + FleetShipList.ROW_GAP;

    @Test
    void aFleetThatFitsShrinksTheListAndNeverScrolls() {
        FleetShipList.Layout layout = FleetShipList.layout(3, 2, CARD_HEIGHT, 1000f);

        assertFalse(layout.scrolls());
        assertEquals(3 * CARD_HEIGHT + 2 * FleetShipList.ROW_GAP, layout.innerHeight(), 0.001f);
        assertEquals(0f, layout.scrollOffset(), 0.001f);
    }

    @Test
    void aLongFleetScrollsTheShownShipToTheMiddle() {
        FleetShipList.Layout layout = FleetShipList.layout(20, 10, CARD_HEIGHT, 500f);

        assertTrue(layout.scrolls());
        assertEquals(500f, layout.innerHeight(), 0.001f);
        float shownShipCentre = 10 * ROW_STEP + CARD_HEIGHT / 2f;
        assertEquals(250f, shownShipCentre - layout.scrollOffset(), 0.001f);
    }

    @Test
    void shipsNearEitherEndStopTheScrollAtTheListEdge() {
        float rowsHeight = 20 * CARD_HEIGHT + 19 * FleetShipList.ROW_GAP;

        assertEquals(0f, FleetShipList.layout(20, 0, CARD_HEIGHT, 500f).scrollOffset(), 0.001f);
        assertEquals(rowsHeight - 500f, FleetShipList.layout(20, 19, CARD_HEIGHT, 500f).scrollOffset(), 0.001f);
    }

    @Test
    void anUnknownShownShipLeavesTheListAtTheTop() {
        assertEquals(0f, FleetShipList.layout(20, -1, CARD_HEIGHT, 500f).scrollOffset(), 0.001f);
    }
}
