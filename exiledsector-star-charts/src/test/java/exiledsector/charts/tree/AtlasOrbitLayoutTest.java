package exiledsector.charts.tree;

import exiledsector.charts.tree.AtlasOrbitSystem.Link;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasOrbitLayoutTest {

    private final AtlasOrbitSystem system = AtlasOrbitSystemTest.data().orbits();
    private final AtlasOrbitLayout layout = AtlasOrbitLayout.of(system);

    private int indexOf(String nodeId) {
        return system.body(nodeId).index();
    }

    private String idOf(int index) {
        return system.bodies().get(index).nodeId();
    }

    @Test
    void onlyBodiesCirclingTheStarWithoutSatellitesAreHighwayNodes() {
        for (String highway : List.of("a1", "a2", "a3", "b1", "b2", "b3")) {
            assertTrue(layout.isHighway(indexOf(highway)), highway);
        }
        for (String other : List.of("root", "planet", "out", "in")) {
            assertFalse(layout.isHighway(indexOf(other)), other);
        }
    }

    @Test
    void aWheelRunsFromItsHighwayNodeThroughTheOuterThenInnerSmallAndTheStarIsNeverOne() {
        List<AtlasOrbitLayout.Wheel> wheels = layout.wheels();

        assertEquals(1, wheels.size());
        AtlasOrbitLayout.Wheel wheel = wheels.get(0);
        assertEquals("planet", idOf(wheel.centre()));
        assertEquals("out", idOf(wheel.outer()));
        assertEquals("in", idOf(wheel.inner()));
        assertEquals("a3", idOf(wheel.highway()));
    }

    @Test
    void raysAreTheLinksTouchingTheStar() {
        Set<String> rays = layout.rays().stream().map(this::describe).collect(Collectors.toSet());

        assertEquals(Set.of("a1-root"), rays);
    }

    @Test
    void everyHighwayTrackBecomesABeltAndTheOutermostIsMarked() {
        List<AtlasOrbitLayout.Belt> belts = layout.belts();

        assertEquals(2, belts.size());
        AtlasOrbitLayout.Belt outermost = belts.stream().filter(AtlasOrbitLayout.Belt::outermost).findFirst().orElseThrow();
        assertEquals(1, belts.stream().filter(AtlasOrbitLayout.Belt::outermost).count());
        assertTrue(outermost.track().memberIndexes().contains(indexOf("b1")));
    }

    private String describe(Link link) {
        String first = idOf(link.first());
        String second = idOf(link.second());
        return first.compareTo(second) < 0 ? first + "-" + second : second + "-" + first;
    }
}
