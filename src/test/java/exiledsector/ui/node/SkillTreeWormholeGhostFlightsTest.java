package exiledsector.ui.node;

import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeTopology;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeWormholeGhostFlightsTest {

    private MockedStatic<SkillTree> skillTreeMock;
    private ShipSkillData data;

    private static SkillNode wormhole(String id, String pairedId, float x) {
        SkillType type = new SkillType.Builder("wormhole", "Wormhole", "", SkillTier.WORMHOLE).build();
        return new SkillNode(id, type, List.of(), x, 0f, new SkillNodeDecoration(null, null, null, null, pairedId));
    }

    @BeforeEach
    void setUp() {
        SkillNode a = wormhole("wormhole_a", "wormhole_b", 0f);
        SkillNode b = wormhole("wormhole_b", "wormhole_a", 5000f);
        Map<String, SkillNode> nodes = new LinkedHashMap<>();
        nodes.put(a.getId(), a);
        nodes.put(b.getId(), b);
        SkillTreeTopology topology = SkillTreeTopology.of(nodes.values());
        skillTreeMock = Mockito.mockStatic(SkillTree.class);
        skillTreeMock.when(SkillTree::topology).thenReturn(topology);
        skillTreeMock.when(() -> SkillTree.get("wormhole_a")).thenReturn(a);
        skillTreeMock.when(() -> SkillTree.get("wormhole_b")).thenReturn(b);
        data = mock(ShipSkillData.class);
    }

    @AfterEach
    void tearDown() {
        skillTreeMock.close();
    }

    @Test
    void launchesOneFlightPerPairAtMostEveryFewSecondsOnceBothEndsAreAllocated() {
        when(data.isAllocated("wormhole_a")).thenReturn(true);
        when(data.isAllocated("wormhole_b")).thenReturn(true);
        SkillTreeWormholeGhostFlights flights = new SkillTreeWormholeGhostFlights(null, new Random(1));

        flights.advance(SkillTreeWormholeGhostFlights.MAX_SECONDS_BETWEEN_FLIGHTS, data);
        assertEquals(1, flights.activeFlightCount());

        flights.advance(1f, data);
        assertEquals(1, flights.activeFlightCount());
    }

    @Test
    void launchFromStartsAFlightImmediatelyRegardlessOfTheSchedule() {
        SkillTreeWormholeGhostFlights flights = new SkillTreeWormholeGhostFlights(null, new Random(1));

        flights.launchFrom(SkillTree.get("wormhole_b"), SkillTree.get("wormhole_a"));

        assertEquals(1, flights.activeFlightCount());
    }

    @Test
    void neverLaunchesWhileThePairIsUnallocated() {
        SkillTreeWormholeGhostFlights flights = new SkillTreeWormholeGhostFlights(null, new Random(1));

        for (int i = 0; i < 10; i++) {
            flights.advance(SkillTreeWormholeGhostFlights.MAX_SECONDS_BETWEEN_FLIGHTS, data);
        }

        assertEquals(0, flights.activeFlightCount());
    }

    @Test
    void flightsExpireAfterCrossingToTheOtherNode() {
        when(data.isAllocated("wormhole_a")).thenReturn(true);
        when(data.isAllocated("wormhole_b")).thenReturn(true);
        SkillTreeWormholeGhostFlights flights = new SkillTreeWormholeGhostFlights(null, new Random(1));
        flights.advance(SkillTreeWormholeGhostFlights.MAX_SECONDS_BETWEEN_FLIGHTS, data);

        when(data.isAllocated("wormhole_a")).thenReturn(false);
        flights.advance(SkillTreeWormholeGhostFlights.MIN_SECONDS_BETWEEN_FLIGHTS, data);

        assertEquals(0, flights.activeFlightCount());
    }
}
