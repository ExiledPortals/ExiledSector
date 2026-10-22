package exiledsector.ui.section;

import exiledsector.ui.decoration.HullPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectionLayoutTest {

    private static final SectionLayout.ShipExtent EXTENT = SectionLayout.ShipExtent.square(300f);

    @Test
    void socketsGoOnTheSideOfTheirAnchorAndStayBalanced() {
        List<SectionLayout.Placement> placements = SectionLayout.place(List.of(
                new HullPoint(10f, 20f), new HullPoint(-30f, 15f), new HullPoint(40f, 5f), new HullPoint(0f, -25f)),
                SectionLayout.Arrangement.BOTH_SIDES);

        long leftCount = placements.stream().filter(SectionLayout.Placement::leftSide).count();
        assertEquals(2, leftCount);
        assertFalse(placements.get(3).leftSide());
        assertTrue(placements.get(0).leftSide());
        assertTrue(placements.get(1).leftSide());
        assertFalse(placements.get(2).leftSide());
    }

    @Test
    void centrelineAnchorsAlternateAndRowsRunFrontToBack() {
        List<SectionLayout.Placement> placements = SectionLayout.place(List.of(
                new HullPoint(-20f, 0f), new HullPoint(30f, 0f), new HullPoint(50f, 0f)),
                SectionLayout.Arrangement.BOTH_SIDES);

        assertEquals(2, placements.stream().filter(SectionLayout.Placement::leftSide).count());
        SectionLayout.Placement rear = placements.get(0);
        SectionLayout.Placement bow = placements.get(2);
        assertTrue(rear.leftSide() == bow.leftSide() ? bow.row() < rear.row() : bow.row() == 0);
        for (SectionLayout.Placement placement : placements) {
            assertTrue(placement.row() < placement.rowsOnSide());
        }
    }

    @Test
    void socketsSitClearOfTheShipWithTheElbowBetweenSocketAndHull() {
        SectionLayout.Placement left = new SectionLayout.Placement(SectionLayout.Side.LEFT, 0, 2);
        SectionLayout.Placement right = new SectionLayout.Placement(SectionLayout.Side.RIGHT, 1, 2);
        float socketLeftX = SectionLayout.sectionX(left, 500f, EXTENT);
        float socketRightX = SectionLayout.sectionX(right, 500f, EXTENT);

        assertTrue(socketLeftX + SectionLayout.SECTION_RADIUS < 350f);
        assertTrue(socketRightX - SectionLayout.SECTION_RADIUS > 650f);
        assertTrue(SectionLayout.elbowX(left, socketLeftX) > SectionLayout.edgeX(left, socketLeftX));
        assertTrue(SectionLayout.elbowX(right, socketRightX) < SectionLayout.edgeX(right, socketRightX));
        assertTrue(SectionLayout.sectionY(left, 400f, EXTENT) > SectionLayout.sectionY(right, 400f, EXTENT));
        assertEquals(400f, (SectionLayout.sectionY(left, 400f, EXTENT) + SectionLayout.sectionY(right, 400f, EXTENT)) / 2f, 1e-3f);
    }

    @Test
    void theRefitArrangementPutsTheBowSocketAboveTheSternSocketBelowAndTheRestOnTheLeft() {
        List<SectionLayout.Placement> placements = SectionLayout.place(List.of(
                new HullPoint(-40f, 10f), new HullPoint(60f, 0f), new HullPoint(10f, -15f), new HullPoint(20f, 30f)),
                SectionLayout.Arrangement.LEFT_ABOVE_BELOW);

        assertEquals(SectionLayout.Side.ABOVE, placements.get(1).side());
        assertEquals(SectionLayout.Side.BELOW, placements.get(0).side());
        assertEquals(SectionLayout.Side.LEFT, placements.get(3).side());
        assertEquals(SectionLayout.Side.LEFT, placements.get(2).side());
        assertTrue(placements.get(3).row() < placements.get(2).row());
        assertTrue(placements.stream().noneMatch(placement -> placement.side() == SectionLayout.Side.RIGHT));
    }

    @Test
    void twoSocketsInTheRefitArrangementBothGoOnTheLeft() {
        List<SectionLayout.Placement> placements = SectionLayout.place(List.of(
                new HullPoint(-40f, 10f), new HullPoint(60f, 0f)), SectionLayout.Arrangement.LEFT_ABOVE_BELOW);

        assertTrue(placements.stream().allMatch(SectionLayout.Placement::leftSide));
    }

    @Test
    void endSocketsSitBeyondTheBowAndSternWithTheElbowTowardsTheShip() {
        SectionLayout.ShipExtent extent = new SectionLayout.ShipExtent(50f, 200f);
        SectionLayout.Placement above = new SectionLayout.Placement(SectionLayout.Side.ABOVE, 0, 1);
        SectionLayout.Placement below = new SectionLayout.Placement(SectionLayout.Side.BELOW, 0, 1);
        float aboveY = SectionLayout.sectionY(above, 400f, extent);
        float belowY = SectionLayout.sectionY(below, 400f, extent);

        assertEquals(450f, SectionLayout.sectionX(above, 500f, extent), 1e-3f, "shifted left by the half width so the leader bends visibly");
        assertEquals(380f, SectionLayout.sectionX(above, 500f, new SectionLayout.ShipExtent(300f, 200f)), 1e-3f);
        assertTrue(aboveY - SectionLayout.SECTION_RADIUS > 600f);
        assertTrue(belowY + SectionLayout.SECTION_RADIUS < 200f);
        assertTrue(SectionLayout.elbowY(above, aboveY) < SectionLayout.edgeY(above, aboveY));
        assertTrue(SectionLayout.elbowY(below, belowY) > SectionLayout.edgeY(below, belowY));
    }
}
