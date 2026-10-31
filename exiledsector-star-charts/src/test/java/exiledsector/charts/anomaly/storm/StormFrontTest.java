package exiledsector.charts.anomaly.storm;

import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import exiledsector.MockSector;
import exiledsector.charts.ChartIds;
import exiledsector.charts.anomaly.AnomalyContentSite;
import exiledsector.charts.anomaly.ChartAnomalies;
import exiledsector.charts.generation.ChartGenContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StormFrontTest {

    private MockSector mockSector;
    private StarSystemAPI system;
    private float elapsedDays;
    private final List<CampaignTerrainAPI> terrain = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        CampaignClockAPI clock = mock(CampaignClockAPI.class);
        when(clock.getTimestamp()).thenReturn(100L);
        when(clock.getElapsedDaysSince(anyLong())).thenAnswer(invocation -> elapsedDays);
        when(mockSector.sector.getClock()).thenReturn(clock);
        system = mock(StarSystemAPI.class);
        when(system.getTerrainCopy()).thenReturn(terrain);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
    }

    private PlanetAPI planet(String id, float x, float y) {
        PlanetAPI planet = mock(PlanetAPI.class);
        when(planet.getId()).thenReturn(id);
        when(planet.getName()).thenReturn(id);
        when(planet.getLocation()).thenReturn(new Vector2f(x, y));
        when(planet.getRadius()).thenReturn(100f);
        return planet;
    }

    private StormFrontTerrainPlugin front(StormPath path) {
        CampaignTerrainAPI entity = mock(CampaignTerrainAPI.class);
        Vector2f location = new Vector2f();
        when(entity.getLocation()).thenReturn(location);
        when(entity.getContainingLocation()).thenReturn(system);
        doAnswer(invocation -> {
            location.set(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(entity).setFixedLocation(anyFloat(), anyFloat());
        StormFrontTerrainPlugin front = new StormFrontTerrainPlugin();
        front.init(StormFront.TERRAIN_ID, entity, new StormFrontTerrainPlugin.Params("run1", path, List.of("inner"), 100L, 3L));
        when(entity.getPlugin()).thenReturn(front);
        terrain.add(entity);
        return front;
    }

    @Test
    void theAnomalyIsBuiltIn() {
        assertSame(StormFront.INSTANCE, ChartAnomalies.byId(StormFront.ANOMALY_ID));
    }

    @Test
    void generationAddsOneTaggedFrontReachingPastTheOutermostWorld() {
        PlanetAPI star = planet("star", 0f, 0f);
        when(star.isStar()).thenReturn(true);
        PlanetAPI inner = planet("inner", 3000f, 0f);
        PlanetAPI outer = planet("outer", 0f, 9000f);
        when(system.getPlanets()).thenReturn(List.of(star, inner, outer));
        CampaignTerrainAPI added = mock(CampaignTerrainAPI.class);
        when(system.addTerrain(anyString(), any())).thenReturn(added);
        AnomalyContentSite site = new AnomalyContentSite(system, "run1", 4, "pirates", ChartGenContext.none(), List.of(), new Vector2f(),
                spot -> true, salt -> new Random(salt));

        StormFront.INSTANCE.generate(site);

        ArgumentCaptor<Object> params = ArgumentCaptor.forClass(Object.class);
        verify(system).addTerrain(eq(StormFront.TERRAIN_ID), params.capture());
        StormFrontTerrainPlugin.Params front = (StormFrontTerrainPlugin.Params) params.getValue();
        assertEquals("run1", front.runId());
        assertEquals(List.of("inner", "outer"), front.markIds());
        assertEquals(9100f + StormRules.EDGE_MARGIN, front.path().edgeRadius(), 0.01f);
        assertEquals(StormRules.BAND_WIDTH, front.path().bandWidth());
        assertEquals(StormRules.CROSSING_DAYS, front.path().crossingDays());
        assertEquals(100L, front.startTimestamp());
        verify(added).addTag(ChartIds.runTag("run1"));
        verify(added).addTag(StormFront.FRONT_TAG);
        assertTrue(site.placed().contains(added));
    }

    @Test
    void theFrontFollowsItsPathThenDissipates() {
        StormFrontTerrainPlugin front = front(new StormPath(0f, 10000f, 3000f, 30f));
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        when(fleet.getLocation()).thenReturn(new Vector2f(0f, 0f));
        when(fleet.getContainingLocation()).thenReturn(system);

        elapsedDays = 15f;
        front.advance(0.1f);
        assertEquals(0f, front.getEntity().getLocation().x, 0.01f);
        assertSame(front, StormFront.frontHolding(fleet));
        assertTrue(StormFront.INSTANCE.isActiveIn(system));

        elapsedDays = 30f;
        assertTrue(front.isDone());
        assertNull(StormFront.frontHolding(fleet));
        assertFalse(StormFront.INSTANCE.isActiveIn(system));
        assertNull(StormFront.INSTANCE.intelText(null, system));
    }

    @Test
    void fleetsInsideLoseSensorRange() {
        StormFrontTerrainPlugin front = front(new StormPath(0f, 10000f, 3000f, 30f));
        CampaignFleetAPI fleet = mock(CampaignFleetAPI.class);
        MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class);
        StatBonus sensorRange = new StatBonus();
        when(stats.getSensorRangeMod()).thenReturn(sensorRange);
        when(fleet.getStats()).thenReturn(stats);

        front.applyEffect(fleet, 0.1f);

        verify(stats).addTemporaryModMult(eq(StormFrontTerrainPlugin.SENSOR_MOD_DAYS), eq(front.getModId()), any(),
                eq(StormRules.SENSOR_RANGE_MULT), eq(sensorRange));
    }

    @Test
    void puffsFillTheBandWithinTheirCaps() {
        StormPath path = new StormPath(0f, 10000f, 3000f, 30f);
        List<StormFrontTerrainPlugin.Puff> puffs = StormFrontTerrainPlugin.puffs(path, 1L);

        assertTrue(puffs.size() >= StormFrontTerrainPlugin.MIN_PUFFS && puffs.size() <= StormFrontTerrainPlugin.MAX_PUFFS);
        for (StormFrontTerrainPlugin.Puff puff : puffs) {
            assertTrue(Math.abs(puff.along()) <= path.bandWidth() / 2f);
            assertTrue(Math.abs(puff.across()) <= path.bandLength() / 2f);
        }
        assertEquals(1f, StormFrontTerrainPlugin.endFade(0f, path.bandLength()));
        assertEquals(0f, StormFrontTerrainPlugin.endFade(path.bandLength() / 2f, path.bandLength()));
    }
}
