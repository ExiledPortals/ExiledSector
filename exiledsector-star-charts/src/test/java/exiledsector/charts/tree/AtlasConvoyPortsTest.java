package exiledsector.charts.tree;

import exiledsector.ProjectFiles;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasConvoyPortsTest {

    private static final float MAX_PORT_SHARE = 0.2f;

    private static Map<String, AtlasBody> bodies() throws Exception {
        return AtlasBodyReader.parse(ProjectFiles.readJson(ProjectFiles.projectRoot().resolve(AtlasBodyReader.BODIES_PATH)));
    }

    private static List<AtlasBody> ports(Map<String, AtlasBody> bodies) {
        return bodies.values().stream().filter(body -> !body.art().convoys().isEmpty()).toList();
    }

    @Test
    void everySupplyConvoyNodeIsAPortWithConvoys() throws Exception {
        Map<String, AtlasBody> bodies = bodies();

        List<AtlasBody> convoyNodes = bodies.values().stream().filter(body -> body.nodeId().startsWith("supply_convoy_chance_")).toList();

        assertFalse(convoyNodes.isEmpty());
        assertTrue(convoyNodes.stream().allMatch(body -> !body.art().convoys().isEmpty()));
        assertTrue(bodies.values().stream().noneMatch(body -> body.art().kind() == AtlasBodyArt.Kind.FLEET));
    }

    @Test
    void everyPortIsAtMostAFifthTheSizeOfTheBodyItsConvoysRunTo() throws Exception {
        Map<String, AtlasBody> bodies = bodies();

        for (AtlasBody port : ports(bodies)) {
            AtlasBody destination = bodies.get(port.wheel());
            assertNotNull(destination, port.nodeId());
            assertTrue(port.art().size() <= destination.art().size() * MAX_PORT_SHARE, port.nodeId());
        }
    }

    @Test
    void everyConvoyWheelWithAPlanetCentreIsPopulatedUnlessItIsAHybrid() throws Exception {
        Map<String, AtlasBody> bodies = bodies();

        for (AtlasBody port : ports(bodies)) {
            AtlasBody destination = bodies.get(port.wheel());
            boolean hybrid = !destination.colour().equalsIgnoreCase(port.colour());
            if (destination.art().kind() == AtlasBodyArt.Kind.PLANET && !hybrid) {
                assertFalse(destination.art().cityLights().isEmpty(), destination.nodeId());
            }
        }
    }
}
