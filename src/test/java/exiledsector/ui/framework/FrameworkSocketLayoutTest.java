package exiledsector.ui.framework;

import exiledsector.ui.decoration.ShipAnchors;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameworkSocketLayoutTest {

    private static final FrameworkSocketLayout.ShipExtent EXTENT = FrameworkSocketLayout.ShipExtent.square(300f);

    @Test
    void socketsGoOnTheSideOfTheirAnchorAndStayBalanced() {
        List<FrameworkSocketLayout.Placement> placements = FrameworkSocketLayout.place(List.of(
                new ShipAnchors.Anchor(10f, 20f), new ShipAnchors.Anchor(-30f, 15f), new ShipAnchors.Anchor(40f, 5f), new ShipAnchors.Anchor(0f, -25f)),
                FrameworkSocketLayout.Arrangement.BOTH_SIDES);

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
                new ShipAnchors.Anchor(-20f, 0f), new ShipAnchors.Anchor(30f, 0f), new ShipAnchors.Anchor(50f, 0f)),
                FrameworkSocketLayout.Arrangement.BOTH_SIDES);

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
        FrameworkSocketLayout.Placement left = new FrameworkSocketLayout.Placement(FrameworkSocketLayout.Side.LEFT, 0, 2);
        FrameworkSocketLayout.Placement right = new FrameworkSocketLayout.Placement(FrameworkSocketLayout.Side.RIGHT, 1, 2);
        float socketLeftX = FrameworkSocketLayout.socketX(left, 500f, EXTENT);
        float socketRightX = FrameworkSocketLayout.socketX(right, 500f, EXTENT);

        assertTrue(socketLeftX + FrameworkSocketLayout.SOCKET_RADIUS < 350f);
        assertTrue(socketRightX - FrameworkSocketLayout.SOCKET_RADIUS > 650f);
        assertTrue(FrameworkSocketLayout.elbowX(left, socketLeftX) > FrameworkSocketLayout.edgeX(left, socketLeftX));
        assertTrue(FrameworkSocketLayout.elbowX(right, socketRightX) < FrameworkSocketLayout.edgeX(right, socketRightX));
        assertTrue(FrameworkSocketLayout.socketY(left, 400f, EXTENT) > FrameworkSocketLayout.socketY(right, 400f, EXTENT));
        assertEquals(400f, (FrameworkSocketLayout.socketY(left, 400f, EXTENT) + FrameworkSocketLayout.socketY(right, 400f, EXTENT)) / 2f, 1e-3f);
    }

    @Test
    void theRefitArrangementPutsTheBowSocketAboveTheSternSocketBelowAndTheRestOnTheLeft() {
        List<FrameworkSocketLayout.Placement> placements = FrameworkSocketLayout.place(List.of(
                new ShipAnchors.Anchor(-40f, 10f), new ShipAnchors.Anchor(60f, 0f), new ShipAnchors.Anchor(10f, -15f), new ShipAnchors.Anchor(20f, 30f)),
                FrameworkSocketLayout.Arrangement.LEFT_ABOVE_BELOW);

        assertEquals(FrameworkSocketLayout.Side.ABOVE, placements.get(1).side());
        assertEquals(FrameworkSocketLayout.Side.BELOW, placements.get(0).side());
        assertEquals(FrameworkSocketLayout.Side.LEFT, placements.get(3).side());
        assertEquals(FrameworkSocketLayout.Side.LEFT, placements.get(2).side());
        assertTrue(placements.get(3).row() < placements.get(2).row());
        assertTrue(placements.stream().noneMatch(placement -> placement.side() == FrameworkSocketLayout.Side.RIGHT));
    }

    @Test
    void twoSocketsInTheRefitArrangementBothGoOnTheLeft() {
        List<FrameworkSocketLayout.Placement> placements = FrameworkSocketLayout.place(List.of(
                new ShipAnchors.Anchor(-40f, 10f), new ShipAnchors.Anchor(60f, 0f)), FrameworkSocketLayout.Arrangement.LEFT_ABOVE_BELOW);

        assertTrue(placements.stream().allMatch(FrameworkSocketLayout.Placement::leftSide));
    }

    @Test
    void endSocketsSitBeyondTheBowAndSternWithTheElbowTowardsTheShip() {
        FrameworkSocketLayout.ShipExtent extent = new FrameworkSocketLayout.ShipExtent(50f, 200f);
        FrameworkSocketLayout.Placement above = new FrameworkSocketLayout.Placement(FrameworkSocketLayout.Side.ABOVE, 0, 1);
        FrameworkSocketLayout.Placement below = new FrameworkSocketLayout.Placement(FrameworkSocketLayout.Side.BELOW, 0, 1);
        float aboveY = FrameworkSocketLayout.socketY(above, 400f, extent);
        float belowY = FrameworkSocketLayout.socketY(below, 400f, extent);

        assertEquals(450f, FrameworkSocketLayout.socketX(above, 500f, extent), 1e-3f, "shifted left by the half width so the leader bends visibly");
        assertEquals(380f, FrameworkSocketLayout.socketX(above, 500f, new FrameworkSocketLayout.ShipExtent(300f, 200f)), 1e-3f);
        assertTrue(aboveY - FrameworkSocketLayout.SOCKET_RADIUS > 600f);
        assertTrue(belowY + FrameworkSocketLayout.SOCKET_RADIUS < 200f);
        assertTrue(FrameworkSocketLayout.elbowY(above, aboveY) < FrameworkSocketLayout.edgeY(above, aboveY));
        assertTrue(FrameworkSocketLayout.elbowY(below, belowY) > FrameworkSocketLayout.edgeY(below, belowY));
    }
}
