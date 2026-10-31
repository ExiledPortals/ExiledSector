package exiledsector.charts.anomaly;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShapeAnomaliesTest {

    private final List<String> terrainAdded = new ArrayList<>();
    private StarSystemAPI system;
    private PlanetAPI star;

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        when(star.isStar()).thenReturn(true);
        when(star.getRadius()).thenReturn(500f);
        List<PlanetAPI> planets = new ArrayList<>(List.of(star));
        for (int world = 0; world < 4; world++) {
            PlanetAPI planet = mock(PlanetAPI.class);
            when(planet.getRadius()).thenReturn(120f);
            planets.add(planet);
        }
        when(system.getPlanets()).thenReturn(planets);
        when(system.addTerrain(anyString(), any())).thenAnswer(invocation -> {
            terrainAdded.add(invocation.getArgument(0));
            return mock(SectorEntityToken.class);
        });
    }

    private AnomalyLayoutSite site() {
        return new AnomalyLayoutSite(system, star, List.of(star), 9000f, new Random(4));
    }

    @Test
    void anAsteroidStormAddsBeltsAndFields() {
        AsteroidStorm.INSTANCE.shape(site());

        verify(system, times(AsteroidStorm.BELTS)).addAsteroidBelt(eq(star), anyInt(), anyFloat(), eq(AsteroidStorm.BELT_WIDTH), anyFloat(), anyFloat(),
                eq(Terrain.ASTEROID_BELT), isNull());
        assertEquals(AsteroidStorm.FIELDS, terrainAdded.stream().filter(Terrain.ASTEROID_FIELD::equals).count());
    }

    @Test
    void anAsteroidStormTagsItsBeltsSoItsCachesCanFindThem() {
        SectorEntityToken belt = mock(SectorEntityToken.class);
        when(system.addAsteroidBelt(eq(star), anyInt(), anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyString(), isNull())).thenReturn(belt);

        AsteroidStorm.INSTANCE.shape(site());

        verify(belt, times(AsteroidStorm.BELTS)).addTag(AsteroidStorm.BELT_TAG);
    }

    @Test
    void aMagneticStormWrapsTheStarAndUpToThreeWorlds() {
        MagneticStorm.INSTANCE.shape(site());

        assertEquals(1 + MagneticStorm.MAX_WORLDS, terrainAdded.size());
        assertTrue(terrainAdded.stream().allMatch(Terrain.MAGNETIC_FIELD::equals));
    }

    @Test
    void stellarFlaresRefuseASystemWithNoVisibleStar() {
        assertThrows(IllegalStateException.class,
                () -> StellarFlares.INSTANCE.shape(new AnomalyLayoutSite(system, star, List.of(), 9000f, new Random(4))));
    }

    @Test
    void starAnomaliesForceTheirStarOrSystemTypeAndShapeAnomaliesDoNot() {
        assertEquals("black_hole", StarAnomalies.BLACK_HOLE.primaryStar());
        assertEquals("star_neutron", StarAnomalies.NEUTRON_STAR.primaryStar());
        assertEquals(com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType.NEBULA, StarAnomalies.DENSE_NEBULA.systemType());
        assertNull(ChartAnomalies.role(StellarFlares.ANOMALY_ID, StarRule.class));
        assertNull(ChartAnomalies.role(AsteroidStorm.ANOMALY_ID, StarRule.class));
        assertSame(StarAnomalies.BLACK_HOLE, ChartAnomalies.role(StarAnomalies.BLACK_HOLE.id(), StarRule.class));
    }
}
