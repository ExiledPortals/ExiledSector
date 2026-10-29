package exiledsector.charts.layout.sketch;

import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.RingBandAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Belt;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Body;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SectorSystemSketcherTest {

    private static PlanetAPI planet(String typeId, float radius, float x, float y, boolean star, SectorEntityToken focus, float period) {
        PlanetAPI planet = mock(PlanetAPI.class);
        when(planet.getTypeId()).thenReturn(typeId);
        when(planet.getRadius()).thenReturn(radius);
        when(planet.getLocation()).thenReturn(new Vector2f(x, y));
        when(planet.isStar()).thenReturn(star);
        if (focus != null) {
            OrbitAPI orbit = mock(OrbitAPI.class);
            when(orbit.getFocus()).thenReturn(focus);
            when(orbit.getOrbitalPeriod()).thenReturn(period);
            when(planet.getOrbit()).thenReturn(orbit);
        }
        return planet;
    }

    @Test
    void theSketchKeepsTheStarPlanetsAndBandsAroundTheCentreButNotMoons() {
        PlanetAPI star = planet("star_yellow", 600f, 0f, 0f, true, null, 0f);
        PlanetAPI planet = planet("barren", 120f, 0f, 3000f, false, star, 200f);
        PlanetAPI moon = planet("frozen", 40f, 0f, 3400f, false, planet, 30f);
        RingBandAPI band = mock(RingBandAPI.class);
        when(band.getFocus()).thenReturn(star);
        when(band.getSpriteKey()).thenReturn("rings_asteroids0");
        when(band.getMiddleRadius()).thenReturn(5000f);
        when(band.getBandWidthInEngine()).thenReturn(256f);
        when(band.getOrbitDays()).thenReturn(300f);
        when(band.getBandIndex()).thenReturn(2);
        StarSystemAPI system = mock(StarSystemAPI.class);
        when(system.getCenter()).thenReturn(star);
        when(system.getType()).thenReturn(StarSystemType.SINGLE);
        when(system.getPlanets()).thenReturn(List.of(star, planet, moon));
        when(system.getEntities(RingBandAPI.class)).thenReturn(List.of(band));

        ChartSystemSketch sketch = SectorSystemSketcher.sketch(system);

        assertEquals(List.of(new Body("star_yellow", 600f, 0f, 0f, 0f)), sketch.stars());
        assertEquals(List.of(new Body("barren", 120f, 3000f, 200f, 90f)), sketch.planets());
        assertEquals(List.of(new Belt("rings_asteroids0", 5000f, 256f, 300f, 2)), sketch.belts());
    }

    @Test
    void noGatewaySystemGivesNoSketch() {
        assertNull(SectorSystemSketcher.sketch(null));
    }
}
