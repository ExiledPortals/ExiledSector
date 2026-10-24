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

    private static ChartLayoutRecipe recipe(Map<String, Float> stars, Map<String, Float> categories, Color tint, float strength) {
        return new ChartLayoutRecipe(stars, categories, List.of(), List.of(), Float.NaN, tint, strength, Map.of(), -1, -1, Float.NaN, "", false);
    }

    @Test
    void phenomenonStarsAndCategoriesNeverComeFromOrdinaryGeneration() {
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
    void theLightTintBlendsTowardsItsColourByItsStrength() {
        Color light = new Color(200, 100, 0, 255);

        assertEquals(new Color(150, 100, 50, 255), recipe(Map.of(), Map.of(), new Color(100, 100, 100), 0.5f).tinted(light));
        assertSame(light, recipe(Map.of(), Map.of(), null, 0.5f).tinted(light));
        assertSame(light, recipe(Map.of(), Map.of(), Color.RED, 0f).tinted(light));
    }

    @Test
    void orbitSettingsFallBackToVanillaUnlessALayoutSetsThem() {
        ChartLayoutRecipe set = new ChartLayoutRecipe(Map.of(), Map.of(), List.of(), List.of(), Float.NaN, null, 0f, Map.of(), 5, 3, 9000f, "",
                false);

        assertFalse(ChartLayoutRecipe.ORDINARY.setsOrbits());
        assertEquals(ChartLayoutRecipe.VANILLA_ORBIT_RADIUS, ChartLayoutRecipe.ORDINARY.orbitRadiusLimit());
        assertTrue(set.setsOrbits());
        assertEquals(5, set.maxOrbits());
        assertEquals(9000f, set.orbitRadiusLimit());
    }

    @Test
    void aRecipesSystemTypeWeightsDecideTheTypeUnlessTheRequestNamesOne() {
        ChartLayoutRecipe trinary = new ChartLayoutRecipe(Map.of(), Map.of(), List.of(), List.of(), Float.NaN, null, 0f,
                Map.of(StarSystemType.TRINARY_2FAR, 1f), -1, -1, Float.NaN, "", false);

        for (long seed = 0; seed < 20; seed++) {
            assertEquals(StarSystemType.TRINARY_2FAR, ChartLayoutGenerator.typeFor(new ChartLayoutRequest(seed, null).withRecipe(trinary)));
        }
        assertEquals(StarSystemType.SINGLE, ChartLayoutGenerator.typeFor(new ChartLayoutRequest(1L, StarSystemType.SINGLE).withRecipe(trinary)));
        assertEquals(StarSystemType.SINGLE, ChartLayoutGenerator.pickType(new Random(3), Map.of()));
    }
}
