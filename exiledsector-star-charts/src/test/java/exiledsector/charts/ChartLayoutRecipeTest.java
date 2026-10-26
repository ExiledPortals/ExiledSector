package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartLayoutRecipeTest {

    private static ChartLook look(Map<String, Float> stars, Map<String, Float> planets, Color tint, float strength) {
        return new ChartLook(List.of(), List.of(), Float.NaN, tint, strength, stars, planets, false, "");
    }

    private static ChartLayoutRecipe recipe(Map<String, Float> stars, Map<String, Float> planets, Color tint, float strength) {
        return new ChartLayoutRecipe(look(stars, planets, tint, strength), ChartShape.ANY);
    }

    @Test
    void anomalyStarsAndCategoriesNeverComeFromOrdinaryGeneration() {
        ChartLayoutRecipe leaning = recipe(Map.of("black_hole", 9f, "star_red_dwarf", 2f), Map.of("cat_magfield", 9f, "cat_frozen", 3f), null, 0f);

        assertEquals(0f, leaning.starWeight("black_hole", 5f));
        assertEquals(0f, ChartLayoutRecipe.ORDINARY.starWeight("star_neutron", 5f));
        assertEquals(10f, leaning.starWeight("star_red_dwarf", 5f));
        assertEquals(5f, leaning.starWeight("star_yellow", 5f));
        assertEquals(0f, leaning.categoryWeight("cat_magfield", 4f));
        assertEquals(12f, leaning.categoryWeight("cat_frozen", 4f));
        assertEquals(4f, ChartLayoutRecipe.ORDINARY.categoryWeight("cat_giant", 4f));
    }

    @Test
    void aLayoutsTerrainWeightsMultiplyTheRegionsPlanetWeights() {
        ChartLayoutRecipe ringed = new ChartLayoutRecipe(look(Map.of(), Map.of("cat_terrain_rings", 3f), null, 0f),
                new ChartShape(Map.of(), -1, -1, Float.NaN, Map.of("cat_terrain_rings", 2f, "cat_giant", 4f), ""));

        assertEquals(6f, ringed.categoryWeight("cat_terrain_rings", 1f));
        assertEquals(4f, ringed.categoryWeight("cat_giant", 1f));
    }

    @Test
    void theLightTintBlendsTowardsItsColourByItsStrength() {
        Color light = new Color(200, 100, 0, 255);

        assertEquals(new Color(150, 100, 50, 255), look(Map.of(), Map.of(), new Color(100, 100, 100), 0.5f).tinted(light));
        assertSame(light, look(Map.of(), Map.of(), null, 0.5f).tinted(light));
        assertSame(light, look(Map.of(), Map.of(), Color.RED, 0f).tinted(light));
    }

    @Test
    void orbitSettingsFallBackToVanillaUnlessALayoutSetsThem() {
        ChartShape set = new ChartShape(Map.of(), 5, 3, 9000f, Map.of(), "");

        assertFalse(ChartShape.ANY.setsOrbits());
        assertEquals(ChartShape.VANILLA_ORBIT_RADIUS, ChartShape.ANY.orbitRadiusLimit());
        assertTrue(set.setsOrbits());
        assertEquals(5, set.maxOrbits());
        assertEquals(9000f, set.orbitRadiusLimit());
    }

    @Test
    void aRecipesSystemTypeWeightsDecideTheTypeUnlessTheRequestNamesOne() {
        ChartLayoutRecipe trinary = new ChartLayoutRecipe(ChartLook.PLAIN,
                new ChartShape(Map.of(StarSystemType.TRINARY_2FAR, 1f), -1, -1, Float.NaN, Map.of(), ""));

        for (long seed = 0; seed < 20; seed++) {
            assertEquals(StarSystemType.TRINARY_2FAR, ChartLayoutGenerator.typeFor(new ChartLayoutRequest(seed, null).withRecipe(trinary)));
        }
        assertEquals(StarSystemType.SINGLE, ChartLayoutGenerator.typeFor(new ChartLayoutRequest(1L, StarSystemType.SINGLE).withRecipe(trinary)));
        assertEquals(StarSystemType.SINGLE, ChartLayoutGenerator.pickType(new Random(3), Map.of()));
    }
}
