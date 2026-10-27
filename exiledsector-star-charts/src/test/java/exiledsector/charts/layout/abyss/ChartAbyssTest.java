package exiledsector.charts.layout.abyss;

import exiledsector.charts.layout.ChartDeepSpace;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartAbyssTest {

    private static final AbyssPalette RED = new AbyssPalette("red", new Color(255, 0, 50), new Color(77, 0, 15), 1f);
    private static final AbyssPalette AMBER = new AbyssPalette("amber", new Color(255, 123, 0), new Color(77, 37, 0), 0.5f);

    @Test
    void anEmptyOrZeroWeightPaletteListPicksNothing() {
        assertNull(ChartAbyss.pick(Map.of(), new Random(1)));
        assertNull(ChartAbyss.pick(Map.of(RED, 0f), new Random(1)));
    }

    @Test
    void palettesArePickedByWeightAndRepeatForTheSameSeed() {
        Map<AbyssPalette, Float> weights = new LinkedHashMap<>();
        weights.put(RED, 1f);
        weights.put(AMBER, 0f);
        Set<String> picked = new HashSet<>();
        for (long seed = 0; seed < 50; seed++) {
            picked.add(ChartAbyss.pick(weights, new Random(seed)).id());
        }
        weights.put(AMBER, 3f);

        assertEquals(Set.of("red"), picked);
        assertEquals(ChartAbyss.pick(weights, new Random(7)), ChartAbyss.pick(weights, new Random(7)));
    }

    @Test
    void theLightLeansFromDeepSpaceAmbientTowardThePaletteColour() {
        Color light = ChartAbyss.light(RED);
        Color ambient = ChartDeepSpace.AMBIENT_LIGHT;

        assertTrue(light.getRed() > ambient.getRed());
        assertTrue(light.getGreen() < ambient.getGreen());
        assertEquals(Math.round(ambient.getBlue() + (50 - ambient.getBlue()) * ChartAbyss.LIGHT_TINT), light.getBlue());
    }

    @Test
    void satellitePhotospheresStayInsideTheReachAndApartFromEachOther() {
        float reach = ChartAbyss.reach(8000f);
        for (long seed = 0; seed < 40; seed++) {
            List<Vector2f> spots = ChartAbyss.satelliteSpots(ChartAbyss.MAX_SATELLITES, reach, new Random(seed));

            assertTrue(!spots.isEmpty(), "seed " + seed);
            for (int i = 0; i < spots.size(); i++) {
                float distance = spots.get(i).length();
                assertTrue(distance >= reach * ChartAbyss.SATELLITE_INNER_SHARE - 0.01f && distance <= reach + 0.01f, "seed " + seed);
                for (int j = i + 1; j < spots.size(); j++) {
                    assertTrue(Vector2f.sub(spots.get(i), spots.get(j), null).length() >= ChartAbyss.SATELLITE_SPACING, "seed " + seed);
                }
            }
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
