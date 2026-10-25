package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class LaneFleetTrafficTest {

    private static final class Relations implements LaneFleetTraffic.Factions {
        private final Set<String> crewed;

        private Relations(Set<String> crewed) {
            this.crewed = crewed;
        }

        @Override
        public List<ShipHullSpecAPI> hulls(String factionId) {
            return crewed.contains(factionId) ? List.of(mock(ShipHullSpecAPI.class)) : List.of();
        }

        @Override
        public Color colour(String factionId) {
            return Color.WHITE;
        }

        @Override
        public boolean friendly(String factionId, String otherFactionId) {
            return Set.of("hegemony", "persean").containsAll(Set.of(factionId, otherFactionId));
        }

        @Override
        public boolean hostile(String factionId, String otherFactionId) {
            return factionId.equals("pirates") != otherFactionId.equals("pirates");
        }
    }

    private static LaneFleetTraffic.Port port(long id, String factionId, Long... neighbours) {
        return new LaneFleetTraffic.Port(id, id * 1000f, 0f, 100f, 200f, factionId, List.of(neighbours));
    }

    @Test
    void aFleetOnlyVisitsNeighboursOfItsOwnOrAFriendlyFaction() {
        LaneFleetTraffic.Port here = port(1, "hegemony", 2L, 3L, 4L, 9L);
        Map<Long, LaneFleetTraffic.Port> ports = Map.of(1L, here, 2L, port(2, "hegemony"), 3L, port(3, "persean"), 4L, port(4, "pirates"));
        Random random = new Random(3L);
        Set<Long> visited = new HashSet<>();
        for (int trip = 0; trip < 200; trip++) {
            visited.add(LaneFleetTraffic.visitTarget(here, "hegemony", ports, new Relations(Set.of()), random).id());
        }

        assertEquals(Set.of(2L, 3L), visited);
        assertEquals(4L, LaneFleetTraffic.visitTarget(here, "pirates", ports, new Relations(Set.of()), random).id());
        assertNull(LaneFleetTraffic.visitTarget(here, "luddic_path", ports, new Relations(Set.of()), random));
    }

    @Test
    void fleetCountFollowsTheCrewedPortsUpToTheCap() {
        Relations pirates = new Relations(Set.of("pirates"));
        List<LaneFleetTraffic.Port> many = new ArrayList<>();
        for (long id = 0; id < 100; id++) {
            many.add(port(id, id % 2 == 0 ? "pirates" : "derelict"));
        }

        assertEquals(2, LaneFleetTraffic.targetFleets(List.of(port(1, "pirates"), port(2, "pirates"), port(3, "pirates"), port(4, "pirates"),
                port(5, "derelict"), port(6, null)), pirates));
        assertEquals(LaneFleetTraffic.MAX_FLEETS, LaneFleetTraffic.targetFleets(many, pirates));
    }

    @Test
    void portsWithoutCrewedFactionsSpawnNothing() {
        LaneFleetTraffic traffic = new LaneFleetTraffic(new Relations(Set.of()), new Random(1L));
        traffic.setPorts(List.of(port(1, "derelict", 2L), port(2, "derelict", 1L), port(3, null)));
        traffic.advance(1f);

        assertEquals(0, traffic.fleetCount());
    }
}
