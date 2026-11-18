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
                { "id": "planet", "colour": "#ffaa50", "art": { "kind": "planet", "planet": "barren", "size": 12, "light": "#ffbe5a" } },
                { "id": "out", "art": { "kind": "sprite", "path": "graphics/ships/nav_buoy.png", "size": 4, "facing": "travel", "spin": 0.2, "heading": 45 } },
                { "id": "in", "art": { "kind": "derelicts", "hulls": ["picket", "warden"], "size": 6 } }
            ] }
            """;

    private static AtlasTreeData data() {
        try {
            return AtlasTreeData.parse(new JSONObject(AtlasTreeFixtures.TYPES), new JSONObject(TREE), new JSONObject(BODIES));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Set<String> describe(AtlasTreeBodies bodies, boolean alongRing) {
        return bodies.links().stream().filter(link -> link.alongRing() == alongRing).map(link -> describe(bodies, link)).collect(Collectors.toSet());
    }

    private static String describe(AtlasTreeBodies bodies, Link link) {
        String first = bodies.bodies().get(link.first()).nodeId();
        String second = bodies.bodies().get(link.second()).nodeId();
        return first.compareTo(second) < 0 ? first + "-" + second : second + "-" + first;
    }

    @Test
    void linksBetweenHighwayNodesTheSameDistanceFromTheStarFollowTheRingAndEveryOtherLinkIsStraight() {
        AtlasTreeBodies bodies = data().bodies();

        assertEquals(Set.of("a1-a2", "a2-a3", "b1-b2"), describe(bodies, true));
        assertEquals(Set.of("a1-root", "a1-b2", "a3-out", "in-out", "in-planet"), describe(bodies, false));
        assertEquals(100f, bodies.ringRadius(bodies.links().stream().filter(Link::alongRing).findFirst().orElseThrow()), 0.01f);
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
        assertTrue(bodies.body("in").art().isShipGroup());
        assertEquals("root", bodies.star().nodeId());
    }

    @Test
    void withoutABodiesFileEveryNodeIsAPlainBodyAndEveryLinkIsStraight() throws JSONException {
        AtlasTreeBodies bodies = AtlasTreeData.parse(new JSONObject(AtlasTreeFixtures.TYPES), new JSONObject(TREE)).bodies();

        assertEquals(AtlasBodyArt.Kind.PLAIN, bodies.body("a1").art().kind());
        assertNull(bodies.star());
        assertTrue(bodies.links().stream().noneMatch(Link::alongRing));
    }
}
