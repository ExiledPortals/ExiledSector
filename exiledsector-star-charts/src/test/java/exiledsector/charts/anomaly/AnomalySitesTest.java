package exiledsector.charts.anomaly;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.content.ChartSites;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.content.FakeChartContentEngine;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.run.ChartRun;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnomalySitesTest {

    private FakeChartContentEngine engine;
    private StarSystemAPI system;
    private PlanetAPI star;

    @BeforeEach
    void setUp() throws Exception {
        engine = FakeChartContentEngine.install(FakeChartContentEngine.offeringNothing());
        ChartPois.register(new JSONArray()
                .put(new JSONObject().put("id", "supply").put("kind", "cache").put("entity", "supply_cache").put("cost", "1").put("weight", "1"))
                .put(new JSONObject().put("id", "derelict").put("kind", "derelict").put("entity", "wreck").put("derelict_type", "SMALL")
                        .put("cost", "1").put("weight", "1")));
        system = mock(StarSystemAPI.class);
        star = mock(PlanetAPI.class);
        when(star.getRadius()).thenReturn(200f);
        when(star.getLocation()).thenReturn(new Vector2f());
        when(system.getStar()).thenReturn(star);
        when(system.getCenter()).thenReturn(star);
    }

    @AfterEach
    void tearDown() {
        ChartContentGenerator.resetForTests();
        ChartPois.resetForTests();
    }

    private AnomalyContentSite site(Predicate<ChartSpot> clear) {
        return site(clear, null);
    }

    private AnomalyContentSite site(Predicate<ChartSpot> clear, Vector2f entry) {
        return new AnomalyContentSite(system, "run1", 4, "pirates", ChartGenContext.none(), List.of(), entry, clear, Random::new);
    }

    private PlanetAPI planet(String id, boolean gasGiant) {
        PlanetAPI planet = mock(PlanetAPI.class);
        when(planet.getId()).thenReturn(id);
        when(planet.isGasGiant()).thenReturn(gasGiant);
        return planet;
    }

    private List<Float> orbitRadii(SectorEntityToken focus) {
        ArgumentCaptor<Float> radius = ArgumentCaptor.forClass(Float.class);
        for (SectorEntityToken site : engine.salvageEntities) {
            verify(site).setCircularOrbit(eq(focus), anyFloat(), radius.capture(), anyFloat());
        }
        return radius.getAllValues();
    }

    @Test
    void aBlackHoleIsRingedByDerelictsOnClearOrbits() {
        AnomalyContentSite site = site(spot -> spot.position().length() > 2000f);

        StarAnomalies.BLACK_HOLE.generate(site);

        int derelicts = engine.salvageEntities.size();
        assertTrue(derelicts >= 6 && derelicts <= 9, "got " + derelicts);
        assertTrue(engine.salvageTypes.stream().allMatch("wreck"::equals));
        assertTrue(orbitRadii(star).stream().allMatch(radius -> radius > 2000f && radius <= 7000f));
        assertEquals(engine.salvageEntities, site.placed());
        assertTrue(engine.salvageEntities.stream().allMatch(wreck -> wreck.hasTag(ChartIds.runTag("run1"))));
    }

    @Test
    void withNowhereClearAStarAnomalyPlacesNothing() {
        StarAnomalies.BLACK_HOLE.generate(site(spot -> false));
        StarAnomalies.NEUTRON_STAR.generate(site(spot -> false));

        assertEquals(List.of(), engine.salvageEntities);
    }

    @Test
    void anAsteroidStormHidesCachesInsideItsOwnBelts() {
        CampaignTerrainAPI stormBelt = belt(3000f, true);
        CampaignTerrainAPI otherBelt = belt(6000f, false);
        when(system.getTerrainCopy()).thenReturn(List.of(stormBelt, otherBelt));

        AsteroidStorm.INSTANCE.generate(site(spot -> true));

        int caches = engine.salvageEntities.size();
        assertTrue(caches >= 2 && caches <= 3, "got " + caches);
        assertTrue(engine.salvageTypes.stream().allMatch("supply_cache"::equals));
        assertTrue(orbitRadii(star).stream().allMatch(radius -> Math.abs(radius - 3000f) <= 125f));
    }

    private CampaignTerrainAPI belt(float middleRadius, boolean storm) {
        BaseRingTerrain ring = mock(BaseRingTerrain.class);
        ring.params = new BaseRingTerrain.RingParams(500f, middleRadius, star);
        CampaignTerrainAPI belt = mock(CampaignTerrainAPI.class);
        when(belt.getPlugin()).thenReturn(ring);
        when(belt.hasTag(AsteroidStorm.BELT_TAG)).thenReturn(storm);
        when(belt.getOrbitFocus()).thenReturn(star);
        return belt;
    }

    @Test
    void aBandedSearchPrefersAnOrbitFarFromTheEntryAndOtherwiseKeepsTheFarthest() {
        Vector2f entry = new Vector2f(3000f, 0f);
        for (long seed = 0; seed < 20; seed++) {
            AnomalyContentSite site = site(spot -> true, entry);

            ChartSites.Orbit orbit = site.clearOrbit(OrbitSearch.bands(star, 2000f, 2000f, 4).awayFromEntry(1000f), new Random(seed));

            assertTrue(site.distanceToEntry(orbit.position()) >= 1000f);
        }
        AnomalyContentSite unreachable = site(spot -> true, entry);
        ChartSites.Orbit farthest = unreachable.clearOrbit(OrbitSearch.bands(star, 2000f, 2000f, 4).awayFromEntry(1_000_000f), new Random(3L));
        assertTrue(farthest.radius() >= 2000f && farthest.radius() <= 10000f);
        assertNull(unreachable.clearOrbit(OrbitSearch.bands(null, 2000f, 2000f, 4), new Random(3L)));
    }

    @Test
    void aSteppingSearchTakesTheFirstClearRadius() {
        AnomalyContentSite site = site(spot -> spot.position().length() > 999f);

        ChartSites.Orbit orbit = site.clearOrbit(OrbitSearch.stepping(star, 500f, 3000f, 250f), new Random(1L));

        assertEquals(1000f, orbit.radius(), 0.001f);
        assertEquals(1000f / AnomalyContentSite.ORBIT_SPEED, orbit.days(), 0.001f);
    }

    @Test
    void freePlanetsSkipClaimedOnesAndPreferSolidGround() {
        PlanetAPI giant = planet("giant", true);
        PlanetAPI rock = planet("rock", false);
        when(system.getPlanets()).thenReturn(List.of(star, giant, rock));
        when(star.isStar()).thenReturn(true);
        AnomalyContentSite site = site(spot -> true);

        assertEquals(List.of(rock), site.freePlanets(true));
        assertEquals(List.of(giant, rock), site.freePlanets(false));
        site.claim(rock);
        assertEquals(List.of(giant), site.freePlanets(true));
        assertEquals(List.of(giant, rock), site.worlds());
    }

    @Test
    void rollingBackRemovesWhatTheAnomalyAddedAndReleasesItsClaims() {
        Set<String> claimed = new HashSet<>();
        AnomalyContentSite site = new AnomalyContentSite(system, "run1", 4, "pirates", ChartGenContext.none(), List.of(), List.of(), null,
                spot -> true, Random::new, claimed);
        SectorEntityToken entity = site.track(mock(SectorEntityToken.class));
        EveryFrameScript script = site.addScript(mock(EveryFrameScript.class));
        site.claim(planet("rock", false));
        site.state("test").put("field", "value");

        site.rollBack();

        verify(system).removeEntity(entity);
        verify(system).removeScript(script);
        assertEquals(Set.of(), claimed);
        assertEquals(List.of(), site.placed());
        ChartRun run = new ChartRun("run1", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        site.commitStates(run);
        assertNull(run.anomalyState("test.field"));
    }

    @Test
    void stagedStateReachesOnlyItsOwnRun() {
        AnomalyContentSite site = site(spot -> true);
        AnomalyState state = site.state("test");
        state.put("count", 3);

        assertEquals(3, state.integer("count"));
        assertThrows(IllegalStateException.class, () -> site.commitStates(new ChartRun("other", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1)));
        ChartRun run = new ChartRun("run1", ChartIds.HYPERSPACE_LOCATION_ID, 0f, 0f, 1);
        site.commitStates(run);
        assertEquals(3, AnomalyState.of(run, "test").integer("count"));
    }
}
