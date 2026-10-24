package exiledsector.charts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.impl.campaign.procgen.CategoryGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.GenContext;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType;
import exiledlib.MockSector;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
    void aStarPhenomenonForcesOnlyThePrimaryStar() {
        ChartStarSystemGenerator blackHole = new ChartStarSystemGenerator(StarAge.OLD, StarSystemType.SINGLE, ChartLayoutRecipe.ORDINARY,
                exiledsector.charts.phenomenon.StarPhenomena.BLACK_HOLE);

        assertEquals("black_hole", blackHole.pickStar(StarAge.OLD).getPlanetType());
        for (int pick = 0; pick < 50; pick++) {
            assertFalse(ChartLayoutRecipe.PHENOMENON_STARS.contains(blackHole.pickStar(StarAge.OLD).getPlanetType()));
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
