package exiledsector.charts.layout.sketch;

import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledsector.charts.ChartTables;
import exiledsector.charts.layout.ChartLayoutRecipe;
import exiledsector.charts.layout.ChartLook;
import exiledsector.charts.layout.ChartShape;
import exiledsector.charts.layout.feature.ChartFeatures;
import exiledsector.charts.layout.feature.RingedGiant;
import exiledsector.charts.layout.feature.RoguePlanet;
import exiledsector.charts.layout.sketch.ChartSketchPalette.BodyKind;
import exiledsector.charts.layout.sketch.ChartSketchPalette.StarKind;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Body;
import exiledsector.charts.layout.sketch.ChartSystemSketch.Moon;
import exiledsector.charts.layout.sketch.ChartSystemSketch.PlanetRing;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartSystemSketcherFeatureTest {

    private static final ChartSketchPalette PALETTE = new ChartSketchPalette(
            List.of(new StarKind("star_yellow", 50f, 50f, 50f, 500f, 700f, false)),
            Map.of("cat_frozen", 100f, "cat_nothing", 50f),
            Map.of("cat_frozen", List.of(new BodyKind("frozen", 10f, 100f, 160f))));
    private static final Set<String> GIANT_TYPES = Set.of("gas_giant", "ice_giant");

    @BeforeEach
    void loadFeatures() throws Exception {
        ChartTables.loadFeatures();
    }

    @AfterEach
    void forgetFeatures() throws Exception {
        ChartFeatures.register(new JSONArray());
    }

    private static ChartLayoutRecipe featuring(String featureId) {
        ChartLook look = new ChartLook(List.of(), List.of(), Float.NaN, null, 0f, Map.of(), Map.of(), false, "");
        return new ChartLayoutRecipe(look, new ChartShape(Map.of(StarSystemType.BINARY_FAR, 1f), 3, 3, Float.NaN, Map.of(), featureId));
    }

    private static int lastPlanet(ChartSystemSketch sketch) {
        return sketch.planets().size() - 1;
    }

    private static void assertOutsideEveryOtherOrbit(ChartSystemSketch sketch, int featurePlanet) {
        Body feature = sketch.planets().get(featurePlanet);
        for (int index = 0; index < featurePlanet; index++) {
            Body other = sketch.planets().get(index);
            assertTrue(other.orbitRadius() + sketch.reachAround(index) < feature.orbitRadius() - sketch.reachAround(featurePlanet));
        }
        for (ChartSystemSketch.Belt belt : sketch.belts()) {
            assertTrue(belt.orbitRadius() + belt.width() / 2f < feature.orbitRadius() - sketch.reachAround(featurePlanet));
        }
    }

    private static void assertFarStarsClearThePlanets(ChartSystemSketch sketch) {
        assertEquals(StarSystemType.BINARY_FAR, sketch.systemType());
        assertEquals(2, sketch.stars().size());
        float planetsReach = 0f;
        for (int index = 0; index < sketch.planets().size(); index++) {
            planetsReach = Math.max(planetsReach, sketch.planets().get(index).orbitRadius() + sketch.reachAround(index));
        }
        Body farStar = sketch.stars().get(1);
        assertTrue(farStar.orbitRadius() - farStar.radius() > planetsReach);
    }

    @Test
    void aRingedGiantShowsAsTheOutermostPlanetWithItsBandsAndMoons() {
        ChartLayoutRecipe recipe = featuring(RingedGiant.FEATURE_ID);
        for (long seed = 0; seed < 60; seed++) {
            ChartSystemSketch sketch = ChartSystemSketcher.sketch(seed, recipe, List.of(), PALETTE);

            int giant = lastPlanet(sketch);
            Body giantBody = sketch.planets().get(giant);
            assertTrue(GIANT_TYPES.contains(giantBody.typeId()));
            assertTrue(giantBody.radius() >= 320f && giantBody.radius() <= 380f);
            assertEquals(3, sketch.planetRings().stream().filter(ring -> ring.planet() == giant).count());
            long moons = sketch.moons().stream().filter(moon -> moon.planet() == giant).count();
            assertTrue(moons == 3 || moons == 4);
            for (PlanetRing ring : sketch.planetRings()) {
                assertTrue(ring.belt().orbitRadius() - ring.belt().width() / 2f > giantBody.radius());
            }
            for (Moon moon : sketch.moons()) {
                assertTrue(moon.body().orbitRadius() > giantBody.radius() + 300f + 2f * 256f);
            }
            assertOutsideEveryOtherOrbit(sketch, giant);
            assertFarStarsClearThePlanets(sketch);
        }
    }

    @Test
    void aRoguePlanetShowsAsALoneWorldFarBeyondTheOrbits() {
        ChartLayoutRecipe recipe = featuring(RoguePlanet.FEATURE_ID);
        for (long seed = 0; seed < 60; seed++) {
            ChartSystemSketch sketch = ChartSystemSketcher.sketch(seed, recipe, List.of(), PALETTE);

            int rogue = lastPlanet(sketch);
            Body rogueBody = sketch.planets().get(rogue);
            assertTrue(Set.of("frozen", "frozen2", "barren", "rocky_ice", "cryovolcanic").contains(rogueBody.typeId()));
            assertTrue(sketch.moons().stream().noneMatch(moon -> moon.planet() == rogue));
            float othersReach = 0f;
            for (int index = 0; index < rogue; index++) {
                othersReach = Math.max(othersReach, sketch.planets().get(index).orbitRadius() + sketch.reachAround(index));
            }
            assertTrue(rogueBody.orbitRadius() >= othersReach + 4000f);
            assertFarStarsClearThePlanets(sketch);
        }
    }

    @Test
    void aShapeWithoutAFeatureAddsNothingBeyondItsOrbits() {
        for (long seed = 0; seed < 60; seed++) {
            ChartSystemSketch sketch = ChartSystemSketcher.sketch(seed, featuring(""), List.of(), PALETTE);

            assertTrue(sketch.planets().size() <= 3);
            assertTrue(sketch.planets().stream().allMatch(planet -> planet.typeId().equals("frozen")));
            assertTrue(sketch.moons().isEmpty());
            assertTrue(sketch.planetRings().isEmpty());
        }
    }

    @Test
    void theFeatureSketchIsTheSameForTheSameSeed() {
        ChartLayoutRecipe recipe = featuring(RingedGiant.FEATURE_ID);
        for (long seed = 0; seed < 20; seed++) {
            assertEquals(ChartSystemSketcher.sketch(seed, recipe, List.of(), PALETTE), ChartSystemSketcher.sketch(seed, recipe, List.of(), PALETTE));
        }
    }
}
