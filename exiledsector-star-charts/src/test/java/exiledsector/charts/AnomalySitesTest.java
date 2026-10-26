package exiledsector.charts;

import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.AsteroidStorm;
import exiledsector.charts.anomaly.StarAnomalies;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartPois;
import exiledsector.charts.content.ChartSpot;
import exiledsector.charts.content.FakeChartContentEngine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        return new AnomalyContentSite(system, "run1", 4, "pirates", ChartGenContext.none(), List.of(), null, clear, Random::new);
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
}
