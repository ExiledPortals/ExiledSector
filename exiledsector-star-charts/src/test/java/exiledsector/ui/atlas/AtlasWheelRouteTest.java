package exiledsector.ui.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasWheelRouteTest {

    private static final float TOLERANCE = 0.5f;
    private static final float OUTER_RADIUS = 100f;
    private static final float INNER_RADIUS = 50f;

    private static AtlasWheelRoute.Wheel wheel(double outerDegrees, double innerDegrees) {
        return new AtlasWheelRoute.Wheel(300f, 0f, 0f, 0f, x(OUTER_RADIUS, outerDegrees), y(OUTER_RADIUS, outerDegrees), x(INNER_RADIUS, innerDegrees),
                y(INNER_RADIUS, innerDegrees));
    }

    private static float x(float radius, double degrees) {
        return radius * (float) Math.cos(Math.toRadians(degrees));
    }

    private static float y(float radius, double degrees) {
        return radius * (float) Math.sin(Math.toRadians(degrees));
    }

    private static double degreesOf(float pointX, float pointY) {
        return Math.toDegrees(Math.atan2(pointY, pointX));
    }

    @Test
    void anOuterSmallInSightOfTheHighwayIsReachedInAStraightLine() {
        AtlasWheelRoute route = new AtlasWheelRoute();
        route.update(wheel(30.0, 200.0));
        route.trace();

        int middle = AtlasWheelRoute.APPROACH_POINTS / 2;
        float share = middle / (float) (AtlasWheelRoute.APPROACH_POINTS - 1);
        assertEquals(300f + (x(OUTER_RADIUS, 30.0) - 300f) * share, route.approachXs[middle], TOLERANCE);
        assertEquals(y(OUTER_RADIUS, 30.0) * share, route.approachYs[middle], TOLERANCE);
    }

    @Test
    void anOuterSmallBehindTheNotableIsReachedAroundItsShorterSide() {
        AtlasWheelRoute route = new AtlasWheelRoute();
        route.update(wheel(120.0, 200.0));
        route.trace();

        double tangentDegrees = Math.toDegrees(Math.acos(OUTER_RADIUS / 300.0));
        assertEquals(tangentDegrees, degreesOf(route.approachXs[8], route.approachYs[8]), 0.5);
        assertEquals(x(OUTER_RADIUS, 120.0), route.approachXs[AtlasWheelRoute.APPROACH_POINTS - 1], TOLERANCE);
        assertEquals(300f, route.approachXs[0], TOLERANCE);
    }

    @Test
    void theRouteKeepsItsSideUntilTheOtherIsClearlyShorterThenCrossesOverThePlanet() {
        AtlasWheelRoute route = new AtlasWheelRoute();
        route.update(wheel(120.0, 200.0));
        route.trace();
        route.update(wheel(-175.0, 200.0));
        route.trace();
        assertTrue(degreesOf(route.approachXs[8], route.approachYs[8]) > 0.0, "still wraps the first way");

        route.update(wheel(-120.0, 200.0));
        route.trace();
        assertTrue(route.blending());
        route.advance(AtlasWheelRoute.BLEND_SECONDS / 2f);
        route.update(wheel(-120.0, 200.0));
        route.trace();
        boolean crossesThePlanet = false;
        for (int point = 0; point < AtlasWheelRoute.APPROACH_POINTS; point++) {
            crossesThePlanet |= Math.hypot(route.approachXs[point], route.approachYs[point]) < OUTER_RADIUS * 0.6;
        }
        assertTrue(crossesThePlanet);

        route.advance(AtlasWheelRoute.BLEND_SECONDS);
        route.update(wheel(-120.0, 200.0));
        route.trace();
        assertTrue(degreesOf(route.approachXs[8], route.approachYs[8]) < 0.0, "now wraps the other way");
    }

    @Test
    void theSpiralCarriesOnTheWayTheApproachWrapsAndEndsOnTheInnerSmall() {
        AtlasWheelRoute route = new AtlasWheelRoute();
        route.update(wheel(120.0, 200.0));
        route.trace();

        int middle = (AtlasWheelRoute.SPIRAL_POINTS - 1) / 2;
        float middleX = route.spiralXs[middle];
        float middleY = route.spiralYs[middle];
        assertEquals(1, route.spiralSide());
        assertTrue(degreesOf(middleX, middleY) > 120.0 && degreesOf(middleX, middleY) < 200.0);
        assertEquals(x(INNER_RADIUS, 200.0), route.spiralXs[AtlasWheelRoute.SPIRAL_POINTS - 1], TOLERANCE);
        assertEquals(y(INNER_RADIUS, 200.0), route.spiralYs[AtlasWheelRoute.SPIRAL_POINTS - 1], TOLERANCE);
    }

    @Test
    void aRouteThatNeverWrapsSpiralsTheShorterWayToTheInnerSmall() {
        AtlasWheelRoute clockwise = new AtlasWheelRoute();
        clockwise.update(wheel(30.0, -60.0));
        AtlasWheelRoute counterClockwise = new AtlasWheelRoute();
        counterClockwise.update(wheel(30.0, 100.0));

        assertEquals(-1, clockwise.spiralSide());
        assertEquals(1, counterClockwise.spiralSide());
    }

    @Test
    void theBendIntoTheNotableTurnsOverSmoothlyWhenTheSpiralChangesSide() {
        AtlasWheelRoute route = new AtlasWheelRoute();
        route.update(wheel(120.0, 200.0));
        route.update(wheel(-120.0, 200.0));
        assertEquals(-1, route.spiralSide());
        assertEquals(1f, route.bendSide(), 0.001f);

        route.advance(AtlasWheelRoute.BLEND_SECONDS / 2f);
        assertEquals(0f, route.bendSide(), 0.001f);

        route.advance(AtlasWheelRoute.BLEND_SECONDS);
        assertEquals(-1f, route.bendSide(), 0.001f);
    }

    @Test
    void aSpiralWoundPastAFullTurnStartsAgainFromTheShortSweep() {
        AtlasWheelRoute route = new AtlasWheelRoute();
        boolean reset = false;
        for (double inner = 40.0; inner <= 460.0; inner += 20.0) {
            route.update(wheel(30.0, inner));
            reset |= route.blending();
        }
        assertTrue(reset);

        route.advance(AtlasWheelRoute.BLEND_SECONDS);
        route.update(wheel(30.0, 460.0));
        route.trace();
        int middle = (AtlasWheelRoute.SPIRAL_POINTS - 1) / 2;
        double share = middle / (double) (AtlasWheelRoute.SPIRAL_POINTS - 1);
        assertEquals(30.0 + 70.0 * share, degreesOf(route.spiralXs[middle], route.spiralYs[middle]), 0.5);
    }
}
