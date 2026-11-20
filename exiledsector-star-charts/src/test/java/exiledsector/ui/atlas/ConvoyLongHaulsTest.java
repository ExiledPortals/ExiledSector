package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasLinkPath;
import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.charts.tree.AtlasTreeBodies.Link;
import exiledsector.charts.tree.ShippedAtlasTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvoyLongHaulsTest {

    private static final float DELTA = 0.01f;
    private static final float STEP_SECONDS = 0.5f;

    private static AtlasTreeBodies shipped() throws Exception {
        return ShippedAtlasTree.load().bodies();
    }

    private static boolean linked(AtlasTreeBodies bodies, Body first, Body second) {
        return bodies.links().stream().anyMatch(link -> link.first() == first.index() && link.second() == second.index()
                || link.second() == first.index() && link.first() == second.index());
    }

    private static boolean onSomeLink(AtlasTreeBodies bodies, float x, float y) {
        for (Link link : bodies.links()) {
            AtlasLinkPath path = link.path();
            for (int point = 0; point < path.size(); point++) {
                if (Math.abs(path.x(point) - x) < DELTA && Math.abs(path.y(point) - y) < DELTA) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void everyConvoyWheelHasOneTerminalAtItsOutermostNodeOnTheHighway() throws Exception {
        AtlasTreeBodies bodies = shipped();
        Set<Integer> convoyWheels = new HashSet<>();
        for (Body body : bodies.bodies()) {
            if (!body.art().convoys().isEmpty() && body.wheel() != AtlasTreeBodies.NO_WHEEL) {
                convoyWheels.add(body.wheel());
            }
        }
        List<ConvoyLongHauls.Terminal> terminals = ConvoyLongHauls.terminalsOf(bodies);

        assertEquals(convoyWheels.size(), terminals.size());
        Set<Integer> seenWheels = new HashSet<>();
        for (ConvoyLongHauls.Terminal terminal : terminals) {
            Body gate = terminal.body();
            assertTrue(seenWheels.add(gate.wheel()), gate.nodeId());
            assertTrue(convoyWheels.contains(gate.wheel()), gate.nodeId());
            assertTrue(bodies.bodies().stream().anyMatch(other -> other.highway() && linked(bodies, gate, other)), gate.nodeId());
            assertFalse(terminal.convoys().isEmpty(), gate.nodeId());
            assertFalse(terminal.haulers().isEmpty(), gate.nodeId());
        }
    }

    @Test
    void everyRouteBetweenTerminalsKeepsToTheConnectorsAndOnlyCrossesTheHighway() throws Exception {
        AtlasTreeBodies bodies = shipped();
        List<ConvoyLongHauls.Terminal> terminals = ConvoyLongHauls.terminalsOf(bodies);
        List<String> problems = new ArrayList<>();

        for (ConvoyLongHauls.Terminal from : terminals) {
            for (ConvoyLongHauls.Terminal to : terminals) {
                if (from == to) {
                    continue;
                }
                String name = from.body().nodeId() + " to " + to.body().nodeId();
                ConvoyRoute route = ConvoyRoute.acrossTheAtlas(bodies, from.body(), to.body());
                int last = route.size() - 1;
                if (Math.abs(route.x(0) - from.body().x()) > DELTA || Math.abs(route.y(last) - to.body().y()) > DELTA || route.size() <= 2) {
                    problems.add(name + " does not run between the terminals");
                }
                for (int point = 0; point <= last; point++) {
                    if (!onSomeLink(bodies, route.x(point), route.y(point))) {
                        problems.add(name + " leaves the connectors at point " + point);
                    }
                    for (Body body : bodies.bodies()) {
                        boolean atBody = Math.abs(route.x(point) - body.x()) < DELTA && Math.abs(route.y(point) - body.y()) < DELTA;
                        boolean allowed = body.highway() && body != bodies.star() || body == from.body() || body == to.body();
                        if (atBody && !allowed) {
                            problems.add(name + " passes through " + body.nodeId());
                        }
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void longHaulsAreAlreadyUnderWayWhenTheScreenOpensAndKeepDepartingAndArriving() throws Exception {
        ConvoyLongHauls longHauls = new ConvoyLongHauls(shipped(), new Random(7L));
        long departedDuringWarmUp = longHauls.departures();

        assertTrue(departedDuringWarmUp > 0);
        assertFalse(longHauls.trips().isEmpty());
        for (float seconds = 0f; seconds < 1200f; seconds += STEP_SECONDS) {
            longHauls.advance(STEP_SECONDS);
            for (ConvoyLongHauls.Trip trip : longHauls.trips()) {
                assertNotSame(trip.from(), trip.to());
                assertFalse(trip.arrived());
            }
        }
        assertTrue(longHauls.departures() > departedDuringWarmUp + longHauls.terminals().size());
    }

    @Test
    void eachTerminalDepartsOnlyInfrequently() throws Exception {
        ConvoyLongHauls longHauls = new ConvoyLongHauls(shipped(), new Random(11L));
        long before = longHauls.departures();
        float watchedSeconds = 1200f;

        for (float seconds = 0f; seconds < watchedSeconds; seconds += STEP_SECONDS) {
            longHauls.advance(STEP_SECONDS);
        }
        long perTerminal = (longHauls.departures() - before) / longHauls.terminals().size();
        assertTrue(perTerminal <= (long) (watchedSeconds / ConvoyLongHauls.MIN_WAIT_SECONDS) + 1L);
    }
}
