package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import exiledsector.skills.SkillNode;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.layout.Star;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FactionFleetRendererTest {

    private static final float TOLERANCE = 0.001f;

    @Test
    void aFactionVolumeCircleTheRegionStarOutToItsNodesIgnoringStrayNodes() {
        List<Star> stars = List.of(new Star("pirate_star", 1000f, 0f, 200f, "star_red", null, List.of("pirate")));
        List<SkillNode> nodes = List.of(node("a", 1500f, 0f, "pirate"), node("b", 1000f, 800f, "pirate"), node("c", 1000f, -1000f, "pirate"),
                node("stray", 9000f, 0f, "pirate"), node("core", 0f, 0f, "core"));

        RoamVolume volume = RoamVolume.region("pirate", stars, nodes);

        assertEquals(new RoamVolume(1000f, 0f, 200f * RoamVolume.STAR_CLEARANCE_MULT, 1000f * RoamVolume.REGION_EDGE_MULT), volume);
    }

    @Test
    void aStarlessFactionVolumeCentresOnItsNodes() {
        RoamVolume volume = RoamVolume.region("luddic", List.of(), List.of(node("a", 100f, 0f, "luddic"), node("b", -100f, 0f, "luddic")));

        assertEquals(0f, volume.centerX(), TOLERANCE);
        assertEquals(0f, volume.innerRadius(), TOLERANCE);
        assertEquals(100f * RoamVolume.REGION_EDGE_MULT, volume.outerRadius(), TOLERANCE);
    }

    @Test
    void aRegionWithoutNodesHasNoVolume() {
        assertNull(RoamVolume.region("hegemony", List.of(), List.of(node("a", 0f, 0f, "core"))));
    }

    @Test
    void fleetsAreLedByAWarshipAndEscortedByFrigatesAndDestroyers() {
        List<ShipHullSpecAPI> hulls = List.of(hull("wolf", HullSize.FRIGATE), hull("hammerhead", HullSize.DESTROYER),
                hull("eagle", HullSize.CRUISER), hull("onslaught", HullSize.CAPITAL_SHIP));
        Random random = new Random(11L);
        for (int fleet = 0; fleet < 200; fleet++) {
            List<ShipHullSpecAPI> picked = FactionFleetRenderer.pickHulls(hulls, random);

            assertTrue(picked.size() >= FactionFleetRenderer.MIN_SHIPS_PER_FLEET && picked.size() <= FactionFleetRenderer.MAX_SHIPS_PER_FLEET);
            assertNotEquals(HullSize.FRIGATE, picked.get(0).getHullSize());
            for (ShipHullSpecAPI escort : picked.subList(1, picked.size())) {
                assertTrue(escort.getHullSize() == HullSize.FRIGATE || escort.getHullSize() == HullSize.DESTROYER);
            }
        }
    }

    @Test
    void aFrigateOnlyFactionStillFormsFleets() {
        List<ShipHullSpecAPI> picked = FactionFleetRenderer.pickHulls(List.of(hull("wolf", HullSize.FRIGATE)), new Random(1L));

        assertTrue(picked.stream().allMatch(hull -> hull.getHullSize() == HullSize.FRIGATE));
    }

    @Test
    void onlyWarshipHullsRoam() {
        assertTrue(FactionFleetRenderer.roams(hull("wolf", HullSize.FRIGATE)));
        assertFalse(FactionFleetRenderer.roams(hull("talon", HullSize.FIGHTER)));
        ShipHullSpecAPI station = hull("station", HullSize.CAPITAL_SHIP);
        when(station.getHints()).thenReturn(EnumSet.of(ShipTypeHints.STATION));
        assertFalse(FactionFleetRenderer.roams(station));
        ShipHullSpecAPI freighter = hull("buffalo", HullSize.DESTROYER);
        when(freighter.isCivilianNonCarrier()).thenReturn(true);
        assertFalse(FactionFleetRenderer.roams(freighter));
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

    private static ShipHullSpecAPI hull(String hullId, HullSize hullSize) {
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class);
        when(hull.getHullId()).thenReturn(hullId);
        when(hull.getHullSize()).thenReturn(hullSize);
        when(hull.getSpriteName()).thenReturn("graphics/ships/" + hullId + ".png");
        when(hull.getHints()).thenReturn(EnumSet.noneOf(ShipTypeHints.class));
        return hull;
    }
}
