package exiledsector.charts.tree;

import exiledsector.charts.tree.AtlasTreeBodies.Link;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasTreeBodiesTest {

    private static final String TREE = """
            { "nodes": [
                { "id": "root", "type": "gate", "connectedTo": ["a1"], "x": 0, "y": 0 },
                { "id": "a1", "type": "salvage", "connectedTo": ["root", "a2", "b2"], "x": 100, "y": 0 },
                { "id": "a2", "type": "salvage", "connectedTo": ["a1", "a3"], "x": -100, "y": 0 },
                { "id": "a3", "type": "salvage", "connectedTo": ["a2", "out"], "x": 0, "y": -100 },
                { "id": "b1", "type": "salvage", "connectedTo": ["b2"], "x": 200, "y": 0 },
                { "id": "b2", "type": "salvage", "connectedTo": ["b1", "a1"], "x": 0, "y": 200 },
                { "id": "planet", "type": "hoard", "connectedTo": ["in"], "x": 0, "y": -260 },
                { "id": "out", "type": "salvage", "connectedTo": ["a3", "in"], "x": 40, "y": -260 },
                { "id": "in", "type": "salvage", "connectedTo": ["out", "planet"], "x": -20, "y": -260 }
            ] }
            """;

    private static final String BODIES = """
            { "bodies": [
                { "id": "root", "art": { "kind": "star", "size": 30 } },
                { "id": "a1", "highway": true },
                { "id": "a2", "highway": true },
                { "id": "a3", "highway": true },
                { "id": "b1", "highway": true },
                { "id": "b2", "highway": true },
                { "id": "planet", "colour": "#ffaa50", "spiral": -1, "art": { "kind": "planet", "planet": "barren", "size": 12, "light": "#ffbe5a" } },
                { "id": "out", "wheel": "planet", "art": { "kind": "sprite", "path": "graphics/ships/nav_buoy.png", "size": 4, "facing": "travel", "spin": 0.2, "heading": 45 } },
                { "id": "in", "wheel": "planet", "art": { "kind": "shrouded", "hulls": ["picket", "warden"], "size": 6 } }
            ] }
            """;

    private static AtlasTreeData data() {
        try {
            return AtlasTreeData.parse(new JSONObject(AtlasTreeFixtures.TYPES), new JSONObject(TREE), new JSONObject(BODIES));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Set<String> describe(AtlasTreeBodies bodies, AtlasTreeBodies.Shape shape) {
        return bodies.links().stream().filter(link -> link.shape() == shape).map(link -> describe(bodies, link)).collect(Collectors.toSet());
    }

    private static Link linkBetween(AtlasTreeBodies bodies, String pair) {
        return bodies.links().stream().filter(link -> describe(bodies, link).equals(pair)).findFirst().orElseThrow();
    }

    private static String describe(AtlasTreeBodies bodies, Link link) {
        String first = bodies.bodies().get(link.first()).nodeId();
        String second = bodies.bodies().get(link.second()).nodeId();
        return first.compareTo(second) < 0 ? first + "-" + second : second + "-" + first;
    }

    @Test
    void ringNeighboursFollowTheRingWheelLinksSpiralIntoTheirCentreAndEveryOtherLinkIsStraight() {
        AtlasTreeBodies bodies = data().bodies();

        assertEquals(Set.of("a1-a2", "a2-a3", "b1-b2"), describe(bodies, AtlasTreeBodies.Shape.ALONG_RING));
        assertEquals(Set.of("a3-out", "in-out", "in-planet"), describe(bodies, AtlasTreeBodies.Shape.SPIRAL));
        assertEquals(Set.of("a1-root", "a1-b2"), describe(bodies, AtlasTreeBodies.Shape.STRAIGHT));
        assertEquals(100f, bodies.ringRadius(bodies.links().stream().filter(Link::alongRing).findFirst().orElseThrow()), 0.01f);
    }

    @Test
    void aSpiralRunsFromBodyToBodyAndClosesInOnItsWheelCentreAllTheWay() {
        AtlasTreeBodies bodies = data().bodies();
        AtlasTreeBodies.Body planet = bodies.body("planet");

        for (String pair : List.of("a3-out", "in-out", "in-planet")) {
            Link link = linkBetween(bodies, pair);
            AtlasTreeBodies.Body first = bodies.bodies().get(link.first());
            AtlasTreeBodies.Body second = bodies.bodies().get(link.second());
            AtlasLinkPath path = link.path();
            assertEquals(first.x(), path.x(0), 0.01f, pair);
            assertEquals(first.y(), path.y(0), 0.01f, pair);
            assertEquals(second.x(), path.x(path.size() - 1), 0.01f, pair);
            assertEquals(second.y(), path.y(path.size() - 1), 0.01f, pair);
            double previous = Double.NaN;
            boolean inward = Math.hypot(first.x() - planet.x(), first.y() - planet.y()) >= Math.hypot(second.x() - planet.x(), second.y() - planet.y());
            for (int point = 0; point < path.size(); point++) {
                double reach = Math.hypot(path.x(point) - planet.x(), path.y(point) - planet.y());
                if (!Double.isNaN(previous)) {
                    assertTrue(inward ? reach <= previous + 0.01 : reach >= previous - 0.01, pair + " point " + point);
                }
                previous = reach;
            }
        }
        Link hook = linkBetween(bodies, "in-planet");
        assertTrue(hook.path().size() > 2);
    }

    @Test
    void bodiesStayWhereTheTreePlacesThemAndOnlyTheTreesOwnLinksAreNeighbours() {
        AtlasTreeData data = data();

        assertEquals(-260f, data.bodies().body("planet").y());
        assertEquals(40f, data.bodies().body("out").x());
        assertEquals(Set.of("b1", "a1"), data.topology().dependents("b2").stream().map(AtlasTreeNode::getId).collect(Collectors.toSet()));
    }

    @Test
    void artColourHeadingAndHighwayLoadFromTheBodiesFile() {
        AtlasTreeBodies bodies = data().bodies();

        AtlasTreeBodies.Body planet = bodies.body("planet");
        assertEquals(AtlasBodyArt.Kind.PLANET, planet.art().kind());
        assertEquals("barren", planet.art().planetType());
        assertEquals(12f, planet.art().size());
        assertEquals("#ffbe5a", planet.art().lightColour());
        assertEquals("#ffaa50", planet.colour());
        assertEquals(AtlasBodyArt.DEFAULT_HEADING, planet.art().heading());
        assertFalse(planet.highway());
        assertTrue(bodies.body("a1").highway());
        assertEquals(AtlasBodyArt.Facing.TRAVEL, bodies.body("out").art().facing());
        assertEquals(0.2f, bodies.body("out").art().spinRate());
        assertEquals(45f, bodies.body("out").art().heading());
        assertEquals(List.of("graphics/ships/nav_buoy.png"), bodies.body("out").art().spritePaths());
        assertEquals(List.of("picket", "warden"), bodies.body("in").art().hullIds());
        assertEquals(AtlasBodyArt.Kind.SHROUDED, bodies.body("in").art().kind());
        assertTrue(bodies.body("in").art().isShipGroup());
        assertEquals(bodies.body("planet").index(), bodies.body("in").wheel());
        assertEquals(-1, bodies.body("planet").spiral());
        assertEquals(AtlasTreeBodies.NO_WHEEL, bodies.body("a1").wheel());
        assertEquals("root", bodies.star().nodeId());
    }

    @Test
    void withoutABodiesFileEveryNodeIsAPlainBodyAndEveryLinkIsStraight() throws JSONException {
        AtlasTreeBodies bodies = AtlasTreeData.parse(new JSONObject(AtlasTreeFixtures.TYPES), new JSONObject(TREE)).bodies();

        assertEquals(AtlasBodyArt.Kind.PLAIN, bodies.body("a1").art().kind());
        assertNull(bodies.star());
        assertTrue(bodies.links().stream().allMatch(link -> link.shape() == AtlasTreeBodies.Shape.STRAIGHT));
    }
}
