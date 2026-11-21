package exiledsector.charts.phasing;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PhaseDestinationTest {

    private static final float STAR_RADIUS = 500f;
    private static final float PLANET_RADIUS = 150f;

    private StarSystemAPI system;
    private PlanetAPI star;
    private final List<PlanetAPI> planets = new ArrayList<>();
    private final List<CampaignTerrainAPI> terrain = new ArrayList<>();

    @BeforeEach
    void setUp() {
        system = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        when(star.getRadius()).thenReturn(STAR_RADIUS);
        when(star.getContainingLocation()).thenReturn(system);
        when(system.getStar()).thenReturn(star);
        when(system.getPlanets()).thenAnswer(invocation -> new ArrayList<>(planets));
        when(system.getTerrainCopy()).thenAnswer(invocation -> new ArrayList<>(terrain));
        CampaignTerrainAPI corona = mock(CampaignTerrainAPI.class);
        StarCoronaTerrainPlugin coronaPlugin = mock(StarCoronaTerrainPlugin.class);
        ((BaseRingTerrain) coronaPlugin).params = new RingParams(300f, 650f, star);
        when(corona.getPlugin()).thenReturn(coronaPlugin);
        when(corona.getOrbitFocus()).thenReturn(star);
        terrain.add(corona);
    }

    private PlanetAPI orbiting(SectorEntityToken focus, float orbitRadius, float radius) {
        PlanetAPI planet = mock(PlanetAPI.class);
        when(planet.getOrbitFocus()).thenReturn(focus);
        when(planet.getCircularOrbitRadius()).thenReturn(orbitRadius);
        when(planet.getRadius()).thenReturn(radius);
        when(planet.getContainingLocation()).thenReturn(system);
        planets.add(planet);
        return planet;
    }

    @Test
    void theStarIsTheFocusUnlessItOrbitsACentre() {
        assertSame(star, PhaseDestination.focusOf(system));

        SectorEntityToken centre = mock(SectorEntityToken.class);
        when(star.getOrbitFocus()).thenReturn(centre);
        when(system.getCenter()).thenReturn(centre);
        assertSame(centre, PhaseDestination.focusOf(system));
    }

    @Test
    void theCoronaIsMeasuredFromTheStarsSurface() {
        assertEquals(300f, PhaseDestination.coronaReach(system, star));
    }

    @Test
    void thePlanetTakesTheFreeOrbitClosestToTheGatePlanet() {
        orbiting(star, 2000f, 100f);
        orbiting(star, 6000f, 100f);
        PlanetAPI gatePlanet = orbiting(star, 8000f, 100f);

        float orbit = PhaseDestination.orbitRadiusNear(system, star, gatePlanet, PLANET_RADIUS);

        assertEquals(7900f - PLANET_RADIUS - PhaseDestination.CLEARANCE, orbit);
    }

    @Test
    void aFarOutlierDoesNotDragThePlanetOutward() {
        orbiting(star, 3200f, 100f);
        PlanetAPI gatePlanet = orbiting(star, 4000f, 100f);
        CustomCampaignEntityAPI outlier = stationary(mock(CustomCampaignEntityAPI.class), 150000f, 0f, 50f);
        when(system.getEntities(CustomCampaignEntityAPI.class)).thenReturn(new ArrayList<>(List.of(outlier)));

        float orbit = PhaseDestination.orbitRadiusNear(system, star, gatePlanet, PLANET_RADIUS);

        assertEquals(4100f + PLANET_RADIUS + PhaseDestination.CLEARANCE, orbit);
    }

    @Test
    void aGapTooNarrowForThePlanetIsSkipped() {
        orbiting(star, 2000f, 200f);
        orbiting(star, 2600f, 200f);
        PlanetAPI gatePlanet = orbiting(star, 3200f, 200f);

        float expected = 3400f + PLANET_RADIUS + PhaseDestination.FALLBACK_CLEARANCE;
        assertEquals(expected, PhaseDestination.orbitRadiusNear(system, star, gatePlanet, PLANET_RADIUS));
    }

    @Test
    void aGateMoonCountsFromItsParentsOrbit() {
        PlanetAPI parent = orbiting(star, 5000f, 300f);
        PlanetAPI moon = orbiting(parent, 700f, 60f);

        assertEquals(5000f, PhaseDestination.distanceFromFocus(star, moon), 0.01f);
    }

    private SectorEntityToken nebulaCentre() {
        SectorEntityToken centre = mock(SectorEntityToken.class);
        when(centre.getLocation()).thenReturn(new Vector2f());
        when(system.getStar()).thenReturn(null);
        when(system.getCenter()).thenReturn(centre);
        terrain.clear();
        return centre;
    }

    private <T extends SectorEntityToken> T stationary(T body, float x, float y, float radius) {
        when(body.getLocation()).thenReturn(new Vector2f(x, y));
        when(body.getRadius()).thenReturn(radius);
        return body;
    }

    @Test
    void stationaryBodiesInANebulaKeepTheOrbitClear() {
        SectorEntityToken centre = nebulaCentre();
        PlanetAPI near = stationary(mock(PlanetAPI.class), 0f, 1200f, 100f);
        PlanetAPI ringed = stationary(mock(PlanetAPI.class), 3000f, 0f, 200f);
        planets.add(near);
        planets.add(ringed);
        planets.add(stationary(mock(PlanetAPI.class), 0f, -7000f, 150f));
        CustomCampaignEntityAPI station = mock(CustomCampaignEntityAPI.class);
        when(station.getOrbitFocus()).thenReturn(ringed);
        when(station.getCircularOrbitRadius()).thenReturn(400f);
        when(station.getRadius()).thenReturn(30f);
        CustomCampaignEntityAPI buoy = stationary(mock(CustomCampaignEntityAPI.class), 5000f, 5000f, 50f);
        when(system.getEntities(CustomCampaignEntityAPI.class)).thenReturn(new ArrayList<>(List.of(station, buoy)));
        JumpPointAPI jumpPoint = stationary(mock(JumpPointAPI.class), -9000f, 0f, 100f);
        when(system.getJumpPoints()).thenReturn(new ArrayList<>(List.of(jumpPoint)));

        assertSame(centre, PhaseDestination.focusOf(system));
        float orbit = PhaseDestination.orbitRadius(system, centre, PLANET_RADIUS, 3300f);

        assertEquals(3430f + PLANET_RADIUS + PhaseDestination.CLEARANCE, orbit, 0.01f);
    }

    @Test
    void aCrowdedNebulaPlacesThePlanetPastItsFarthestBody() {
        SectorEntityToken centre = nebulaCentre();
        planets.add(stationary(mock(PlanetAPI.class), 1000f, 0f, 200f));
        planets.add(stationary(mock(PlanetAPI.class), 0f, 1600f, 200f));
        planets.add(stationary(mock(PlanetAPI.class), -2200f, 0f, 200f));
        JumpPointAPI jumpPoint = stationary(mock(JumpPointAPI.class), 0f, -2600f, 100f);
        when(system.getJumpPoints()).thenReturn(new ArrayList<>(List.of(jumpPoint)));

        float orbit = PhaseDestination.orbitRadius(system, centre, PLANET_RADIUS, 1600f);

        assertEquals(2700f + PLANET_RADIUS + PhaseDestination.FALLBACK_CLEARANCE, orbit, 0.01f);
    }

    @Test
    void anEmptySystemPlacesThePlanetClearOfTheCorona() {
        float orbit = PhaseDestination.orbitRadius(system, star, PLANET_RADIUS, 0f);

        float clearOfCorona = Math.max(PhaseDestination.MIN_PAD, 300f) + PLANET_RADIUS + PhaseDestination.CLEARANCE;
        assertEquals(STAR_RADIUS + clearOfCorona, orbit);
    }
}
