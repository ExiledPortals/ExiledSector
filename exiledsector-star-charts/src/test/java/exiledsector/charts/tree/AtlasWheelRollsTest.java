package exiledsector.charts.tree;

import exiledsector.charts.tree.AtlasTreeBodies.Body;
import exiledsector.charts.tree.AtlasTreeBodies.Link;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasWheelRollsTest {

    private static final int SEEDS = 8;
    private static final float SHIPPED_BODY_CLEARANCE = 12f;
    private static final float LANE_CLEARANCE = 20f;

    private static final String LONE_WHEEL = """
            { "nodes": [
                { "id": "gate", "type": "gate", "connectedTo": ["outer"], "x": 0, "y": 0 },
                { "id": "outer", "type": "salvage", "connectedTo": ["gate", "inner"], "x": 300, "y": -400 },
                { "id": "inner", "type": "salvage", "connectedTo": ["outer", "hub"], "x": -150, "y": -400 },
                { "id": "hub", "type": "hoard", "connectedTo": ["inner"], "x": 0, "y": -400 }%s
            ] }
            """;

    private static final String WALL = """
            ,
                { "id": "wall", "type": "salvage", "connectedTo": [], "x": 0, "y": -1400 }
            """;

    private static final String LONE_WHEEL_BODIES = """
            { "bodies": [
                { "id": "gate", "highway": true },
                { "id": "hub", "spiral": 1, "art": { "kind": "planet", "planet": "barren", "size": 40 } },
                { "id": "outer", "wheel": "hub", "art": { "kind": "planet", "planet": "barren", "size": 10 } },
                { "id": "inner", "wheel": "hub", "art": { "kind": "planet", "planet": "barren", "size": 10 } },
                { "id": "wall", "art": { "kind": "planet", "planet": "barren", "size": 900 } }
            ] }
            """;

    private static AtlasTreeBodies loneWheel(boolean walled) {
        try {
            return AtlasTreeData.parse(new JSONObject(AtlasTreeFixtures.TYPES), new JSONObject(LONE_WHEEL.formatted(walled ? WALL : "")),
                    new JSONObject(LONE_WHEEL_BODIES)).bodies();
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static AtlasTreeBodies shipped() throws Exception {
        return ShippedAtlasTree.load().bodies();
    }

    private static double distance(Body first, Body second) {
        return Math.hypot(first.x() - second.x(), first.y() - second.y());
    }

    @Test
    void rolledMinorsStayOnTheirOrbitsAndMoveRoundThem() {
        AtlasTreeBodies data = loneWheel(false);
        Body hub = data.body("hub");
        boolean anyMoved = false;
        for (int seed = 0; seed < SEEDS; seed++) {
            AtlasTreeBodies rolled = data.withRolledWheels(new Random(seed));
            for (String member : List.of("outer", "inner")) {
                assertEquals(distance(data.body(member), hub), distance(rolled.body(member), rolled.body("hub")), 0.5, member + " seed " + seed);
                anyMoved |= distance(data.body(member), rolled.body(member)) > 1;
            }
            assertEquals(data.body("gate").x(), rolled.body("gate").x());
            assertEquals(hub.x(), rolled.body("hub").x());
        }
        assertTrue(anyMoved);
    }

    @Test
    void everyRolledWheelLinkRunsFromBodyToBodyAndClosesInOnItsCentre() {
        AtlasTreeBodies rolled = loneWheel(false).withRolledWheels(new Random(3));
        Body hub = rolled.body("hub");
        for (Link link : rolled.links()) {
            Body first = rolled.bodies().get(link.first());
            Body second = rolled.bodies().get(link.second());
            AtlasLinkPath path = link.path();
            assertEquals(AtlasTreeBodies.Shape.SPIRAL, link.shape());
            assertEquals(first.x(), path.x(0), 0.5f);
            assertEquals(first.y(), path.y(0), 0.5f);
            assertEquals(second.x(), path.x(path.size() - 1), 0.5f);
            assertEquals(second.y(), path.y(path.size() - 1), 0.5f);
            boolean inward = distance(first, hub) >= distance(second, hub);
            double previous = Double.NaN;
            for (int point = 0; point < path.size(); point++) {
                double reach = Math.hypot(path.x(point) - hub.x(), path.y(point) - hub.y());
                if (!Double.isNaN(previous)) {
                    assertTrue(inward ? reach <= previous + 0.5 : reach >= previous - 0.5, first.nodeId() + "-" + second.nodeId() + " point " + point);
                }
                previous = reach;
            }
            assertTrue(path.size() <= AtlasLinkPath.MAX_WHEEL_SPIRAL_POINTS);
        }
    }

    @Test
    void aWheelThatCannotWindClearOfItsNeighboursKeepsItsShippedLayout() {
        AtlasTreeBodies data = loneWheel(true);

        assertSame(data, data.withRolledWheels(new Random(1)));
    }

    @Test
    void theSameRollGivesTheSameAtlas() throws Exception {
        AtlasTreeBodies data = shipped();
        AtlasTreeBodies first = data.withRolledWheels(new Random(42));
        AtlasTreeBodies second = data.withRolledWheels(new Random(42));
        AtlasTreeBodies other = data.withRolledWheels(new Random(43));

        for (Body body : first.bodies()) {
            assertEquals(body.x(), second.bodies().get(body.index()).x());
            assertEquals(body.y(), second.bodies().get(body.index()).y());
        }
        assertNotEquals(positions(first), positions(other));
    }

    private static List<Float> positions(AtlasTreeBodies bodies) {
        List<Float> positions = new ArrayList<>();
        for (Body body : bodies.bodies()) {
            positions.add(body.x());
            positions.add(body.y());
        }
        return positions;
    }

    @Test
    void shippedWheelsWindFromCalmHooksToNearlyTwoTurnsAndMostRollClear() throws Exception {
        AtlasTreeBodies data = shipped();
        long wheels = data.bodies().stream().filter(body -> body.wheel() != AtlasTreeBodies.NO_WHEEL).map(Body::wheel).distinct().count();
        double calmest = Double.MAX_VALUE;
        double wildest = 0;
        for (int seed = 0; seed < SEEDS; seed++) {
            Map<Integer, AtlasWheelRolls.Placement> placements = AtlasWheelRolls.roll(data, new Random(seed));
            assertTrue(placements.size() >= wheels * 0.9, "only " + placements.size() + " of " + wheels + " wheels rolled clear with seed " + seed);
            for (AtlasWheelRolls.Placement placement : placements.values()) {
                calmest = Math.min(calmest, placement.totalSweep());
                wildest = Math.max(wildest, placement.totalSweep());
            }
        }
        assertTrue(calmest < Math.toRadians(300), "calmest " + Math.toDegrees(calmest));
        assertTrue(wildest > Math.toRadians(600), "wildest " + Math.toDegrees(wildest));
    }

    @Test
    void rolledSpiralsBendSmoothlyEvenOnLongSpokes() throws Exception {
        AtlasTreeBodies data = shipped();
        for (int seed = 0; seed < SEEDS; seed++) {
            AtlasTreeBodies bodies = data.withRolledWheels(new Random(seed));
            for (int index = 0; index < bodies.links().size(); index++) {
                Link link = bodies.links().get(index);
                if (link.path() == data.links().get(index).path()) {
                    continue;
                }
                AtlasLinkPath path = link.path();
                for (int point = 0; point + 1 < path.size(); point++) {
                    double step = Math.hypot(path.x(point + 1) - path.x(point), path.y(point + 1) - path.y(point));
                    assertTrue(step < 60, "seed " + seed + " step " + step + " on " + bodies.bodies().get(link.first()).nodeId());
                }
            }
        }
    }

    @Test
    void rolledLanesAlwaysBendTowardTheirCentre() throws Exception {
        AtlasTreeBodies data = shipped();
        for (int seed = 0; seed < SEEDS; seed++) {
            AtlasTreeBodies bodies = data.withRolledWheels(new Random(seed));
            for (int index = 0; index < bodies.links().size(); index++) {
                Link link = bodies.links().get(index);
                if (link.path() == data.links().get(index).path()) {
                    continue;
                }
                Body first = bodies.bodies().get(link.first());
                Body member = first.wheel() != AtlasTreeBodies.NO_WHEEL ? first : bodies.bodies().get(link.second());
                Body centre = bodies.bodies().get(member.wheel());
                boolean inward = distance(first, centre) >= distance(bodies.bodies().get(link.second()), centre);
                AtlasLinkPath path = link.path();
                for (int point = 1; point + 1 < path.size(); point++) {
                    double before = Math.atan2(path.y(point) - path.y(point - 1), path.x(point) - path.x(point - 1));
                    double after = Math.atan2(path.y(point + 1) - path.y(point), path.x(point + 1) - path.x(point));
                    double bend = Math.IEEEremainder(after - before, Math.PI * 2) * centre.spiral() * (inward ? 1 : -1);
                    assertTrue(bend > -1e-3, "seed " + seed + " " + first.nodeId() + " bends outward at point " + point);
                }
            }
        }
    }

    @Test
    void rolledLanesKeepClearOfLanesTheyShareNoNodeWith() throws Exception {
        AtlasTreeBodies data = shipped();
        List<String> overlaps = new ArrayList<>();
        for (int seed = 0; seed < SEEDS; seed++) {
            AtlasTreeBodies bodies = data.withRolledWheels(new Random(seed));
            for (int index = 0; index < bodies.links().size(); index++) {
                Link link = bodies.links().get(index);
                if (link.path() == data.links().get(index).path()) {
                    continue;
                }
                for (Link other : bodies.links()) {
                    boolean sharesNode = other.first() == link.first() || other.first() == link.second()
                            || other.second() == link.first() || other.second() == link.second();
                    if (!sharesNode && pathsWithin(link.path(), other.path(), LANE_CLEARANCE)) {
                        overlaps.add("seed " + seed + ": " + bodies.bodies().get(link.first()).nodeId() + "-" + bodies.bodies().get(link.second()).nodeId()
                                + " runs over " + bodies.bodies().get(other.first()).nodeId() + "-" + bodies.bodies().get(other.second()).nodeId());
                    }
                }
            }
        }

        assertTrue(overlaps.isEmpty(), String.join("\n", overlaps));
    }

    private static boolean pathsWithin(AtlasLinkPath first, AtlasLinkPath second, float clearance) {
        if (first.minX() - clearance > second.maxX() || second.minX() - clearance > first.maxX()
                || first.minY() - clearance > second.maxY() || second.minY() - clearance > first.maxY()) {
            return false;
        }
        for (int point = 0; point < first.size(); point++) {
            for (int segment = 0; segment + 1 < second.size(); segment++) {
                if (AtlasWheelRolls.toSegment(first.x(point), first.y(point), second.x(segment), second.y(segment), second.x(segment + 1), second.y(segment + 1)) < clearance) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void noRolledLinkPassesOverAnotherBody() throws Exception {
        AtlasTreeBodies data = shipped();
        List<String> crossings = new ArrayList<>();
        for (int seed = 0; seed < SEEDS; seed++) {
            AtlasTreeBodies bodies = data.withRolledWheels(new Random(seed));
            for (Link link : bodies.links()) {
                if (link.alongRing()) {
                    continue;
                }
                Body first = bodies.bodies().get(link.first());
                Body second = bodies.bodies().get(link.second());
                for (Body body : bodies.bodies()) {
                    if (body != first && body != second && body != bodies.star()
                            && distanceToPath(body, link.path()) < body.art().size() + SHIPPED_BODY_CLEARANCE) {
                        crossings.add("seed " + seed + ": " + first.nodeId() + "-" + second.nodeId() + " crosses " + body.nodeId());
                    }
                }
            }
        }

        assertTrue(crossings.isEmpty(), String.join("\n", crossings));
    }

    private static double distanceToPath(Body body, AtlasLinkPath path) {
        double nearest = Double.MAX_VALUE;
        for (int segment = 0; segment + 1 < path.size(); segment++) {
            nearest = Math.min(nearest, AtlasWheelRolls.toSegment(body.x(), body.y(), path.x(segment), path.y(segment), path.x(segment + 1), path.y(segment + 1)));
        }
        return nearest;
    }
}
