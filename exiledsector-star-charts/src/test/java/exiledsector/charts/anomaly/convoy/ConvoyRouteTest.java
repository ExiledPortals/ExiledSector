package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConvoyRouteTest {

    private static SectorEntityToken at(float x, float y) {
        SectorEntityToken entity = mock(SectorEntityToken.class);
        when(entity.getLocation()).thenReturn(new Vector2f(x, y));
        when(entity.getRadius()).thenReturn(50f);
        return entity;
    }

    @Test
    void stopsAreVisitedNearestFirstFromTheEntry() {
        SectorEntityToken near = at(1000f, 0f);
        SectorEntityToken middle = at(3000f, 0f);
        SectorEntityToken far = at(6000f, 0f);

        assertEquals(List.of(near, middle, far), ConvoyRoute.nearestFirst(new Vector2f(), List.of(far, near, middle)));
    }

    @Test
    void theConvoyEntersOppositeItsDestinationAndLeavesNearIt() {
        SectorEntityToken destination = at(5000f, 0f);
        for (long seed = 0; seed < 20; seed++) {
            ConvoyRoute.Plan plan = ConvoyRoute.plan(List.of(destination), List.of(), Set.of(), new Random(seed));

            assertEquals(destination, plan.destination());
            assertTrue(plan.entry().x < 0f, "the entry is on the far side");
            assertTrue(plan.exit().x > 0f, "the exit is on the destination's side");
            assertEquals(5050f + ConvoyRules.EDGE_MARGIN, plan.entry().length(), 1f);
            assertEquals(List.of(), plan.waypoints());
        }
    }

    @Test
    void aTakenDestinationIsSkippedWhileAnotherPlanetIsFree() {
        SectorEntityToken taken = at(4000f, 0f);
        SectorEntityToken free = at(-4000f, 0f);

        for (long seed = 0; seed < 10; seed++) {
            ConvoyRoute.Plan plan = ConvoyRoute.plan(List.of(taken, free), List.of(), Set.of(taken), new Random(seed));
            assertEquals(free, plan.destination());
            assertEquals(List.of(taken), plan.waypoints());
            assertNotEquals(plan.destination(), plan.waypoints().get(0));
        }
        assertNull(ConvoyRoute.plan(List.of(), List.of(at(1f, 1f)), Set.of(), new Random(1L)));
    }

    @Test
    void pathLengthAddsEveryLeg() {
        assertEquals(7f, ConvoyRoute.pathLength(new Vector2f(), List.of(new Vector2f(3f, 0f), new Vector2f(3f, 4f))), 0.001f);
    }
}
