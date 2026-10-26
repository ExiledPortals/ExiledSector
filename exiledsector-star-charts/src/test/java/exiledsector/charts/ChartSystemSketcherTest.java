package exiledsector.charts;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.ChartSketchPalette.BodyKind;
import exiledsector.charts.ChartSketchPalette.StarKind;
import exiledsector.charts.ChartSystemSketch.Body;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartSystemSketcherTest {

    private static final ChartSketchPalette PALETTE = new ChartSketchPalette(
            List.of(new StarKind("star_red_dwarf", 75f, 65f, 55f, 500f, 600f, false), new StarKind("star_white", 10f, 20f, 50f, 400f, 700f, false),
                    new StarKind("nebula_center_old", 0f, 0f, 3f, 0f, 0f, true)),
            Map.of("cat_giant", 100f, "cat_frozen", 30f, "cat_terrain_rings", 200f, "cat_nothing", 150f),
            Map.of("cat_giant", List.of(new BodyKind("gas_giant", 10f, 250f, 350f)),
                    "cat_frozen", List.of(new BodyKind("frozen", 10f, 130f, 130f)),
                    "cat_terrain_rings", List.of(new BodyKind("asteroid_belt", 150f, 0f, 0f), new BodyKind("ring_ice", 100f, 0f, 0f))));

    private static ChartLayoutRecipe recipe(Map<String, Float> starWeights, Map<String, Float> planetWeights, ChartShape shape) {
        ChartLook look = new ChartLook(List.of(), List.of(), Float.NaN, null, 0f, starWeights, planetWeights, false, "");
        return new ChartLayoutRecipe(look, shape);
    }

    @Test
    void theSameSeedSketchesTheSameSystemWithTheRealGeneratorsStarCount() {
        for (long seed = 0; seed < 200; seed++) {
            ChartSystemSketch sketch = ChartSystemSketcher.sketch(seed, ChartLayoutRecipe.ORDINARY, "", PALETTE);

            assertEquals(sketch, ChartSystemSketcher.sketch(seed, ChartLayoutRecipe.ORDINARY, "", PALETTE));
            StarSystemType type = ChartLayoutGenerator.typeFor(new ChartLayoutRequest(seed, null));
            assertEquals(type, sketch.systemType());
            assertEquals(1 + ChartSystemSketcher.closeStars(type) + ChartSystemSketcher.farStars(type), sketch.stars().size());
        }
    }

    @Test
    void aDeepSpaceSketchHasNoStarAndOneWorldAtTheCentre() {
        ChartLayoutRecipe starless = recipe(Map.of(), Map.of(), new ChartShape(Map.of(StarSystemType.DEEP_SPACE, 1f), 0, 0, Float.NaN, Map.of(), ""));
        Set<String> centres = new HashSet<>();

        for (long seed = 0; seed < 200; seed++) {
            ChartSystemSketch sketch = ChartSystemSketcher.sketch(seed, starless, "", PALETTE);

            assertEquals(StarSystemType.DEEP_SPACE, sketch.systemType());
            assertTrue(sketch.stars().isEmpty());
            assertEquals(1, sketch.planets().size());
            assertEquals(0f, sketch.planets().get(0).orbitRadius());
            centres.add(sketch.planets().get(0).typeId());
        }
        assertTrue(centres.contains("gas_giant") || centres.contains("ice_giant"), centres.toString());
        assertTrue(centres.contains("frozen"), centres.toString());
        assertTrue(ChartDeepSpace.PLANETOIDS.keySet().containsAll(centres.stream().filter(type -> !ChartDeepSpace.GIANTS.containsKey(type)).toList()));
    }

    @Test
    void theRegionLeaningsDecideWhichStarsAndPlanetsAppear() {
        ChartLayoutRecipe frozenOnly = recipe(Map.of("star_red_dwarf", 0f),
                Map.of("cat_giant", 0f, "cat_terrain_rings", 0f, "cat_nothing", 0f), new ChartShape(Map.of(), 4, 4, Float.NaN, Map.of(), ""));

        for (long seed = 0; seed < 50; seed++) {
            ChartSystemSketch sketch = ChartSystemSketcher.sketch(seed, frozenOnly, "", PALETTE);

            assertTrue(sketch.stars().stream().allMatch(star -> star.typeId().equals("star_white")));
            assertEquals(4, sketch.planets().size());
            assertTrue(sketch.planets().stream().allMatch(planet -> planet.typeId().equals("frozen")));
            assertTrue(sketch.belts().isEmpty());
        }
    }

    @Test
    void orbitsStartInsideTheShapesRadiusLimit() {
        ChartLayoutRecipe tight = recipe(Map.of(), Map.of("cat_nothing", 0f), new ChartShape(Map.of(), 12, 12, 5000f, Map.of(), ""));

        for (long seed = 0; seed < 50; seed++) {
            ChartSystemSketch sketch = ChartSystemSketcher.sketch(seed, tight, "", PALETTE);

            assertTrue(sketch.planets().size() + sketch.belts().size() < 12);
            for (Body planet : sketch.planets()) {
                assertTrue(planet.orbitRadius() - planet.radius() < 5000f);
            }
        }
    }

    @Test
    void beltsUseTheRingTerrainKindsAndPlanetsOrbitOutsideTheStars() {
        Set<String> seenBelts = new HashSet<>();
        for (long seed = 0; seed < 100; seed++) {
            ChartSystemSketch sketch = ChartSystemSketcher.sketch(seed, ChartLayoutRecipe.ORDINARY, "", PALETTE);
            sketch.belts().forEach(belt -> seenBelts.add(belt.typeId()));
            float centreReach = sketch.stars().stream().filter(star -> star.orbitDays() < ChartSystemSketcher.CLOSE_STAR_DAYS * 2f + 1f)
                    .map(star -> star.orbitRadius() + star.radius()).max(Float::compare).orElse(0f);
            for (Body planet : sketch.planets()) {
                assertTrue(planet.orbitRadius() > centreReach);
                assertTrue(planet.orbitDays() > 0f);
            }
        }
        assertEquals(Set.of("asteroid_belt", "ring_ice"), seenBelts);
    }

    @Test
    void anEmptyPaletteFallsBackToALoneWhiteStar() {
        ChartSystemSketch sketch = ChartSystemSketcher.sketch(7L, recipe(Map.of(), Map.of(), new ChartShape(Map.of(
                StarSystemType.SINGLE, 1f), -1, -1, Float.NaN, Map.of(), "")), "", ChartSketchPalette.EMPTY);

        assertEquals(List.of(new Body(ChartSystemSketcher.FALLBACK_STAR, ChartSystemSketcher.FALLBACK_STAR_RADIUS, 0f, 0f, 0f)), sketch.stars());
        assertTrue(sketch.planets().isEmpty());
        assertFalse(sketch.outerRadius() <= 0f);
    }

    @Test
    void thePaletteKeepsTheTablesOrderSoSketchesSurviveARestart() {
        Map<String, Float> categories = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            categories.put("cat_" + i, 1f);
        }

        ChartSketchPalette palette = new ChartSketchPalette(List.of(), categories, Map.of());

        assertEquals(List.copyOf(categories.keySet()), List.copyOf(palette.categories().keySet()));
    }
}
