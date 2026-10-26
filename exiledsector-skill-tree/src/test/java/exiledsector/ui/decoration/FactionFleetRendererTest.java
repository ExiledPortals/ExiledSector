package exiledsector.ui.decoration;

import exiledsector.skills.SkillNode;
import exiledsector.skills.decoration.Star;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionFleetRendererTest {

    private static final float TOLERANCE = 0.001f;

    @Test
    void aFactionVolumeCircleTheRegionStarOutToItsNodesIgnoringStrayNodes() {
        List<Star> stars = List.of(new Star("pirate_star", 1000f, 0f, 200f, "star_red", null, List.of("pirate")));
        List<SkillNode> nodes = List.of(node("a", 1500f, 0f, "pirate"), node("b", 1000f, 800f, "pirate"), node("c", 1000f, -1000f, "pirate"),
                node("stray", 9000f, 0f, "pirate"), node("core", 0f, 0f, "core"));

        RoamVolume volume = TreeRoamVolumes.region("pirate", stars, nodes);

        assertEquals(new RoamVolume(1000f, 0f, 200f * TreeRoamVolumes.STAR_CLEARANCE_MULT, 1000f * TreeRoamVolumes.REGION_EDGE_MULT), volume);
    }

    @Test
    void aStarlessFactionVolumeCentresOnItsNodes() {
        RoamVolume volume = TreeRoamVolumes.region("luddic", List.of(), List.of(node("a", 100f, 0f, "luddic"), node("b", -100f, 0f, "luddic")));

        assertEquals(0f, volume.centerX(), TOLERANCE);
        assertEquals(0f, volume.innerRadius(), TOLERANCE);
        assertEquals(100f * TreeRoamVolumes.REGION_EDGE_MULT, volume.outerRadius(), TOLERANCE);
    }

    @Test
    void aRegionWithoutNodesHasNoVolume() {
        assertNull(TreeRoamVolumes.region("hegemony", List.of(), List.of(node("a", 0f, 0f, "core"))));
    }

    @Test
    void withoutAlliesAFleetNeverLeavesHome() {
        RoamVolume home = new RoamVolume(0f, 0f, 0f, 1000f);
        Random random = new Random(5L);
        for (int arrival = 0; arrival < 500; arrival++) {
            assertEquals(home, FactionFleetRenderer.nextVolume(home, home, List.of(), random));
        }
    }

    @Test
    void aFleetSometimesVisitsAnAllyAndLaterHeadsBackOrOnward() {
        RoamVolume home = new RoamVolume(0f, 0f, 0f, 1000f);
        RoamVolume ally = new RoamVolume(10000f, 0f, 0f, 1000f);
        RoamVolume otherAlly = new RoamVolume(0f, 10000f, 0f, 1000f);
        Random random = new Random(9L);
        int departures = 0;
        for (int arrival = 0; arrival < 1000; arrival++) {
            RoamVolume next = FactionFleetRenderer.nextVolume(home, home, List.of(ally, otherAlly), random);
            if (next != home) {
                departures++;
                assertTrue(next == ally || next == otherAlly);
            }
            RoamVolume fromAlly = FactionFleetRenderer.nextVolume(ally, home, List.of(ally, otherAlly), random);
            assertTrue(fromAlly == ally || fromAlly == home || fromAlly == otherAlly);
        }
        assertTrue(departures > 50 && departures < 300, "departures " + departures);
    }

    @Test
    void aFlightSentToAnotherVolumeArrivesThereAndRoamsIt() {
        RoamVolume home = new RoamVolume(0f, 0f, 500f, 1500f);
        RoamVolume ally = new RoamVolume(13000f, 0f, 500f, 1500f);
        boolean[] sent = {false};
        FleetFlight flight = new FleetFlight(home, 150f, new Random(4L), current -> {
            if (!sent[0]) {
                sent[0] = true;
                return ally;
            }
            return current;
        });
        for (int frame = 0; frame < 60 * 600; frame++) {
            flight.advance(1f / 60f);
        }
        assertEquals(ally, flight.roamVolume());
        assertTrue(Math.hypot(flight.positionX() - ally.centerX(), flight.positionY() - ally.centerY()) < ally.outerRadius() + 400f);
    }

    private static SkillNode node(String id, float x, float y, String region) {
        return new SkillNode(id, null, List.of(), x, y, SkillNodeDecoration.NONE, List.of(region));
    }
}
