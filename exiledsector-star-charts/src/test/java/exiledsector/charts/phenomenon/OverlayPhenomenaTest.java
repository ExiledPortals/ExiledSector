package exiledsector.charts.phenomenon;

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

class OverlayPhenomenaTest {

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

    private PhenomenonSite site() {
        return new PhenomenonSite(system, star, List.of(star), 9000f, new Random(4));
    }

    @Test
    void anAsteroidStormAddsBeltsAndFields() {
        AsteroidStorm.INSTANCE.shape(site());

        verify(system, times(AsteroidStorm.BELTS)).addAsteroidBelt(eq(star), anyInt(), anyFloat(), eq(AsteroidStorm.BELT_WIDTH), anyFloat(), anyFloat(),
                eq(Terrain.ASTEROID_BELT), isNull());
        assertEquals(AsteroidStorm.FIELDS, terrainAdded.stream().filter(Terrain.ASTEROID_FIELD::equals).count());
    }

    @Test
    void aMagneticStormWrapsTheStarAndUpToThreeWorlds() {
        MagneticStorm.INSTANCE.shape(site());

        assertEquals(1 + MagneticStorm.MAX_WORLDS, terrainAdded.size());
        assertTrue(terrainAdded.stream().allMatch(Terrain.MAGNETIC_FIELD::equals));
    }

    @Test
    void starPhenomenaForceTheirStarOrSystemTypeAndOverlaysDoNot() {
        assertEquals("black_hole", StarPhenomena.BLACK_HOLE.primaryStar());
        assertEquals("star_neutron", StarPhenomena.NEUTRON_STAR.primaryStar());
        assertEquals(com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType.NEBULA, StarPhenomena.DENSE_NEBULA.systemType());
        assertEquals(ChartPhenomenon.Kind.OVERLAY, StellarFlares.INSTANCE.kind());
        assertEquals(null, AsteroidStorm.INSTANCE.primaryStar());
    }
}
