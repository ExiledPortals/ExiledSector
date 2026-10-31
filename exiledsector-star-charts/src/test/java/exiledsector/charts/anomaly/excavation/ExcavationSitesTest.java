package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.anomaly.AnomalyRun;
import exiledsector.charts.effect.ChartEffect;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.loot.DropType;
import exiledsector.charts.run.ChartRun;
import exiledsector.charts.run.ChartRuns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExcavationSitesTest {

    private static final String RUN_ID = "run1";

    private MockSector mockSector;
    private StarSystemAPI system;
    private final List<PlanetAPI> planets = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        system = mock(StarSystemAPI.class);
        when(system.getPlanets()).thenReturn(planets);
        when(system.getId()).thenReturn("charted space");
        when(system.getOptionalUniqueId()).thenReturn(ChartIds.systemId(RUN_ID));
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private PlanetAPI planet(String id, boolean star, boolean gasGiant) {
        PlanetAPI planet = mock(PlanetAPI.class);
        when(planet.getId()).thenReturn(id);
        when(planet.getName()).thenReturn("Planet " + id);
        when(planet.isStar()).thenReturn(star);
        when(planet.isGasGiant()).thenReturn(gasGiant);
        when(system.getEntityById(id)).thenReturn(planet);
        planets.add(planet);
        return planet;
    }

    private AnomalyContentSite site(ChartGenContext context, long seed) {
        return new AnomalyContentSite(system, RUN_ID, 4, "pirates", context, List.of(), new Vector2f(), spot -> true,
                salt -> new Random(seed ^ salt));
    }

    private static ChartRun run() {
        return new ChartRun(RUN_ID, ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
    }

    @Test
    void theAnomalyIsBuiltIn() {
        assertSame(ExcavationSites.INSTANCE, ChartAnomalies.byId(ExcavationSites.ANOMALY_ID));
    }

    @Test
    void sitesGoOnSolidPlanetsAndAreKeptInTheRun() {
        planet("sun", true, false);
        planet("giant", false, true);
        planet("rock1", false, false);
        planet("rock2", false, false);
        planet("rock3", false, false);
        planet("rock4", false, false);
        for (long seed = 0; seed < 30; seed++) {
            ChartRun run = run();
            List<ExcavationSite> placed = ExcavationSites.place(site(ChartGenContext.none(), seed), run, mockSector.sector);

            assertTrue(placed.size() >= ExcavationRules.MIN_SITES && placed.size() <= ExcavationRules.MAX_SITES, String.valueOf(placed.size()));
            assertEquals(placed, ExcavationStates.all(run));
            Set<String> planetIds = new HashSet<>();
            for (ExcavationSite site : placed) {
                assertTrue(site.planetId().startsWith("rock"), site.planetId());
                assertTrue(planetIds.add(site.planetId()));
                assertFalse(site.cleared());
                assertTrue(ExcavationRules.LEANS.contains(site.lean()));
            }
        }
    }

    @Test
    void sitesSkipPlanetsAnotherAnomalyHoldsAndClaimTheirOwn() {
        PlanetAPI facility = planet("facility", false, false);
        PlanetAPI rock = planet("rock", false, false);
        for (long seed = 0; seed < 20; seed++) {
            AnomalyContentSite site = site(ChartGenContext.none(), seed);
            site.claim(facility);

            List<ExcavationSite> placed = ExcavationSites.place(site, run(), mockSector.sector);

            assertEquals(List.of("rock"), placed.stream().map(ExcavationSite::planetId).toList());
            assertTrue(site.isClaimed(rock));
        }
    }

    @Test
    void halfTheSitesHaveAnOpenShaft() {
        Random random = new Random(7L);
        int open = 0;
        for (int roll = 0; roll < 2000; roll++) {
            open += ExcavationRules.access(random) == ExcavationSite.Access.OPEN ? 1 : 0;
        }
        assertTrue(open > 900 && open < 1100, String.valueOf(open));
    }

    @Test
    void theExtraSitesLineAddsSitesUpToThePlanetsThereAre() {
        for (int index = 0; index < 8; index++) {
            planet("rock" + index, false, false);
        }
        ChartGenContext context = ChartGenContext.of(List.of(new ChartGenContext.AppliedEffect(ChartEffect.CHART_EXTRA_EXCAVATION_SITES, 2f)));
        ChartRun run = run();

        List<ExcavationSite> placed = ExcavationSites.place(site(context, 3L), run, mockSector.sector);

        assertTrue(placed.size() >= ExcavationRules.MIN_SITES + 2 && placed.size() <= ExcavationRules.MAX_SITES + 2);
        planets.clear();
        planet("only", false, false);
        assertEquals(1, ExcavationSites.place(site(context, 3L), run(), mockSector.sector).size());
    }

    @Test
    void gasGiantsHoldSitesOnlyWhenNoSolidPlanetIsLeft() {
        planet("sun", true, false);
        planet("giant", false, true);

        assertEquals(List.of("giant"), ExcavationSites.candidates(site(ChartGenContext.none(), 1L)).stream().map(PlanetAPI::getId).toList());
        planet("rock", false, false);
        assertEquals(List.of("rock"), ExcavationSites.candidates(site(ChartGenContext.none(), 1L)).stream().map(PlanetAPI::getId).toList());
    }

    @Test
    void generatingWithoutARunToRecordInFails() {
        planet("rock", false, false);

        assertThrows(IllegalStateException.class, () -> ExcavationSites.INSTANCE.generate(site(ChartGenContext.none(), 1L)));
    }

    @Test
    void generatingRecordsTheSitesInTheCurrentRun() {
        planet("rock", false, false);
        ChartRun run = run();
        ChartRuns.store(run);

        ExcavationSites.INSTANCE.generate(site(ChartGenContext.none(), 1L));

        assertEquals("rock", ExcavationStates.all(run).get(0).planetId());
        assertNotNull(ExcavationSites.INSTANCE.intelText(new AnomalyRun(run, system)));
    }

    @Test
    void aClearedSiteStaysRecordedButNoLongerStands() {
        ChartRun run = run();
        ExcavationSite site = new ExcavationSite(0, "rock", ExcavationSite.Access.COLLAPSED, DropType.WEAPON, 42L, false);
        ExcavationStates.record(run, site);
        ExcavationStates.record(run, new ExcavationSite(1, "stone", ExcavationSite.Access.OPEN, DropType.AI_CORE, -9L, false));

        assertTrue(ExcavationStates.clear(run, site));
        assertFalse(ExcavationStates.clear(run, site));

        assertTrue(ExcavationStates.at(run, "rock").cleared());
        assertNull(ExcavationStates.standingAt(run, "rock"));
        assertEquals(List.of("stone"), ExcavationStates.standing(run).stream().map(ExcavationSite::planetId).toList());
        assertEquals(-9L, ExcavationStates.standingAt(run, "stone").seed());
    }

    @Test
    void aSiteSurvivesEncodingAndRejectsBrokenText() {
        ExcavationSite site = new ExcavationSite(2, "planet_7", ExcavationSite.Access.OPEN, DropType.BLUEPRINT, Long.MIN_VALUE, true);

        assertEquals(site, ExcavationSite.decode(2, site.encode()));
        assertNull(ExcavationSite.decode(0, "planet,open,NOT_A_TYPE,1,standing"));
        assertNull(ExcavationSite.decode(0, "planet,sideways,WEAPON,1,standing"));
        assertNull(ExcavationSite.decode(0, "planet,open,WEAPON"));
        assertFalse(ExcavationSite.encodable("a,b"));
        assertFalse(ExcavationSite.encodable("a=b"));
    }

    @Test
    void theIntelNamesThePlanetsWhoseSitesStillStand() {
        planet("rock", false, false);
        planet("stone", false, false);
        ChartRun run = run();
        ExcavationSite rock = new ExcavationSite(0, "rock", ExcavationSite.Access.OPEN, DropType.WEAPON, 1L, false);
        ExcavationStates.record(run, rock);
        ExcavationStates.record(run, new ExcavationSite(1, "stone", ExcavationSite.Access.COLLAPSED, DropType.WEAPON, 2L, false));

        String both = ExcavationSites.INSTANCE.intelText(new AnomalyRun(run, system));
        assertTrue(both.contains("Planet rock") && both.contains("Planet stone"), both);
        ExcavationStates.clear(run, rock);
        String one = ExcavationSites.INSTANCE.intelText(new AnomalyRun(run, system));
        assertFalse(one.contains("Planet rock"), one);
        assertTrue(one.contains("Planet stone"), one);
    }
}
