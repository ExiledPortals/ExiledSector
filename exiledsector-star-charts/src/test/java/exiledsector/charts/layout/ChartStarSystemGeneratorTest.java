package exiledsector.charts.layout;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.CategoryGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.GenContext;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.GenResult;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import com.fs.starfarer.api.impl.campaign.procgen.TerrainGenDataSpec;
import exiledlib.MockSector;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin.CoronaParams;
import exiledsector.charts.ChartWormholePlacement;
import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.anomaly.ChartCoronaTerrainPlugin;
import exiledsector.charts.anomaly.ChartPulsarBeamTerrainPlugin;
import exiledsector.charts.anomaly.StarAnomalies;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChartStarSystemGeneratorTest {

    private static final List<String> STARS = List.of("star_yellow", "star_red_dwarf", "black_hole", "star_neutron");
    private static final List<String> CATEGORIES = List.of("cat_barren", "cat_frozen", "cat_magfield");

    private MockSector mockSector;
    private Random previousRandom;

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        SettingsAPI settings = mock(SettingsAPI.class);
        mockSector.global.when(Global::getSettings).thenReturn(settings);
        List<PlanetSpecAPI> specs = new ArrayList<>();
        for (String id : STARS) {
            PlanetSpecAPI spec = mock(PlanetSpecAPI.class);
            when(spec.isStar()).thenReturn(true);
            when(spec.getPlanetType()).thenReturn(id);
            specs.add(spec);
            StarGenDataSpec data = mock(StarGenDataSpec.class);
            when(data.getFreqOLD()).thenReturn(10f);
            when(settings.getSpec(StarGenDataSpec.class, id, true)).thenReturn(data);
        }
        when(settings.getAllPlanetSpecs()).thenReturn(specs);
        List<CategoryGenDataSpec> categories = new ArrayList<>();
        for (String id : CATEGORIES) {
            CategoryGenDataSpec category = mock(CategoryGenDataSpec.class);
            when(category.getCategory()).thenReturn(id);
            when(category.getFrequency()).thenReturn(10f);
            when(category.getMultiplier(any())).thenReturn(1f);
            categories.add(category);
        }
        when(settings.getAllSpecs(eq(CategoryGenDataSpec.class))).thenReturn(categories);
        previousRandom = StarSystemGenerator.random;
        StarSystemGenerator.random = new Random(11);
    }

    @AfterEach
    void tearDown() {
        StarSystemGenerator.random = previousRandom;
        mockSector.close();
    }

    private static ChartLayoutRecipe recipe(Map<String, Float> stars, Map<String, Float> categories) {
        return new ChartLayoutRecipe(new ChartLook(List.of(), List.of(), Float.NaN, null, 0f, stars, categories, false, ""), ChartShape.ANY);
    }

    @Test
    void starsSkipBlackHolesAndNeutronStarsAndFollowTheRegionWeights() {
        ChartStarSystemGenerator ordinary = new ChartStarSystemGenerator(StarAge.OLD, StarSystemType.SINGLE, ChartLayoutRecipe.ORDINARY, null);
        ChartStarSystemGenerator dwarfsOnly = new ChartStarSystemGenerator(StarAge.OLD, StarSystemType.SINGLE,
                recipe(Map.of("star_yellow", 0f), Map.of()), null);
        Set<String> seen = new HashSet<>();

        for (int pick = 0; pick < 200; pick++) {
            seen.add(ordinary.pickStar(StarAge.OLD).getPlanetType());
            assertEquals("star_red_dwarf", dwarfsOnly.pickStar(StarAge.OLD).getPlanetType());
        }

        assertEquals(Set.of("star_yellow", "star_red_dwarf"), seen);
    }

    @Test
    void aStarAnomalyForcesOnlyThePrimaryStar() {
        ChartStarSystemGenerator blackHole = new ChartStarSystemGenerator(StarAge.OLD, StarSystemType.SINGLE, ChartLayoutRecipe.ORDINARY,
                java.util.List.of(exiledsector.charts.anomaly.StarAnomalies.BLACK_HOLE));

        assertEquals("black_hole", blackHole.pickStar(StarAge.OLD).getPlanetType());
        for (int pick = 0; pick < 50; pick++) {
            assertFalse(ChartLayoutRecipe.ANOMALY_STARS.contains(blackHole.pickStar(StarAge.OLD).getPlanetType()));
        }
    }

    @Test
    void aDeepSpaceLayoutPutsOneLoneWorldAtTheCentreUnderAmbientLight() {
        SettingsAPI settings = Global.getSettings();
        StarGenDataSpec centreData = mock(StarGenDataSpec.class);
        when(centreData.getAge()).thenReturn(StarAge.OLD);
        when(settings.getSpec(StarGenDataSpec.class, "nebula_center_old", false)).thenReturn(centreData);
        when(settings.getSpec(eq(PlanetGenDataSpec.class), any(), eq(true))).thenReturn(mock(PlanetGenDataSpec.class));
        StarSystemAPI system = mock(StarSystemAPI.class);
        SectorEntityToken centreToken = mock(SectorEntityToken.class);
        when(system.initNonStarCenter()).thenReturn(centreToken);
        PlanetAPI world = mock(PlanetAPI.class);
        PlanetSpecAPI worldSpec = mock(PlanetSpecAPI.class);
        when(world.getSpec()).thenReturn(worldSpec);
        when(worldSpec.getDescriptionId()).thenReturn("barren");
        GenResult added = new GenResult();
        added.entities = new ArrayList<>(List.of(world));
        ChartLayoutRecipe starless = new ChartLayoutRecipe(new ChartLook(List.of("graphics/backgrounds/background4.jpg"), List.of(), 0f, null, 0f,
                Map.of(), Map.of(), false, ""), new ChartShape(Map.of(StarSystemType.DEEP_SPACE, 1f), 0, 0, Float.NaN, Map.of(), ""));
        ChartStarSystemGenerator generator = spy(new ChartStarSystemGenerator(StarAge.OLD, StarSystemType.DEEP_SPACE, starless, null));
        doReturn(added).when(generator).addPlanet(any(), any(), anyBoolean(), anyBoolean());

        generator.generateLayout(system, "chart_star");

        verify(system).setType(StarSystemType.DEEP_SPACE);
        verify(system).setLightColor(ChartDeepSpace.AMBIENT_LIGHT);
        verify(centreToken).addTag(Tags.AMBIENT_LS);
        verify(world).setOrbit(null);
        verify(world).setLocation(0f, 0f);
        verify(world).setDescriptionIdOverride("barren_deep_space");
        assertSame(world, generator.featureAnchor());
    }

    @Test
    void neutronStarChartsAlwaysGetSevenPlanetsInsideThePulsarsCover() {
        for (long seed = 0; seed < 40; seed++) {
            for (float orbitLimit : List.of(9000f, 20000f)) {
                StarSystemGenerator.random = new Random(seed);
                TopUpLayout layout = new TopUpLayout(orbitLimit);

                layout.generate();

                assertTrue(layout.planets.size() >= StarAnomalies.NEUTRON_STAR.minPlanets(), "seed " + seed);
                long covered = layout.addedRadii.stream().filter(radius -> radius < SMALLEST_PULSAR_COVER).count();
                assertTrue(covered * 2 > layout.addedRadii.size(), "seed " + seed);
            }
        }
    }

    @Test
    void anOrdinaryChartGetsNoExtraPlanets() {
        TopUpLayout layout = new TopUpLayout(20000f, List.of());

        layout.generate();

        assertTrue(layout.planets.isEmpty());
    }

    @Test
    void pastTheOrbitLimitTheTopUpFillsGapsBetweenOrbitsBeforeGoingFurtherOut() {
        TopUpLayout layout = new TopUpLayout(6000f);
        layout.generate();
        layout.planets.clear();
        layout.addedRadii.clear();
        layout.planets.add(layout.planetAt(1500f));
        layout.planets.add(layout.planetAt(5000f));
        GenResult existing = new GenResult();
        existing.orbitalWidth = 5100f * 2f;

        layout.generator.topUpPlanets(existing, 6000f);

        assertEquals(StarAnomalies.NEUTRON_STAR.minPlanets(), layout.planets.size());
        for (float radius : layout.addedRadii) {
            assertTrue(radius <= 6000f, "radius " + radius);
            for (float taken : List.of(1500f, 5000f)) {
                assertTrue(Math.abs(radius - taken) > TOP_UP_PLANET_RADIUS + ChartWormholePlacement.PLANET_CLEARANCE, "radius " + radius);
            }
        }
    }

    @Test
    void topUpOrbitsPickOnlyPlanetCategories() {
        TerrainGenDataSpec rings = mock(TerrainGenDataSpec.class);
        when(rings.getCategory()).thenReturn("cat_terrain_rings");
        when(Global.getSettings().getAllSpecs(TerrainGenDataSpec.class)).thenReturn(List.of(rings));
        TopUpLayout layout = new TopUpLayout(20000f);

        layout.generate();

        assertFalse(layout.contexts.isEmpty());
        assertTrue(layout.contexts.stream().allMatch(context -> context.excludeCategories.contains("cat_terrain_rings")));
    }

    @Test
    void neutronStarsGetTheChartCoronaAndPulsarBeamWithVanillasParameters() {
        StarSystemAPI system = mock(StarSystemAPI.class);
        StarGenDataSpec neutronData = mock(StarGenDataSpec.class);
        when(neutronData.getCoronaMult()).thenReturn(100f);
        when(neutronData.getCoronaMin()).thenReturn(2000f);
        when(neutronData.getSolarWind()).thenReturn(50f);
        when(neutronData.getCrLossMult()).thenReturn(15f);
        when(Global.getSettings().getSpec(StarGenDataSpec.class, "star_neutron", false)).thenReturn(neutronData);
        SectorEntityToken beamToken = mock(SectorEntityToken.class);
        when(system.addTerrain(eq(ChartPulsarBeamTerrainPlugin.TERRAIN_ID), any())).thenReturn(beamToken);
        PlanetAPI neutronStar = star("star_neutron", system);
        PlanetAPI yellowStar = star("star_yellow", system);
        ChartStarSystemGenerator generator = new ChartStarSystemGenerator(StarAge.OLD, StarSystemType.SINGLE, ChartLayoutRecipe.ORDINARY, null);
        generator.init(system, StarAge.OLD);

        generator.setPulsarIfNeutron(yellowStar);
        generator.setPulsarIfNeutron(neutronStar);

        verify(system).addCorona(neutronStar, ChartCoronaTerrainPlugin.TERRAIN_ID, 300f, 3f, 0f, 3f);
        ArgumentCaptor<Object> params = ArgumentCaptor.forClass(Object.class);
        verify(system).addTerrain(eq(ChartPulsarBeamTerrainPlugin.TERRAIN_ID), params.capture());
        CoronaParams beam = (CoronaParams) params.getValue();
        assertEquals(200f + 200f * 100f, beam.bandWidthInEngine);
        assertSame(neutronStar, beam.relatedEntity);
        assertEquals(15f, beam.crLossMult);
        verify(beamToken).setCircularOrbit(neutronStar, 0f, 0f, 100f);
        verify(system, never()).addCorona(eq(yellowStar), any(String.class), anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    private static PlanetAPI star(String type, StarSystemAPI system) {
        PlanetAPI star = mock(PlanetAPI.class);
        PlanetSpecAPI spec = mock(PlanetSpecAPI.class);
        when(spec.getPlanetType()).thenReturn(type);
        when(star.getSpec()).thenReturn(spec);
        when(star.getRadius()).thenReturn(200f);
        when(star.getContainingLocation()).thenReturn(system);
        return star;
    }

    private static final float TOP_UP_PLANET_RADIUS = 100f;
    private static final float SMALLEST_NEUTRON_STAR = 150f;
    private static final float SMALLEST_PULSAR_COVER = SMALLEST_NEUTRON_STAR + SMALLEST_NEUTRON_STAR * (100f - 10f * 0.5f) + 1000f;

    private final class TopUpLayout {

        final List<PlanetAPI> planets = new ArrayList<>();
        final List<Float> addedRadii = new ArrayList<>();
        final List<GenContext> contexts = new ArrayList<>();
        final ChartStarSystemGenerator generator;
        private final StarSystemAPI system = mock(StarSystemAPI.class);
        private final SectorEntityToken centreToken = mock(SectorEntityToken.class);

        TopUpLayout(float orbitLimit) {
            this(orbitLimit, List.of(new ChartAnomaly() {
                @Override
                public String id() {
                    return "needs_planets";
                }

                @Override
                public int minPlanets() {
                    return StarAnomalies.NEUTRON_STAR.minPlanets();
                }
            }));
        }

        TopUpLayout(float orbitLimit, List<ChartAnomaly> anomalies) {
            SettingsAPI settings = Global.getSettings();
            StarGenDataSpec centreData = mock(StarGenDataSpec.class);
            when(centreData.getAge()).thenReturn(StarAge.OLD);
            when(settings.getSpec(StarGenDataSpec.class, "nebula_center_old", false)).thenReturn(centreData);
            when(settings.getSpec(eq(PlanetGenDataSpec.class), any(), eq(true))).thenReturn(mock(PlanetGenDataSpec.class));
            when(system.initNonStarCenter()).thenReturn(centreToken);
            when(system.getPlanets()).thenReturn(planets);
            PlanetAPI world = mock(PlanetAPI.class);
            when(world.getSpec()).thenReturn(mock(PlanetSpecAPI.class));
            GenResult added = new GenResult();
            added.entities = new ArrayList<>(List.of(world));
            ChartLayoutRecipe recipe = new ChartLayoutRecipe(new ChartLook(List.of("graphics/backgrounds/background4.jpg"), List.of(), 0f, null, 0f,
                    Map.of(), Map.of(), false, ""), new ChartShape(Map.of(StarSystemType.DEEP_SPACE, 1f), 0, 0, orbitLimit, Map.of(), ""));
            generator = spy(new ChartStarSystemGenerator(StarAge.OLD, StarSystemType.DEEP_SPACE, recipe, anomalies));
            doReturn(added).when(generator).addPlanet(any(), any(), anyBoolean(), anyBoolean());
            doAnswer(call -> {
                float radius = call.getArgument(1);
                contexts.add(call.getArgument(0));
                planets.add(planetAt(radius));
                addedRadii.add(radius);
                GenResult orbit = new GenResult();
                orbit.orbitalWidth = (radius + TOP_UP_PLANET_RADIUS) * 2f;
                return orbit;
            }).when(generator).addTopUpOrbit(any(), anyFloat());
        }

        void generate() {
            generator.generateLayout(system, "chart_star");
        }

        PlanetAPI planetAt(float radius) {
            PlanetAPI planet = mock(PlanetAPI.class);
            when(planet.getRadius()).thenReturn(TOP_UP_PLANET_RADIUS);
            when(planet.getOrbitFocus()).thenReturn(centreToken);
            when(planet.getCircularOrbitRadius()).thenReturn(radius);
            return planet;
        }
    }

    @Test
    void categoriesSkipMagneticFieldsAndFollowTheRecipeWeights() {
        ChartStarSystemGenerator frozenOnly = spy(new ChartStarSystemGenerator(StarAge.OLD, StarSystemType.SINGLE,
                recipe(Map.of(), Map.of("cat_barren", 0f)), null));
        ChartStarSystemGenerator ordinary = spy(new ChartStarSystemGenerator(StarAge.OLD, StarSystemType.SINGLE, ChartLayoutRecipe.ORDINARY, null));
        doReturn(false).when(frozenOnly).isCategoryEmpty(any(), any(), any(), anyBoolean());
        doReturn(false).when(ordinary).isCategoryEmpty(any(), any(), any(), anyBoolean());
        GenContext context = new GenContext(ordinary, null, null, null, null, 0, StarAge.OLD.name(), 0f, 1000f, null, -1);
        Set<String> seen = new HashSet<>();

        for (int pick = 0; pick < 200; pick++) {
            seen.add(ordinary.pickCategory(context, null, false).getCategory());
            assertEquals("cat_frozen", frozenOnly.pickCategory(context, null, false).getCategory());
        }

        assertTrue(seen.containsAll(List.of("cat_barren", "cat_frozen")));
        assertFalse(seen.contains("cat_magfield"));
    }
}
