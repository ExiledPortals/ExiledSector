package exiledsector.charts.tree;

import exiledsector.charts.tree.AtlasOrbitSystem.Link;
import exiledsector.charts.tree.AtlasOrbitSystem.LinkKind;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasOrbitSystemTest {

    private static final String TREE = """
            { "nodes": [
                { "id": "root", "type": "gate", "connectedTo": ["a1"], "x": 0, "y": 0 },
                { "id": "a1", "type": "salvage", "connectedTo": ["root", "a2", "b2"], "x": 100, "y": 0 },
                { "id": "a2", "type": "salvage", "connectedTo": ["a1", "a3"], "x": -100, "y": 0 },
                { "id": "a3", "type": "salvage", "connectedTo": ["a2", "out"], "x": 0, "y": -100 },
                { "id": "b1", "type": "salvage", "connectedTo": ["b2"], "x": 200, "y": 0 },
                { "id": "b2", "type": "salvage", "connectedTo": ["b1", "b3", "a1"], "x": 0, "y": 200 },
                { "id": "b3", "type": "salvage", "connectedTo": ["b2"], "x": -200, "y": 0 },
                { "id": "planet", "type": "hoard", "connectedTo": ["in"], "x": 0, "y": -260 },
                { "id": "out", "type": "salvage", "connectedTo": ["a3", "in"], "x": 40, "y": -260 },
                { "id": "in", "type": "salvage", "connectedTo": ["out", "planet"], "x": -20, "y": -260 }
            ] }
            """;

    private static final String ORBITS = """
            { "orbits": [
                { "id": "root", "art": { "kind": "star", "size": 30 } },
                { "id": "a1", "around": "root", "speed": 2 },
                { "id": "a2", "around": "root", "speed": 2 },
                { "id": "a3", "around": "root", "speed": 2 },
                { "id": "b1", "around": "root", "speed": 1 },
                { "id": "b2", "around": "root", "speed": 1 },
                { "id": "b3", "around": "root", "speed": 1 },
                { "id": "planet", "around": "root", "speed": 2, "colour": "#ffaa50", "art": { "kind": "planet", "planet": "barren", "size": 12 } },
                { "id": "out", "around": "planet", "speed": 5, "art": { "kind": "sprite", "path": "graphics/ships/nav_buoy.png", "size": 4, "facing": "travel" } },
                { "id": "in", "around": "planet", "speed": 9 }
            ] }
            """;

    private static AtlasTreeData data() {
        try {
            return AtlasTreeData.parse(new JSONObject(AtlasTreeFixtures.TYPES), new JSONObject(TREE), new JSONObject(ORBITS));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Link linkBetween(AtlasOrbitSystem system, String firstId, String secondId) {
        int first = system.body(firstId).index();
        int second = system.body(secondId).index();
        return system.links().stream()
                .filter(link -> (link.first() == first && link.second() == second) || (link.first() == second && link.second() == first))
                .findFirst().orElseThrow();
    }

    private static String anchorOf(AtlasOrbitSystem system, Link link) {
        return system.bodies().get(link.first()).nodeId();
    }

    @Test
    void linksWithinOneTrackFollowItWhileLinksBetweenOrbitsBecomeTethersHangingFromTheOuterOrParentBody() {
        AtlasOrbitSystem system = data().orbits();

        assertEquals(LinkKind.ARC, linkBetween(system, "a1", "a2").kind());
        assertEquals(LinkKind.STRAIGHT, linkBetween(system, "root", "a1").kind());
        assertEquals(LinkKind.STRAIGHT, linkBetween(system, "in", "planet").kind());
        Link ringTether = linkBetween(system, "a1", "b2");
        assertEquals(LinkKind.TETHER, ringTether.kind());
        assertEquals("b2", anchorOf(system, ringTether));
        Link spoke = linkBetween(system, "a3", "out");
        assertEquals(LinkKind.TETHER, spoke.kind());
        assertEquals("a3", anchorOf(system, spoke));
        Link moonTether = linkBetween(system, "out", "in");
        assertEquals(LinkKind.TETHER, moonTether.kind());
        assertEquals("out", anchorOf(system, moonTether));
    }

    @Test
    void aTetherLandsOnItsWholeTrackSoEveryBodyOnThatTrackNeighboursItsAnchor() {
        AtlasTreeData data = data();

        Set<String> fromAnchor = data.topology().dependents("b2").stream().map(AtlasTreeNode::getId).collect(Collectors.toSet());
        Set<String> fromRing = data.topology().dependents("a2").stream().map(AtlasTreeNode::getId).collect(Collectors.toSet());

        assertTrue(fromAnchor.containsAll(List.of("a1", "a2", "a3", "b1", "b3")), fromAnchor.toString());
        assertTrue(fromRing.contains("b2"), fromRing.toString());
        assertTrue(data.node("a2").getConnectedNodeIds().contains("b2"));
        assertTrue(data.node("b2").getConnectedNodeIds().contains("a2"));
    }

    @Test
    void bodiesTurnWithTheirTrackAndMoonsCircleTheirPlanetWithoutStretchingAnyLink() {
        AtlasOrbitSystem system = data().orbits();
        AtlasOrbitSystem.Positions positions = system.newPositions();

        for (double seconds : new double[]{0.0, 7.5, 61.0, 3600.25}) {
            system.positionsAt(seconds, positions);
            int planet = system.body("planet").index();
            int outer = system.body("out").index();
            int inner = system.body("in").index();
            int ringNode = system.body("a3").index();
            assertEquals(40f, distance(positions, outer, planet), 0.01f);
            assertEquals(20f, distance(positions, inner, planet), 0.01f);
            assertEquals(160f, distance(positions, ringNode, planet), 0.01f);
            assertEquals(200f, distance(positions, system.body("b2").index(), system.body("root").index()), 0.01f);
        }

        system.positionsAt(45.0, positions);
        int ringNode = system.body("a1").index();
        assertEquals(100f * (float) Math.cos(Math.toRadians(90.0)), positions.x(ringNode), 0.01f);
        assertEquals(100f * (float) Math.sin(Math.toRadians(90.0)), positions.y(ringNode), 0.01f);
    }

    @Test
    void orbitArtAndColourLoadFromTheOrbitsFile() {
        AtlasOrbitSystem system = data().orbits();

        AtlasOrbitSystem.Body planet = system.body("planet");
        assertNotNull(planet);
        assertEquals(AtlasBodyArt.Kind.PLANET, planet.art().kind());
        assertEquals("barren", planet.art().planetType());
        assertEquals(12f, planet.art().size());
        assertEquals("#ffaa50", planet.colour());
        assertEquals(AtlasBodyArt.Facing.TRAVEL, system.body("out").art().facing());
        assertEquals(List.of("graphics/ships/nav_buoy.png"), system.body("out").art().spritePaths());
        assertEquals(AtlasBodyArt.Kind.PLAIN, system.body("in").art().kind());
    }

    @Test
    void withoutAnOrbitsFileEveryNodeStaysWhereTheTreePlacesIt() throws JSONException {
        AtlasTreeData data = AtlasTreeData.parse(new JSONObject(AtlasTreeFixtures.TYPES), new JSONObject(TREE));
        AtlasOrbitSystem system = data.orbits();
        AtlasOrbitSystem.Positions positions = system.newPositions();

        system.positionsAt(100.0, positions);

        int ringNode = system.body("b2").index();
        assertEquals(0f, positions.x(ringNode));
        assertEquals(200f, positions.y(ringNode));
        assertEquals(Set.of(), system.neighboursAcrossTethers("b2"));
    }

    private static float distance(AtlasOrbitSystem.Positions positions, int first, int second) {
        return (float) Math.hypot(positions.x(first) - positions.x(second), positions.y(first) - positions.y(second));
    }
}
