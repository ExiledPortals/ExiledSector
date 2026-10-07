package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.layout.RingBelt;
import exiledsector.skills.layout.Rotation;
import exiledsector.skills.layout.Star;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeFleetMotionTest {

    private static final float TOLERANCE = 0.001f;
    private static final float FRAME_SECONDS = 1f / 60f;

    private static final CoreVolume CORE = new CoreVolume(0f, 0f, 375f, 4050f);

    @Test
    void theCoreVolumeSpansFromAroundTheCoreStarToTheOutermostCoreRingBelt() {
        List<Star> stars = List.of(new Star("faction_star", 0f, -13000f, 200f, "star_red", null, List.of("tritachyon")),
                new Star("central_star", 0f, 0f, 150f, "star_yellow", null, List.of("core")));
        List<RingBelt> ringBelts = List.of(
                new RingBelt("middle", 0f, 0f, new RingBelt.Shape(2850f, 3200f, "a.png", new Rotation(0f, 0.6f)), List.of("core")),
                new RingBelt("outer", 0f, 0f, new RingBelt.Shape(3600f, 4050f, "a.png", new Rotation(0f, -0.8f)), List.of("core")),
                new RingBelt("faction", 0f, -13000f, new RingBelt.Shape(100f, 900f, "a.png", new Rotation(0f, 0f)), List.of("tritachyon")));

        assertEquals(CORE, CoreVolume.of(stars, ringBelts));
    }

    @Test
    void withoutCoreRingBeltsTheVolumeFallsBackToADefaultRadius() {
        CoreVolume coreVolume = CoreVolume.of(List.of(), List.of());

        assertEquals(CoreVolume.DEFAULT_OUTER_RADIUS, coreVolume.outerRadius(), TOLERANCE);
        assertEquals(CoreVolume.DEFAULT_STAR_RADIUS * CoreVolume.STAR_CLEARANCE_MULT, coreVolume.innerRadius(), TOLERANCE);
    }

    @Test
    void randomPointsLandBetweenTheStarAndTheOuterEdge() {
        Random random = new Random(7L);
        float[] point = new float[2];
        for (int i = 0; i < 1000; i++) {
            CORE.randomPoint(random, point);
            double radius = Math.hypot(point[0], point[1]);
            assertTrue(radius >= CORE.innerRadius() - TOLERANCE && radius <= CORE.outerRadius() + TOLERANCE, "radius " + radius);
        }
    }

    @Test
    void aPathThroughTheStarIsRejectedAndOneAroundItIsNot() {
        assertFalse(CORE.segmentClearsStar(-2000f, 0f, 2000f, 0f));
        assertTrue(CORE.segmentClearsStar(-2000f, 1000f, 2000f, 1000f));
        assertTrue(CORE.segmentClearsStar(1000f, 1000f, 1000f, 1000f));
    }

    @Test
    void theFleetKeepsFlyingBetweenWaypointsInsideTheCoreVolume() {
        FleetFlight flight = new FleetFlight(CORE, 200f, new Random(3L));
        float firstWaypointX = flight.waypointX();
        boolean changedWaypoint = false;
        float highestEngineLevel = 0f;
        for (int frame = 0; frame < 60 * 600; frame++) {
            flight.advance(FRAME_SECONDS);
            double radius = Math.hypot(flight.positionX(), flight.positionY());
            assertTrue(radius < CORE.outerRadius() + 400f, "flew out to " + radius);
            changedWaypoint |= Float.compare(flight.waypointX(), firstWaypointX) != 0;
            highestEngineLevel = Math.max(highestEngineLevel, flight.engineLevel());
            assertTrue(flight.engineLevel() >= 0f && flight.engineLevel() <= 1f);
        }
        assertTrue(changedWaypoint);
        assertEquals(1f, highestEngineLevel, TOLERANCE);
    }

    @Test
    void shipsSpreadFurtherTheSmallerTheyAreAndALoneLargestShipSitsInTheMiddle() {
        assertEquals(0f, FleetShipDrift.maxOffset(40f, 5f, 5f, true), TOLERANCE);
        assertEquals(20f, FleetShipDrift.maxOffset(40f, 5f, 5f, false), TOLERANCE);
        assertEquals(36f, FleetShipDrift.maxOffset(40f, 5f, 1f, false), TOLERANCE);
    }

    @Test
    void shipsDriftWithinTheirOffsetAndTurnToTheFleetsFacing() {
        FleetShipDrift drift = new FleetShipDrift(FleetShipDrift.HullMotion.of(HullSize.FRIGATE), 30f, 0f, new Random(11L));
        for (int frame = 0; frame < 60 * 120; frame++) {
            drift.advance(FRAME_SECONDS, 90f);
            assertTrue(Math.hypot(drift.offsetX(), drift.offsetY()) <= 30f * 1.5f, "drifted to " + Math.hypot(drift.offsetX(), drift.offsetY()));
        }
        assertEquals(90f, drift.facingDeg(), TOLERANCE);
    }

    @Test
    void aCentredShipStaysAtTheCentre() {
        FleetShipDrift drift = new FleetShipDrift(FleetShipDrift.HullMotion.of(HullSize.CAPITAL_SHIP), 0f, 0f, new Random(5L));
        for (int frame = 0; frame < 600; frame++) {
            drift.advance(FRAME_SECONDS, 0f);
        }
        assertEquals(0f, drift.offsetX(), TOLERANCE);
        assertEquals(0f, drift.offsetY(), TOLERANCE);
    }

    @Test
    void turnsTakeTheShortWayRound() {
        assertEquals(20f, FleetShipDrift.shortestTurn(350f, 10f), TOLERANCE);
        assertEquals(-20f, FleetShipDrift.shortestTurn(10f, 350f), TOLERANCE);
        assertEquals(-90f, FleetShipDrift.shortestTurn(-180f, 90f), TOLERANCE);
    }

    @Test
    void bigFleetsShowTheFirstEightAndLastTwelveShipsAndNeverFighterWings() {
        List<FleetMemberAPI> members = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            FleetMemberAPI member = mock(FleetMemberAPI.class);
            when(member.isFighterWing()).thenReturn(i == 3);
            when(member.getId()).thenReturn("ship-" + i);
            when(member.getHullSpec()).thenReturn(mock(ShipHullSpecAPI.class));
            members.add(member);
        }

        List<String> shownIds = SkillTreeFleetRenderer.shownMembers(members, "ship-0").stream().map(FleetMemberAPI::getId).toList();

        assertEquals(20, shownIds.size());
        assertEquals(List.of("ship-0", "ship-1", "ship-2", "ship-4", "ship-5", "ship-6", "ship-7", "ship-8"), shownIds.subList(0, 8));
        assertEquals("ship-18", shownIds.get(8));
        assertEquals("ship-29", shownIds.get(19));
        assertFalse(shownIds.contains("ship-3"));
    }

    @Test
    void theShipWhoseTreeIsOpenIsAlwaysAmongTheShownShips() {
        List<FleetMemberAPI> members = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            FleetMemberAPI member = mock(FleetMemberAPI.class);
            when(member.getId()).thenReturn("ship-" + i);
            when(member.getHullSpec()).thenReturn(mock(ShipHullSpecAPI.class));
            members.add(member);
        }

        List<String> shownIds = SkillTreeFleetRenderer.shownMembers(members, "ship-12").stream().map(FleetMemberAPI::getId).toList();

        assertEquals(20, shownIds.size());
        assertTrue(shownIds.contains("ship-12"));
    }
}
