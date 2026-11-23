package exiledsector.ui.decoration;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResearchStationOrbitTest {

    private static final float TOLERANCE = 0.01f;
    private static final SkillType STATION_TYPE = new SkillType.Builder("ai_subsystem_integration", "AI Subsystem Integration", "a.png", SkillTier.NOTABLE).build();
    private static final SkillNode STATION = new SkillNode("ai_subsystem_integration_1", STATION_TYPE, List.of(), 0f, 950f);

    @Test
    void theOrbitStartsAtTheStationsTreePositionAndCirclesTheCentralStar() {
        ResearchStationOrbit orbit = new ResearchStationOrbit(STATION, 0f);

        assertEquals(0f, orbit.x(), TOLERANCE);
        assertEquals(950f, orbit.y(), TOLERANCE);

        orbit.advance(ResearchStationOrbit.ORBIT_SECONDS / 2f);

        assertEquals(0f, orbit.x(), TOLERANCE);
        assertEquals(-950f, orbit.y(), TOLERANCE);
    }

    @Test
    void aFullOrbitTakesThreeMinutes() {
        ResearchStationOrbit orbit = new ResearchStationOrbit(STATION, 0f);
        orbit.advance(ResearchStationOrbit.ORBIT_SECONDS);

        assertEquals(180f, ResearchStationOrbit.ORBIT_SECONDS);
        assertEquals(0f, orbit.x(), 0.05f);
        assertEquals(950f, orbit.y(), 0.05f);
    }

    @Test
    void anOrbitStartedPartWayAroundIsAlreadyThere() {
        ResearchStationOrbit late = new ResearchStationOrbit(STATION, ResearchStationOrbit.ORBIT_SECONDS / 4f);
        ResearchStationOrbit early = new ResearchStationOrbit(STATION, 0f);
        early.advance(ResearchStationOrbit.ORBIT_SECONDS / 4f);

        assertEquals(early.x(), late.x(), TOLERANCE);
        assertEquals(early.y(), late.y(), TOLERANCE);
    }

    @Test
    void withoutAStationTheOrbitStaysAtTheCentre() {
        ResearchStationOrbit orbit = new ResearchStationOrbit(null, 10f);

        assertNull(orbit.stationNode());
        assertEquals(0f, orbit.x());
        assertEquals(0f, orbit.y());
    }
}
