package exiledsector.ui.slipstream;

import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorridorSearchTest {

    private static final ViewWindow VIEW = new ViewWindow(-5000f, -3000f, 5000f, 3000f);
    private static final float WIDTH = 400f;
    private static final float OBSTACLE_RADIUS = 500f;

    @Test
    void routesStayClearOfEveryObstacleByMoreThanTheWidenedStream() {
        List<Vector2f> obstacles = new ArrayList<>();
        for (float x = -4000f; x <= 4000f; x += 2600f) {
            for (float y = -2600f; y <= 2600f; y += 2600f) {
                obstacles.add(new Vector2f(x, y));
            }
        }
        float halfStream = WIDTH * Slipstream.MAX_WIDTH_SHARE / 2f + WIDTH * Slipstream.WOBBLE_SHARE;
        int planned = 0;
        for (int seed = 0; seed < 20; seed++) {
            CorridorGrid grid = CorridorGrid.covering(VIEW, WIDTH);
            for (Vector2f obstacle : obstacles) {
                grid.blockCircle(obstacle.x, obstacle.y, OBSTACLE_RADIUS);
            }
            SlipstreamRoute route = CorridorSearch.route(grid, VIEW, WIDTH, new Random(seed));
            if (route == null) {
                continue;
            }
            planned++;
            for (float distance = 0f; distance <= route.length(); distance += WIDTH / 8f) {
                Vector2f point = route.pointAt(distance);
                for (Vector2f obstacle : obstacles) {
                    float gap = Vector2f.sub(point, obstacle, null).length() - OBSTACLE_RADIUS;
                    assertTrue(gap > halfStream, "route came within " + gap + " of an obstacle");
                }
            }
        }
        assertTrue(planned >= 10, "only " + planned + " of 20 routes planned");
    }

    @Test
    void routesStartAndEndOutsideTheViewAndCrossIt() {
        CorridorGrid grid = CorridorGrid.covering(VIEW, WIDTH);
        SlipstreamRoute route = CorridorSearch.route(grid, VIEW, WIDTH, new Random(3));

        assertNotNull(route);
        Vector2f start = route.points().get(0);
        Vector2f end = route.points().get(route.points().size() - 1);
        assertFalse(VIEW.contains(start.x, start.y));
        assertFalse(VIEW.contains(end.x, end.y));
        assertTrue(CorridorSearch.isAcceptable(route.points(), VIEW, WIDTH));
    }

    @Test
    void routesComeFromFarOffScreenAndLeaveFarOffScreen() {
        float farOff = Math.max(VIEW.width(), VIEW.height()) * 0.4f;
        ViewWindow nearView = VIEW.expanded(farOff);
        for (int seed = 0; seed < 10; seed++) {
            SlipstreamRoute route = CorridorSearch.route(CorridorGrid.covering(VIEW, WIDTH), VIEW, WIDTH, new Random(seed));
            assertNotNull(route);
            Vector2f start = route.points().get(0);
            Vector2f end = route.points().get(route.points().size() - 1);
            assertFalse(nearView.contains(start.x, start.y), "started at " + start);
            assertFalse(nearView.contains(end.x, end.y), "ended at " + end);
        }
    }

    @Test
    void routesTurnInGentleCurves() {
        float maxTurnDegrees = 20f;
        for (int seed = 0; seed < 20; seed++) {
            CorridorGrid grid = CorridorGrid.covering(VIEW, WIDTH);
            grid.blockCircle(0f, 0f, OBSTACLE_RADIUS);
            grid.blockCircle(-3000f, 1800f, OBSTACLE_RADIUS);
            grid.blockCircle(3000f, -1800f, OBSTACLE_RADIUS);
            SlipstreamRoute route = CorridorSearch.route(grid, VIEW, WIDTH, new Random(seed));
            if (route == null) {
                continue;
            }
            for (float distance = WIDTH; distance < route.length() - WIDTH; distance += WIDTH / 2f) {
                Vector2f before = route.headingAt(distance - WIDTH / 2f);
                Vector2f after = route.headingAt(distance);
                double turn = Math.toDegrees(Math.acos(Math.max(-1f, Math.min(1f, Vector2f.dot(before, after)))));
                assertTrue(turn < maxTurnDegrees, "turned " + turn + " degrees at " + distance + " with seed " + seed);
            }
        }
    }

    @Test
    void aFullyBlockedViewHasNoRoute() {
        CorridorGrid grid = CorridorGrid.covering(VIEW, WIDTH);
        grid.blockCircle(0f, 0f, 20000f);

        assertNull(CorridorSearch.route(grid, VIEW, WIDTH, new Random(1)));
    }

    @Test
    void aWallAcrossTheViewOnlyLetsRoutesThroughItsGap() {
        float gapCenterY = 1000f;
        float gapHalfHeight = 900f;
        List<Vector2f> wall = new ArrayList<>();
        for (float y = -14000f; y <= 14000f; y += 200f) {
            if (Math.abs(y - gapCenterY) > gapHalfHeight) {
                wall.add(new Vector2f(0f, y));
            }
        }
        for (int seed = 0; seed < 10; seed++) {
            CorridorGrid grid = CorridorGrid.covering(VIEW, WIDTH);
            for (Vector2f post : wall) {
                grid.blockCircle(post.x, post.y, 100f);
            }
            SlipstreamRoute route = CorridorSearch.route(grid, VIEW, WIDTH, new Random(seed));
            if (route == null) {
                continue;
            }
            for (int i = 1; i < route.points().size(); i++) {
                Vector2f from = route.points().get(i - 1);
                Vector2f to = route.points().get(i);
                if ((from.x < 0f) != (to.x < 0f)) {
                    float crossingY = from.y + (to.y - from.y) * (0f - from.x) / (to.x - from.x);
                    assertTrue(Math.abs(crossingY - gapCenterY) < gapHalfHeight, "crossed the wall at " + crossingY);
                }
            }
        }
    }
}
