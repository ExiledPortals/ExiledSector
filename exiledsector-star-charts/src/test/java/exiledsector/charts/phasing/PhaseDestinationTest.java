package exiledsector.charts.phasing;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain.RingParams;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeAll
    static void loadThemeGenerator() throws ClassNotFoundException {
        ThemeGeneratorLoading.ensureLoaded();
    }

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

    private void orbiting(SectorEntityToken focus, float orbitRadius, float radius) {
        PlanetAPI planet = mock(PlanetAPI.class);
        when(planet.getOrbitFocus()).thenReturn(focus);
        when(planet.getCircularOrbitRadius()).thenReturn(orbitRadius);
        when(planet.getRadius()).thenReturn(radius);
        when(planet.getContainingLocation()).thenReturn(system);
        planets.add(planet);
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
    void thePlanetGoesInTheMiddleOfTheWidestGap() {
        orbiting(star, 2000f, 100f);
        orbiting(star, 6000f, 100f);
        orbiting(star, 8000f, 100f);

        assertEquals(4000f, PhaseDestination.orbitRadius(system, star, PLANET_RADIUS));
    }

    @Test
    void aGapTooNarrowForThePlanetIsSkipped() {
        orbiting(star, 2000f, 200f);
        orbiting(star, 2600f, 200f);
        orbiting(star, 3200f, 200f);

        float expected = 3400f + PLANET_RADIUS + PhaseDestination.FALLBACK_CLEARANCE;
        assertEquals(expected, PhaseDestination.orbitRadius(system, star, PLANET_RADIUS));
    }

    @Test
    void anEmptySystemPlacesThePlanetClearOfTheCorona() {
        float orbit = PhaseDestination.orbitRadius(system, star, PLANET_RADIUS);

        float minPad = 300f + PLANET_RADIUS + PhaseDestination.CLEARANCE;
        assertEquals(STAR_RADIUS + minPad + PLANET_RADIUS + PhaseDestination.FALLBACK_CLEARANCE, orbit);
    }
}
