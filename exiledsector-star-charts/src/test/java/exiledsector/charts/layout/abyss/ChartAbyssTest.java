package exiledsector.charts.layout.abyss;

import exiledsector.charts.layout.ChartDeepSpace;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartAbyssTest {

    private static final AbyssPalette RED = new AbyssPalette("red", new Color(255, 0, 50), new Color(77, 0, 15), 1f);
    private static final AbyssPalette AMBER = new AbyssPalette("amber", new Color(255, 123, 0), new Color(77, 37, 0), 0.5f);

    @Test
    void theLightLeansFromDeepSpaceAmbientTowardThePaletteColour() {
        Color light = ChartAbyss.light(RED);
        Color ambient = ChartDeepSpace.AMBIENT_LIGHT;

        assertTrue(light.getRed() > ambient.getRed());
        assertTrue(light.getGreen() < ambient.getGreen());
        assertEquals(Math.round(ambient.getBlue() + (50 - ambient.getBlue()) * ChartAbyss.LIGHT_TINT), light.getBlue());
    }

    @Test
    void satellitePhotospheresSitAColossalSpacingFromTheCentreAndFromEachOther() {
        for (long seed = 0; seed < 40; seed++) {
            List<Vector2f> spots = ChartAbyss.satelliteSpots(ChartAbyss.MAX_SATELLITES, new Random(seed));
            float fogReach = ChartAbyss.fogReach(ChartAbyss.reach(0f), spots);

            assertEquals(ChartAbyss.MAX_SATELLITES, spots.size(), "seed " + seed);
            for (int i = 0; i < spots.size(); i++) {
                float distance = spots.get(i).length();
                assertTrue(distance >= ChartAbyss.SATELLITE_SPACING - 0.01f
                        && distance <= ChartAbyss.SATELLITE_SPACING + ChartAbyss.SATELLITE_SPREAD + 0.01f, "seed " + seed);
                assertTrue(fogReach >= distance + ChartAbyss.SATELLITE_CORE_RADIUS, "seed " + seed);
                for (int j = i + 1; j < spots.size(); j++) {
                    assertTrue(Vector2f.sub(spots.get(i), spots.get(j), null).length() >= ChartAbyss.SATELLITE_SPACING, "seed " + seed);
                }
            }
        }
    }

    @Test
    void lightsReachAsFarAsRandomAssortmentOfThingsColossalPhotospheres() {
        for (long seed = 0; seed < 20; seed++) {
            ChartAbyss.Light satellite = ChartAbyss.rollLight(new Vector2f(), ChartAbyss.SATELLITE_CORE_RADIUS, 0f, new Random(seed));
            ChartAbyss.Light centre = ChartAbyss.rollLight(new Vector2f(), ChartAbyss.CENTRE_CORE_RADIUS, ChartAbyss.CENTRE_EXTRA_LIGHT, new Random(seed));

            assertTrue(satellite.lightRadius() >= 600f + 41500f && satellite.lightRadius() <= 600f + 43000f, "seed " + seed);
            assertTrue(centre.lightRadius() >= 1000f + 45500f && centre.lightRadius() <= 1000f + 47000f, "seed " + seed);
        }
    }

    @Test
    void siteSpotsRingEachPhotosphereOutsideItsCore() {
        Vector2f centre = new Vector2f(14000f, -3000f);
        List<Vector2f> spots = ChartAbyss.spotsAround(centre, ChartAbyss.SATELLITE_CORE_RADIUS, new Random(5L));

        assertEquals(ChartAbyss.SITE_SPOTS_PER_PHOTOSPHERE, spots.size());
        for (Vector2f spot : spots) {
            float distance = Vector2f.sub(spot, centre, null).length();
            assertTrue(distance >= ChartAbyss.SATELLITE_CORE_RADIUS + ChartAbyss.MIN_SITE_GAP - 0.5f, String.valueOf(distance));
            assertTrue(distance <= ChartAbyss.SATELLITE_CORE_RADIUS + ChartAbyss.MAX_SITE_GAP + 0.5f, String.valueOf(distance));
        }
    }

    @Test
    void theReachNeverFallsBelowTheMinimum() {
        assertEquals(ChartAbyss.MIN_REACH, ChartAbyss.reach(0f));
        assertEquals(15000f + ChartAbyss.REACH_MARGIN, ChartAbyss.reach(15000f));
    }

    @Test
    void fogGridsArePowersOfTwoThatCoverTheReachUpToTheCap() {
        assertEquals(ChartAbyss.MIN_FOG_TILES, ChartAbyss.fogTiles(1000f));
        assertEquals(64, ChartAbyss.fogTiles(9000f));
        assertEquals(128, ChartAbyss.fogTiles(18000f));
        assertEquals(ChartAbyss.MAX_FOG_TILES, ChartAbyss.fogTiles(1000000f));
        for (float reach = 1000f; reach < 25000f; reach += 700f) {
            int tiles = ChartAbyss.fogTiles(reach);
            assertEquals(0, tiles & (tiles - 1), "reach " + reach);
            assertTrue(tiles == ChartAbyss.MAX_FOG_TILES || tiles * ChartAbyss.FOG_TILE_SIZE >= reach * 2f, "reach " + reach);
        }
    }

    @Test
    void paletteVignettesAreClampedToAFraction() {
        assertEquals(1f, new AbyssPalette("bright", Color.RED, Color.BLACK, 3f).vignette());
        assertEquals(0f, new AbyssPalette("dark", Color.RED, Color.BLACK, -1f).vignette());
    }
}
