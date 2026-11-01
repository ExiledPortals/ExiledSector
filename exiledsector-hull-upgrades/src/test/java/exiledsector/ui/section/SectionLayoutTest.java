package exiledsector.ui.section;

import exiledsector.ui.decoration.HullPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectionLayoutTest {

    private static final SectionLayout.ShipExtent EXTENT = SectionLayout.ShipExtent.square(300f);

    @Test
    void socketsSitClearOfTheShipWithTheElbowBetweenSocketAndHull() {
        SectionLayout.Placement top = new SectionLayout.Placement(SectionLayout.Side.LEFT, 0, 2);
        SectionLayout.Placement bottom = new SectionLayout.Placement(SectionLayout.Side.LEFT, 1, 2);
        float socketX = SectionLayout.sectionX(top, 500f, EXTENT);

        assertTrue(socketX + SectionLayout.SECTION_RADIUS < 350f);
        assertTrue(SectionLayout.elbowX(top, socketX) > SectionLayout.edgeX(top, socketX));
        assertTrue(SectionLayout.sectionY(top, 400f, EXTENT) > SectionLayout.sectionY(bottom, 400f, EXTENT));
        assertEquals(400f, (SectionLayout.sectionY(top, 400f, EXTENT) + SectionLayout.sectionY(bottom, 400f, EXTENT)) / 2f, 1e-3f);
    }

    @Test
    void theRefitArrangementPutsTheBowSocketAboveTheSternSocketBelowAndTheRestOnTheLeft() {
        List<SectionLayout.Placement> placements = SectionLayout.place(List.of(
                new HullPoint(-40f, 10f), new HullPoint(60f, 0f), new HullPoint(10f, -15f), new HullPoint(20f, 30f)));

        assertEquals(SectionLayout.Side.ABOVE, placements.get(1).side());
        assertEquals(SectionLayout.Side.BELOW, placements.get(0).side());
        assertEquals(SectionLayout.Side.LEFT, placements.get(3).side());
        assertEquals(SectionLayout.Side.LEFT, placements.get(2).side());
        assertTrue(placements.get(3).row() < placements.get(2).row());
    }

    @Test
    void twoSocketsInTheRefitArrangementBothGoOnTheLeft() {
        List<SectionLayout.Placement> placements = SectionLayout.place(List.of(
                new HullPoint(-40f, 10f), new HullPoint(60f, 0f)));

        assertTrue(placements.stream().allMatch(placement -> placement.side() == SectionLayout.Side.LEFT));
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
