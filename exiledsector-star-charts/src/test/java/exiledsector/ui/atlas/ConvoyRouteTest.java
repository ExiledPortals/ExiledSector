package exiledsector.ui.atlas;

import exiledsector.charts.tree.AtlasLinkPath;
import exiledsector.charts.tree.AtlasTreeBodies;
import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.charts.tree.AtlasTreeBodies.Link;
import exiledsector.charts.tree.ShippedAtlasTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvoyRouteTest {

    private static final float DELTA = 0.01f;

    private static AtlasTreeBodies shipped() throws Exception {
        return ShippedAtlasTree.load().bodies();
    }

    private static List<Body> ports(AtlasTreeBodies bodies) {
        return bodies.bodies().stream().filter(body -> !body.art().convoys().isEmpty() && body.wheel() != AtlasTreeBodies.NO_WHEEL).toList();
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
    void everyPortsRouteRunsFromThePortToItsWheelCentreAlongTheConnectors() throws Exception {
        AtlasTreeBodies bodies = shipped();
        List<String> problems = new ArrayList<>();

        assertFalse(ports(bodies).isEmpty());
        for (Body port : ports(bodies)) {
            Body centre = bodies.bodies().get(port.wheel());
            ConvoyRoute route = ConvoyRoute.fromPortToWheelCentre(bodies, port);
            int last = route.size() - 1;
            assertTrue(route.size() > 2, port.nodeId());
            assertEquals(port.x(), route.x(0), DELTA, port.nodeId());
            assertEquals(port.y(), route.y(0), DELTA, port.nodeId());
            assertEquals(centre.x(), route.x(last), DELTA, port.nodeId());
            assertEquals(centre.y(), route.y(last), DELTA, port.nodeId());
            for (int point = 0; point <= last; point++) {
                if (!onSomeLink(bodies, route.x(point), route.y(point))) {
                    problems.add(port.nodeId() + " leaves the connectors at point " + point);
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void anOuterPortsConvoysPassThroughTheInnerLeadUpOnTheirWay() throws Exception {
        AtlasTreeBodies bodies = shipped();
        int passingThrough = 0;

        for (Body port : ports(bodies)) {
            Body centre = bodies.bodies().get(port.wheel());
            boolean linkedToCentre = bodies.links().stream().anyMatch(link -> link.first() == port.index() && link.second() == centre.index()
                    || link.second() == port.index() && link.first() == centre.index());
            if (linkedToCentre) {
                continue;
            }
            ConvoyRoute route = ConvoyRoute.fromPortToWheelCentre(bodies, port);
            boolean throughALeadUp = false;
            for (Body other : bodies.bodies()) {
                if (other.wheel() != centre.index() || other.index() == port.index()) {
                    continue;
                }
                for (int point = 1; point < route.size() - 1; point++) {
                    throughALeadUp |= Math.abs(route.x(point) - other.x()) < DELTA && Math.abs(route.y(point) - other.y()) < DELTA;
                }
            }
            assertTrue(throughALeadUp, port.nodeId());
            passingThrough++;
        }
        assertTrue(passingThrough > 0);
    }
}
