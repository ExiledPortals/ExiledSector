package exiledsector.charts;

import com.fs.starfarer.api.campaign.PlanetAPI;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartBodyNamesTest {

    @Test
    void starsAreLetteredPlanetsNumberedOutwardAndMoonsLetteredAfterTheirPlanet() {
        FakeSystem fake = new FakeSystem();
        PlanetAPI primary = fake.addStar("star", 600f);
        PlanetAPI secondary = fake.addStar("star_b", 300f);
        when(fake.system.getSecondary()).thenReturn(secondary);
        PlanetAPI outer = fake.addPlanet("outer", 200f, primary, 9000f);
        PlanetAPI inner = fake.addPlanet("inner", 150f, primary, 3000f);
        PlanetAPI firstMoon = fake.addPlanet("moon_1", 60f, outer, 400f);
        PlanetAPI secondMoon = fake.addPlanet("moon_2", 50f, outer, 700f);

        ChartBodyNames.assign(fake.system, "1A2B");

        assertEquals(ChartBodyNames.starName("1A2B", 'A'), fake.names.get(primary));
        assertEquals(ChartBodyNames.starName("1A2B", 'B'), fake.names.get(secondary));
        assertEquals(ChartBodyNames.planetName("1A2B", 1), fake.names.get(inner));
        assertEquals(ChartBodyNames.planetName("1A2B", 2), fake.names.get(outer));
        assertEquals(ChartBodyNames.moonName(fake.names.get(outer), 0), fake.names.get(firstMoon));
        assertEquals(ChartBodyNames.moonName(fake.names.get(outer), 1), fake.names.get(secondMoon));
        verify(outer.getMarket()).setName(fake.names.get(outer));
        assertTrue(fake.names.get(inner).contains("1A2B"), fake.names.get(inner));
    }

    @Test
    void theDesignationComesFromTheSeedAndIsFourHexDigits() {
        String designation = ChartBodyNames.designation(new Random(3L));

        assertEquals(designation, ChartBodyNames.designation(new Random(3L)));
        assertTrue(designation.matches("[0-9A-F]{4}"), designation);
        assertNotEquals(ChartBodyNames.planetName(designation, 1), ChartBodyNames.planetName(designation, 2));
    }
}
