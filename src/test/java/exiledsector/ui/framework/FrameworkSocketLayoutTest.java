package exiledsector.ui.framework;

import exiledsector.ui.decoration.ShipAnchors;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameworkSocketLayoutTest {

    @Test
    void socketsGoOnTheSideOfTheirAnchorAndStayBalanced() {
        List<FrameworkSocketLayout.Placement> placements = FrameworkSocketLayout.place(List.of(
                new ShipAnchors.Anchor(10f, 20f), new ShipAnchors.Anchor(-30f, 15f), new ShipAnchors.Anchor(40f, 5f), new ShipAnchors.Anchor(0f, -25f)));

        long leftCount = placements.stream().filter(FrameworkSocketLayout.Placement::leftSide).count();
        assertEquals(2, leftCount);
        assertFalse(placements.get(3).leftSide());
        assertTrue(placements.get(0).leftSide());
        assertTrue(placements.get(1).leftSide());
        assertFalse(placements.get(2).leftSide());
    }

    @Test
    void centrelineAnchorsAlternateAndRowsRunFrontToBack() {
        List<FrameworkSocketLayout.Placement> placements = FrameworkSocketLayout.place(List.of(
                new ShipAnchors.Anchor(-20f, 0f), new ShipAnchors.Anchor(30f, 0f), new ShipAnchors.Anchor(50f, 0f)));

        assertEquals(2, placements.stream().filter(FrameworkSocketLayout.Placement::leftSide).count());
        FrameworkSocketLayout.Placement rear = placements.get(0);
        FrameworkSocketLayout.Placement bow = placements.get(2);
        assertTrue(rear.leftSide() == bow.leftSide() ? bow.row() < rear.row() : bow.row() == 0);
        for (FrameworkSocketLayout.Placement placement : placements) {
            assertTrue(placement.row() < placement.rowsOnSide());
        }
    }

    @Test
    void socketsSitClearOfTheShipWithTheElbowBetweenSocketAndHull() {
        FrameworkSocketLayout.Placement left = new FrameworkSocketLayout.Placement(true, 0, 2);
        FrameworkSocketLayout.Placement right = new FrameworkSocketLayout.Placement(false, 1, 2);
        float socketLeftX = FrameworkSocketLayout.socketX(left, 500f, 300f);
        float socketRightX = FrameworkSocketLayout.socketX(right, 500f, 300f);

        assertTrue(socketLeftX + FrameworkSocketLayout.SOCKET_RADIUS < 350f);
        assertTrue(socketRightX - FrameworkSocketLayout.SOCKET_RADIUS > 650f);
        assertTrue(FrameworkSocketLayout.elbowX(left, socketLeftX) > FrameworkSocketLayout.edgeX(left, socketLeftX));
        assertTrue(FrameworkSocketLayout.elbowX(right, socketRightX) < FrameworkSocketLayout.edgeX(right, socketRightX));
        assertTrue(FrameworkSocketLayout.socketY(left, 400f) > FrameworkSocketLayout.socketY(right, 400f));
        assertEquals(400f, (FrameworkSocketLayout.socketY(left, 400f) + FrameworkSocketLayout.socketY(right, 400f)) / 2f, 1e-3f);
    }
}
