package exiledsector.ui.decoration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipAnchorsTest {

    private static final List<ShipAnchors.Anchor> LASHER = outline(-10, 32, 16, 33, 16, 20, 30, 21, 30, 10, 48, 15, 61, 0, 48, -12, 30, -9, 30, -20,
            16, -20, 15, -32, -14, -29, -11, -20, -23, -21, -32, 1, -22, 21, -9, 22);
    private static final List<ShipAnchors.Anchor> NARROW_HULL = outline(60, 0, 20, 8, -40, 8, -40, -8, 20, -8);
    private static final List<ShipAnchors.Anchor> PARAGON = outline(169, 37, 129, 66, 84, 95, 57, 118, 43, 117, 9, 134, -34, 134,
            -64, 117, -99, 127, -120, 140, -142, 157, -172, 146, -178, 116, -181, 88, -181, 57, -173, 38, -143, 32, -167, 3, -57, -23,
            -24, 2, -59, 26, -1, 73, 80, -1, -1, -72, -57, -23, -167, 3, -146, -31, -176, -41, -180, -55, -185, -75, -177, -112, -173,
            -144, -140, -156, -121, -139, -100, -125, -62, -115, -32, -134, 8, -134, 44, -116, 56, -118, 80, -98, 126, -66, 169, -35);

    private static List<ShipAnchors.Anchor> outline(float... coordinates) {
        List<ShipAnchors.Anchor> points = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) {
            points.add(new ShipAnchors.Anchor(coordinates[i], coordinates[i + 1]));
        }
        return points;
    }

    @Test
    void pointsInsideAndOutsideTheHullOutlineAreToldApart() {
        assertTrue(ShipAnchors.contains(LASHER, ShipAnchors.CENTRE));
        assertTrue(ShipAnchors.contains(LASHER, new ShipAnchors.Anchor(50f, 0f)));
        assertFalse(ShipAnchors.contains(LASHER, new ShipAnchors.Anchor(70f, 0f)));
        assertFalse(ShipAnchors.contains(LASHER, new ShipAnchors.Anchor(40f, 30f)));
    }

    @Test
    void theArmourRayStopsWhereItLeavesTheHull() {
        ShipAnchors.Anchor edge = ShipAnchors.rayExit(NARROW_HULL, 0f, 1f);

        assertEquals(0f, edge.forward(), 1e-4f);
        assertEquals(8f, edge.left(), 1e-4f);
        assertNull(ShipAnchors.rayExit(List.of(new ShipAnchors.Anchor(10f, 10f), new ShipAnchors.Anchor(20f, 10f),
                new ShipAnchors.Anchor(20f, 20f)), 0f, -1f));
    }

    @Test
    void anAnchorOutsideTheHullIsPulledInTowardTheCentre() {
        ShipAnchors.Anchor wide = new ShipAnchors.Anchor(0f, 30f);
        ShipAnchors.Anchor pulled = ShipAnchors.pullInside(NARROW_HULL, wide);

        assertTrue(ShipAnchors.contains(NARROW_HULL, pulled));
        assertTrue(pulled.left() > 0f && pulled.left() < 8f);
        ShipAnchors.Anchor inside = new ShipAnchors.Anchor(10f, 2f);
        assertEquals(inside, ShipAnchors.pullInside(NARROW_HULL, inside));
    }

    @Test
    void theArmourAnchorSitsOnTheHullEvenWhenTheOutlineLoopsAroundTheCentre() {
        ShipAnchors.Anchor offHull = new ShipAnchors.Anchor(57f, 139f);
        ShipAnchors.Anchor armour = ShipAnchors.armorAnchor(PARAGON, offHull);

        assertFalse(ShipAnchors.contains(PARAGON, ShipAnchors.CENTRE));
        assertFalse(ShipAnchors.contains(PARAGON, offHull));
        assertTrue(ShipAnchors.contains(PARAGON, armour));
        assertTrue(armour.left() > 73f);
    }
}
